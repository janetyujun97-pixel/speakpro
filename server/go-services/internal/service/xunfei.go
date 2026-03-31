package service

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/gorilla/websocket"
	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// XunfeiClient 讯飞开放平台 API 客户端
// 文档: https://www.xfyun.cn/doc/
type XunfeiClient struct {
	appID     string
	apiKey    string
	apiSecret string
}

func NewXunfeiClient(cfg *config.Config) *XunfeiClient {
	return &XunfeiClient{
		appID:     cfg.XunfeiAppID,
		apiKey:    cfg.XunfeiAPIKey,
		apiSecret: cfg.XunfeiAPISecret,
	}
}

// buildAuthURL 构建讯飞 WebSocket 鉴权 URL
// 签名算法: HMAC-SHA256，参考: https://www.xfyun.cn/doc/asr/voicedictation/API.html
func (c *XunfeiClient) buildAuthURL(rawURL string) (string, error) {
	u, err := url.Parse(rawURL)
	if err != nil {
		return "", fmt.Errorf("解析 URL 失败: %w", err)
	}

	// RFC1123 时间格式
	date := time.Now().UTC().Format(http.TimeFormat)

	// 构建签名原串: host + date + request-line
	host := u.Host
	path := u.RequestURI()
	signStr := fmt.Sprintf("host: %s\ndate: %s\nGET %s HTTP/1.1", host, date, path)

	// HMAC-SHA256 签名
	mac := hmac.New(sha256.New, []byte(c.apiSecret))
	mac.Write([]byte(signStr))
	sig := base64.StdEncoding.EncodeToString(mac.Sum(nil))

	// 构建 Authorization 字段
	auth := fmt.Sprintf(
		`api_key="%s", algorithm="hmac-sha256", headers="host date request-line", signature="%s"`,
		c.apiKey, sig,
	)
	authEncoded := base64.StdEncoding.EncodeToString([]byte(auth))

	// 拼接最终 URL 参数
	params := url.Values{}
	params.Set("authorization", authEncoded)
	params.Set("date", date)
	params.Set("host", host)

	return fmt.Sprintf("%s?%s", rawURL, params.Encode()), nil
}

// Recognize 实时语音识别 (ASR) — 讯飞 IAT WebSocket API
// 将 PCM/RAW 格式音频转写为英文文本
func (c *XunfeiClient) Recognize(audioData []byte) (string, error) {
	if c.appID == "" {
		return "", errors.New("讯飞 AppID 未配置")
	}

	authURL, err := c.buildAuthURL("wss://iat-api.xfyun.cn/v2/iat")
	if err != nil {
		return "", err
	}

	conn, _, err := websocket.DefaultDialer.Dial(authURL, nil)
	if err != nil {
		return "", fmt.Errorf("讯飞 ASR WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 首帧：携带业务配置 + 首段音频
	chunkSize := 1280
	firstChunk := audioData
	if len(firstChunk) > chunkSize {
		firstChunk = audioData[:chunkSize]
	}

	firstFrame := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": c.appID,
		},
		"business": map[string]interface{}{
			"language": "en_us",
			"domain":   "iat",
			"accent":   "mandarin",
			"vad_eos":  10000, // 端点检测超时 ms
			"dwa":      "wpgs", // 动态修正
		},
		"data": map[string]interface{}{
			"status":   0, // 0=首帧
			"format":   "audio/L16;rate=16000",
			"encoding": "raw",
			"audio":    base64.StdEncoding.EncodeToString(firstChunk),
		},
	}
	if err := conn.WriteJSON(firstFrame); err != nil {
		return "", fmt.Errorf("发送 ASR 首帧失败: %w", err)
	}

	// 发送剩余音频帧（每帧 1280 字节 = 40ms @ 16kHz）
	for i := chunkSize; i < len(audioData); i += chunkSize {
		end := min(i+chunkSize, len(audioData))
		status := 1 // 中间帧
		if end >= len(audioData) {
			status = 2 // 末帧
		}
		frame := map[string]interface{}{
			"data": map[string]interface{}{
				"status":   status,
				"format":   "audio/L16;rate=16000",
				"encoding": "raw",
				"audio":    base64.StdEncoding.EncodeToString(audioData[i:end]),
			},
		}
		if err := conn.WriteJSON(frame); err != nil {
			return "", fmt.Errorf("发送 ASR 音频帧失败: %w", err)
		}
	}

	// 接收识别结果并拼接
	var fullTranscript strings.Builder
	for {
		var result struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Data    struct {
				Status int `json:"status"`
				Result struct {
					Ws []struct {
						Cw []struct {
							W string `json:"w"`
						} `json:"cw"`
					} `json:"ws"`
				} `json:"result"`
			} `json:"data"`
		}
		if err := conn.ReadJSON(&result); err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				break
			}
			return "", fmt.Errorf("读取 ASR 结果失败: %w", err)
		}
		if result.Code != 0 {
			return "", fmt.Errorf("讯飞 ASR 错误 %d: %s", result.Code, result.Message)
		}
		for _, ws := range result.Data.Result.Ws {
			for _, cw := range ws.Cw {
				fullTranscript.WriteString(cw.W)
			}
		}
		if result.Data.Status == 2 {
			break
		}
	}

	return fullTranscript.String(), nil
}

// Assess 发音评测 — 讯飞 ISE (Intelligent Speech Evaluation) WebSocket API
// 返回发音、流利度、完整度等多维度评分
func (c *XunfeiClient) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	if c.appID == "" {
		return nil, errors.New("讯飞 AppID 未配置")
	}

	authURL, err := c.buildAuthURL("wss://ise-api.xfyun.cn/v2/ise")
	if err != nil {
		return nil, err
	}

	conn, _, err := websocket.DefaultDialer.Dial(authURL, nil)
	if err != nil {
		return nil, fmt.Errorf("讯飞 ISE WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 首帧：评测配置 + 参考文本 + 首段音频
	chunkSize := 1280
	firstChunk := audioData
	if len(firstChunk) > chunkSize {
		firstChunk = audioData[:chunkSize]
	}

	// 参考文本需要 UTF-8 BOM
	refTextWithBOM := "\uFEFF" + referenceText

	firstFrame := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": c.appID,
		},
		"business": map[string]interface{}{
			"category": "read_sentence",   // 读句子模式
			"sub":      "ise",
			"ent":      "en_us-isesub01",  // 英文评测引擎
			"text":     refTextWithBOM,
			"tte":      "utf-8",
			"is_end":   0,
			"cmd":      "ssb",
		},
		"data": map[string]interface{}{
			"status":   0, // 首帧
			"encoding": "raw",
			"audio":    base64.StdEncoding.EncodeToString(firstChunk),
		},
	}
	if err := conn.WriteJSON(firstFrame); err != nil {
		return nil, fmt.Errorf("发送 ISE 首帧失败: %w", err)
	}

	// 发送剩余音频帧
	for i := chunkSize; i < len(audioData); i += chunkSize {
		end := min(i+chunkSize, len(audioData))
		status := 1
		if end >= len(audioData) {
			status = 2
		}
		frame := map[string]interface{}{
			"data": map[string]interface{}{
				"status":   status,
				"encoding": "raw",
				"audio":    base64.StdEncoding.EncodeToString(audioData[i:end]),
			},
		}
		if err := conn.WriteJSON(frame); err != nil {
			return nil, fmt.Errorf("发送 ISE 音频帧失败: %w", err)
		}
	}

	// 接收评测结果（ISE 返回 base64 编码的 XML）
	var lastData string
	for {
		var frame struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Data    struct {
				Status int    `json:"status"`
				Data   string `json:"data"` // base64 XML
			} `json:"data"`
		}
		if err := conn.ReadJSON(&frame); err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				break
			}
			return nil, fmt.Errorf("读取 ISE 结果失败: %w", err)
		}
		if frame.Code != 0 {
			return nil, fmt.Errorf("讯飞 ISE 错误 %d: %s", frame.Code, frame.Message)
		}
		if frame.Data.Data != "" {
			lastData = frame.Data.Data
		}
		if frame.Data.Status == 2 {
			break
		}
	}

	return parseISEScore(lastData), nil
}

// parseISEScore 解析讯飞 ISE 返回的 base64 XML 评分结果
// ISE 返回的 XML 包含 totalscore、fluency_score、integrity_score 等属性
func parseISEScore(encoded string) *model.PronunciationScore {
	if encoded == "" {
		return &model.PronunciationScore{Overall: 0}
	}

	xmlData, err := base64.StdEncoding.DecodeString(encoded)
	if err != nil {
		return &model.PronunciationScore{Overall: 0}
	}

	xmlStr := string(xmlData)
	overall := extractXMLAttr(xmlStr, "totalscore")
	fluency := extractXMLAttr(xmlStr, "fluency_score")
	integrity := extractXMLAttr(xmlStr, "integrity_score")

	// 各维度推算（ISE XML 中还有更细粒度的 phone 评分）
	return &model.PronunciationScore{
		Overall:    overall,
		Fluency:    fluency,
		Integrity:  integrity,
		Stress:     overall * 0.9,     // 重音评分估算
		Intonation: overall * 0.95,    // 语调评分估算
		Phonemes:   []model.PhonemeScore{}, // TODO: 解析音素级评分
	}
}

// extractXMLAttr 从 XML 字符串中提取指定属性的浮点值
// 例: totalscore="85.5" → 85.5
func extractXMLAttr(xmlStr, attr string) float64 {
	key := attr + `="`
	idx := strings.Index(xmlStr, key)
	if idx < 0 {
		return 0
	}
	rest := xmlStr[idx+len(key):]
	end := strings.Index(rest, `"`)
	if end < 0 {
		return 0
	}
	var val float64
	fmt.Sscanf(rest[:end], "%f", &val)
	return val
}

// Synthesize TTS 语音合成 — 讯飞 TTS WebSocket API
// text: 待合成文本（英文）; voice: 音色; speed: 语速(0-100)
// 返回 PCM 原始音频数据
func (c *XunfeiClient) Synthesize(text string, voice string, speed int) ([]byte, error) {
	if c.appID == "" {
		return nil, errors.New("讯飞 AppID 未配置")
	}

	if voice == "" {
		voice = "x4_enus_luna_assist" // 默认英文女声
	}
	if speed <= 0 || speed > 100 {
		speed = 50 // 默认中等语速
	}

	authURL, err := c.buildAuthURL("wss://tts-api.xfyun.cn/v2/tts")
	if err != nil {
		return nil, err
	}

	conn, _, err := websocket.DefaultDialer.Dial(authURL, nil)
	if err != nil {
		return nil, fmt.Errorf("讯飞 TTS WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 发送合成请求（TTS 只需发一帧请求）
	reqFrame := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": c.appID,
		},
		"business": map[string]interface{}{
			"aue":    "raw",                    // 输出格式: PCM
			"auf":    "audio/L16;rate=16000",   // 采样率
			"vcn":    voice,                    // 发音人
			"speed":  speed,                    // 语速 (0-100)
			"volume": 50,                       // 音量
			"pitch":  50,                       // 音调
			"tte":    "utf8",                   // 文本编码
			"sfl":    1,                        // 流式返回
		},
		"data": map[string]interface{}{
			"status": 2, // TTS 只有一帧请求，status=2 表示结束
			"text":   base64.StdEncoding.EncodeToString([]byte(text)),
		},
	}
	if err := conn.WriteJSON(reqFrame); err != nil {
		return nil, fmt.Errorf("发送 TTS 请求失败: %w", err)
	}

	// 接收音频帧并拼接
	var audioBuf []byte
	for {
		var frame struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Data    struct {
				Status int    `json:"status"`
				Audio  string `json:"audio"` // base64 PCM
				Ced    string `json:"ced"`   // 合成进度
			} `json:"data"`
		}
		if err := conn.ReadJSON(&frame); err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				break
			}
			return nil, fmt.Errorf("读取 TTS 音频帧失败: %w", err)
		}
		if frame.Code != 0 {
			return nil, fmt.Errorf("讯飞 TTS 错误 %d: %s", frame.Code, frame.Message)
		}
		if frame.Data.Audio != "" {
			chunk, err := base64.StdEncoding.DecodeString(frame.Data.Audio)
			if err == nil {
				audioBuf = append(audioBuf, chunk...)
			}
		}
		if frame.Data.Status == 2 {
			break
		}
	}

	return audioBuf, nil
}
