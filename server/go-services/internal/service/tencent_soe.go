package service

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	mrand "math/rand"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/gorilla/websocket"
	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// TencentSOEClient 腾讯云智聆口语评测（新版）客户端
//
// 新版 SOE 使用 WebSocket 协议（wss://soe.cloud.tencent.com/soe/api/{appid}）
// 替换了旧版 REST TransmitOralProcessWithInit —— 老版账号从 2024 年起已陆续停用，
// "智聆口语评测（新版本）" 预付费包只对新版 WS 接口计费。
//
// 鉴权：HMAC-SHA1（secretKey 为密钥）对字典序拼接后的 URL 签名，Base64 + URLEncode。
// 流式：客户端按 1:1 实时率发送 PCM（每 40ms 发送 1280 字节 @16kHz），
//       结束时发送 {"type":"end"}；服务端在中间消息里逐步给出评分。
type TencentSOEClient struct {
	secretID  string
	secretKey string
	appID     string
}

func NewTencentSOEClient(cfg *config.Config) *TencentSOEClient {
	return &TencentSOEClient{
		secretID:  cfg.TencentSecretID,
		secretKey: cfg.TencentSecretKey,
		// 同账号下 SOE 和 ASR 共用同一个 AppID
		appID: cfg.TencentASRAppID,
	}
}

// IsConfigured 检查是否已配置
func (c *TencentSOEClient) IsConfigured() bool {
	return c.secretID != "" && c.secretKey != "" && c.appID != ""
}

// Assess 发音评测 — 评估学生发音的准确度、流利度、完整度
// 输入: PCM/WAV 音频数据 + 参考文本
// 输出: 发音评分（0-100 各维度）
func (c *TencentSOEClient) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	if !c.IsConfigured() {
		return nil, fmt.Errorf("腾讯云 SOE 未配置（需 SECRET_ID/KEY + ASR_APP_ID）")
	}

	// 去掉 WAV 头，拿到原始 PCM
	pcm := stripWAVHeader(audioData)

	voiceID := generateUUID()
	timestamp := time.Now().Unix()
	params := []kvPair{
		{"secretid", c.secretID},
		{"timestamp", strconv.FormatInt(timestamp, 10)},
		{"expired", strconv.FormatInt(timestamp+3600, 10)},
		{"nonce", strconv.Itoa(mrand.Intn(1_000_000_000) + 1)},
		{"voice_id", voiceID},
		{"voice_format", "0"},          // PCM
		{"server_engine_type", "16k_en"}, // 英文 16kHz
		{"eval_mode", "1"},             // 句子模式
		{"score_coeff", "2.0"},         // 苛刻系数（中等）
		{"text_mode", "0"},             // 普通文本
		{"sentence_info_enabled", "1"}, // 中间结果带评分
		{"ref_text", referenceText},
	}
	sort.Slice(params, func(i, j int) bool { return params[i].k < params[j].k })

	// 签名原文：host + path + ? + k=v&k=v...（value 不编码，符合腾讯 WS 签名约定）
	var signBuf strings.Builder
	signBuf.WriteString("soe.cloud.tencent.com/soe/api/")
	signBuf.WriteString(c.appID)
	signBuf.WriteString("?")
	for i, p := range params {
		if i > 0 {
			signBuf.WriteString("&")
		}
		signBuf.WriteString(p.k)
		signBuf.WriteString("=")
		signBuf.WriteString(p.v)
	}

	h := hmac.New(sha1.New, []byte(c.secretKey))
	h.Write([]byte(signBuf.String()))
	sigEncoded := url.QueryEscape(base64.StdEncoding.EncodeToString(h.Sum(nil)))

	// 构造 WS URL（value 需 urlencode 以处理 ref_text 里的空格/标点）
	var urlBuf strings.Builder
	urlBuf.WriteString("wss://soe.cloud.tencent.com/soe/api/")
	urlBuf.WriteString(c.appID)
	urlBuf.WriteString("?")
	for i, p := range params {
		if i > 0 {
			urlBuf.WriteString("&")
		}
		urlBuf.WriteString(p.k)
		urlBuf.WriteString("=")
		urlBuf.WriteString(url.QueryEscape(p.v))
	}
	urlBuf.WriteString("&signature=")
	urlBuf.WriteString(sigEncoded)

	wsURL := urlBuf.String()
	log.Printf("[TencentSOE] 评测请求: audio=%d bytes, ref=%q, voice=%s",
		len(pcm), referenceText, voiceID)

	// 建立 WebSocket 连接
	dialer := *websocket.DefaultDialer
	dialer.HandshakeTimeout = 10 * time.Second
	conn, _, err := dialer.Dial(wsURL, nil)
	if err != nil {
		return nil, fmt.Errorf("SOE WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 读取握手响应（第一条消息）
	_, handshakeMsg, err := conn.ReadMessage()
	if err != nil {
		return nil, fmt.Errorf("SOE 握手读取失败: %w", err)
	}
	var handshake struct {
		Code    int    `json:"code"`
		Message string `json:"message"`
		VoiceID string `json:"voice_id"`
	}
	if err := json.Unmarshal(handshakeMsg, &handshake); err != nil {
		return nil, fmt.Errorf("SOE 握手响应解析失败: %w", err)
	}
	if handshake.Code != 0 {
		return nil, fmt.Errorf("SOE 握手失败 [%d]: %s", handshake.Code, handshake.Message)
	}
	log.Printf("[TencentSOE] 握手成功: voice_id=%s", handshake.VoiceID)

	// 独立 goroutine 按 1:1 实时率发送音频（超过 1:1 或间隔 >6s 腾讯会断开）
	go func() {
		const chunkSize = 1280 // 40ms @ 16kHz / 16bit / mono
		for i := 0; i < len(pcm); i += chunkSize {
			end := i + chunkSize
			if end > len(pcm) {
				end = len(pcm)
			}
			if err := conn.WriteMessage(websocket.BinaryMessage, pcm[i:end]); err != nil {
				log.Printf("[TencentSOE] 写入音频失败: %v", err)
				return
			}
			time.Sleep(40 * time.Millisecond)
		}
		// 结束标志
		if err := conn.WriteMessage(websocket.TextMessage, []byte(`{"type":"end"}`)); err != nil {
			log.Printf("[TencentSOE] 发送结束标志失败: %v", err)
		}
	}()

	// 主循环读响应，累积最后一条带分数的 result
	var lastScore soeResult
	haveScore := false
	for {
		_, msg, err := conn.ReadMessage()
		if err != nil {
			return nil, fmt.Errorf("SOE 读取响应失败: %w", err)
		}

		var resp struct {
			Code    int             `json:"code"`
			Message string          `json:"message"`
			Final   int             `json:"final"`
			Result  json.RawMessage `json:"result"`
		}
		if err := json.Unmarshal(msg, &resp); err != nil {
			log.Printf("[TencentSOE] 跳过解析失败的消息: %v", err)
			continue
		}
		if resp.Code != 0 {
			return nil, fmt.Errorf("SOE 评测错误 [%d]: %s", resp.Code, resp.Message)
		}

		if len(resp.Result) > 0 {
			if r, ok := decodeSoeResult(resp.Result); ok {
				// 只保留稳定值（-1 表示尚未评出）
				if r.PronAccuracy >= 0 {
					lastScore = r
					haveScore = true
				}
			}
		}

		if resp.Final == 1 {
			break
		}
	}

	if !haveScore {
		return nil, fmt.Errorf("SOE 未返回有效评分")
	}

	// 映射归一化到 0-100
	//   PronAccuracy / SuggestedScore : 0-100
	//   PronFluency  / PronCompletion : 0-1   ← ×100
	score := &model.PronunciationScore{
		Overall:    lastScore.PronAccuracy,
		Fluency:    lastScore.PronFluency * 100,
		Integrity:  lastScore.PronCompletion * 100,
		Stress:     lastScore.PronAccuracy,
		Intonation: lastScore.PronAccuracy * 0.95, // 估算
		Phonemes:   []model.PhonemeScore{},
	}
	if lastScore.SuggestedScore > 0 {
		score.Overall = lastScore.SuggestedScore
	}

	log.Printf("[TencentSOE] 评测成功: accuracy=%.1f, fluency=%.2f, completion=%.2f, suggested=%.2f",
		lastScore.PronAccuracy, lastScore.PronFluency,
		lastScore.PronCompletion, lastScore.SuggestedScore)

	return score, nil
}

// ==================== 内部 ====================

type kvPair struct {
	k, v string
}

type soeResult struct {
	PronAccuracy   float64 `json:"PronAccuracy"`
	PronFluency    float64 `json:"PronFluency"`
	PronCompletion float64 `json:"PronCompletion"`
	SuggestedScore float64 `json:"SuggestedScore"`
}

// decodeSoeResult —— result 字段在不同情形下有两种形态：
// 1) JSON 对象：直接按 soeResult 解码
// 2) JSON 字符串内嵌对象：先解码为 string，再解码为 soeResult
func decodeSoeResult(raw json.RawMessage) (soeResult, bool) {
	var obj soeResult
	if err := json.Unmarshal(raw, &obj); err == nil && obj != (soeResult{}) {
		return obj, true
	}
	var s string
	if err := json.Unmarshal(raw, &s); err == nil && s != "" {
		var inner soeResult
		if err := json.Unmarshal([]byte(s), &inner); err == nil {
			return inner, true
		}
	}
	return soeResult{}, false
}

// generateUUID 生成 UUID v4
func generateUUID() string {
	b := make([]byte, 16)
	_, _ = rand.Read(b)
	b[6] = (b[6] & 0x0f) | 0x40 // Version 4
	b[8] = (b[8] & 0x3f) | 0x80 // Variant 10
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}
