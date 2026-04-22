package service

import (
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha1"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"net/url"
	"sort"
	"strings"
	"time"

	"github.com/gorilla/websocket"
	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// TencentSOEClient 腾讯云智聆口语评测（新版）WebSocket 客户端
// 文档: https://cloud.tencent.cn/document/product/1774/107497
type TencentSOEClient struct {
	secretID  string
	secretKey string
	appID     string
	region    string
}

func NewTencentSOEClient(cfg *config.Config) *TencentSOEClient {
	return &TencentSOEClient{
		secretID:  cfg.TencentSecretID,
		secretKey: cfg.TencentSecretKey,
		appID:     cfg.TencentASRAppID,
		region:    cfg.TencentRegion,
	}
}

func (c *TencentSOEClient) IsConfigured() bool {
	return c.secretID != "" && c.secretKey != "" && c.appID != ""
}

// Assess 发音评测
func (c *TencentSOEClient) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	if !c.IsConfigured() {
		return nil, fmt.Errorf("腾讯云 SOE 未配置")
	}

	audioData = stripWAVHeader(audioData)
	voiceID := soeGenUUID()

	log.Printf("[TencentSOE] 评测: audio=%d bytes, ref=%q, voice=%s",
		len(audioData), referenceText[:soeMin(50, len(referenceText))], voiceID)

	// 1. 构建签名 URL 并连接 WebSocket
	wsURL := c.buildWSURL(voiceID, referenceText)

	conn, _, err := websocket.DefaultDialer.Dial(wsURL, nil)
	if err != nil {
		return nil, fmt.Errorf("SOE WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 读取握手响应
	_, handshakeMsg, err := conn.ReadMessage()
	if err != nil {
		return nil, fmt.Errorf("SOE 握手读取失败: %w", err)
	}

	var handshake struct {
		Code    int    `json:"code"`
		Message string `json:"message"`
	}
	json.Unmarshal(handshakeMsg, &handshake)
	if handshake.Code != 0 {
		return nil, fmt.Errorf("SOE 握手失败 %d: %s", handshake.Code, handshake.Message)
	}

	log.Printf("[TencentSOE] 握手成功")

	// 2. 发送音频（二进制帧，每 40ms = 1280 bytes @ 16kHz 16bit mono）
	chunkSize := 1280
	for i := 0; i < len(audioData); i += chunkSize {
		end := i + chunkSize
		if end > len(audioData) {
			end = len(audioData)
		}
		if err := conn.WriteMessage(websocket.BinaryMessage, audioData[i:end]); err != nil {
			return nil, fmt.Errorf("发送音频帧失败: %w", err)
		}
		// 模拟实时发送（40ms 间隔），避免发送过快被拒
		time.Sleep(10 * time.Millisecond)
	}

	// 3. 发送结束帧
	endFrame := []byte(`{"type":"end"}`)
	if err := conn.WriteMessage(websocket.TextMessage, endFrame); err != nil {
		return nil, fmt.Errorf("发送结束帧失败: %w", err)
	}

	// 4. 接收评测结果（等待 final=1）
	var finalScore *model.PronunciationScore
	for {
		_, msg, err := conn.ReadMessage()
		if err != nil {
			if finalScore != nil {
				break
			}
			return nil, fmt.Errorf("读取 SOE 结果失败: %w", err)
		}

		var resp struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Final   int    `json:"final"`
			Result  json.RawMessage `json:"result"`
		}

		if err := json.Unmarshal(msg, &resp); err != nil {
			continue
		}

		if resp.Code != 0 {
			return nil, fmt.Errorf("SOE 错误 %d: %s", resp.Code, resp.Message)
		}

		if resp.Final == 1 && resp.Result != nil {
			// 解析评分结果
			var result struct {
				PronAccuracy   float64 `json:"PronAccuracy"`
				PronFluency    float64 `json:"PronFluency"`
				PronCompletion float64 `json:"PronCompletion"`
				SuggestedScore float64 `json:"SuggestedScore"`
			}
			json.Unmarshal(resp.Result, &result)

			overall := result.PronAccuracy
			if result.SuggestedScore > 0 {
				overall = result.SuggestedScore
			}

			finalScore = &model.PronunciationScore{
				Overall:    overall,
				Fluency:    result.PronFluency,
				Integrity:  result.PronCompletion,
				Stress:     result.PronAccuracy,
				Intonation: result.PronAccuracy * 0.95,
				Phonemes:   []model.PhonemeScore{},
			}

			log.Printf("[TencentSOE] 评测成功: overall=%.1f, fluency=%.1f, completion=%.1f",
				overall, result.PronFluency, result.PronCompletion)
			break
		}
	}

	if finalScore == nil {
		return nil, fmt.Errorf("SOE 未返回评测结果")
	}

	return finalScore, nil
}

// buildWSURL 构建新版 SOE WebSocket 签名 URL
func (c *TencentSOEClient) buildWSURL(voiceID, refText string) string {
	timestamp := time.Now().Unix()
	expired := timestamp + 600

	nonceBuf := make([]byte, 4)
	rand.Read(nonceBuf)
	nonce := int(nonceBuf[0])<<24 | int(nonceBuf[1])<<16 | int(nonceBuf[2])<<8 | int(nonceBuf[3])
	if nonce < 0 {
		nonce = -nonce
	}

	params := map[string]string{
		"secretid":           c.secretID,
		"timestamp":          fmt.Sprintf("%d", timestamp),
		"expired":            fmt.Sprintf("%d", expired),
		"nonce":              fmt.Sprintf("%d", nonce),
		"server_engine_type": "16k_en",
		"voice_id":           voiceID,
		"voice_format":       "1",
		"ref_text":           refText,
		"eval_mode":          "1",
		"score_coeff":        "2",
	}

	// 按字典序排序
	keys := make([]string, 0, len(params))
	for k := range params {
		keys = append(keys, k)
	}
	sort.Strings(keys)

	var pairs []string
	for _, k := range keys {
		pairs = append(pairs, fmt.Sprintf("%s=%s", k, params[k]))
	}
	queryStr := strings.Join(pairs, "&")

	// 签名原文 = host/path?params（不含协议前缀）
	signStr := fmt.Sprintf("soe.cloud.tencent.com/soe/api/%s?%s", c.appID, queryStr)

	// HMAC-SHA1 签名
	h := hmac.New(sha1.New, []byte(c.secretKey))
	h.Write([]byte(signStr))
	signature := base64.StdEncoding.EncodeToString(h.Sum(nil))

	// 拼接完整 URL（参数值需要 URL 编码）
	var encodedPairs []string
	for _, k := range keys {
		encodedPairs = append(encodedPairs, fmt.Sprintf("%s=%s", k, url.QueryEscape(params[k])))
	}
	encodedQuery := strings.Join(encodedPairs, "&")

	wsURL := fmt.Sprintf("wss://soe.cloud.tencent.com/soe/api/%s?%s&signature=%s",
		c.appID, encodedQuery, url.QueryEscape(signature))

	return wsURL
}

func soeGenUUID() string {
	b := make([]byte, 16)
	rand.Read(b)
	b[6] = (b[6] & 0x0f) | 0x40
	b[8] = (b[8] & 0x3f) | 0x80
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}

func soeMin(a, b int) int {
	if a < b {
		return a
	}
	return b
}
