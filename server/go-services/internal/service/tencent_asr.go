package service

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"

	"github.com/speakpro/go-services/internal/config"
)

// TencentASRClient 腾讯云实时语音识别客户端
// 使用 SentenceRecognition (一句话识别) REST API
type TencentASRClient struct {
	secretID  string
	secretKey string
	appID     string
	region    string
}

func NewTencentASRClient(cfg *config.Config) *TencentASRClient {
	return &TencentASRClient{
		secretID:  cfg.TencentSecretID,
		secretKey: cfg.TencentSecretKey,
		appID:     cfg.TencentASRAppID,
		region:    cfg.TencentRegion,
	}
}

// IsConfigured 检查是否已配置
func (c *TencentASRClient) IsConfigured() bool {
	return c.secretID != "" && c.secretKey != ""
}

// Recognize 语音识别 — 将 PCM 音频转写为文本
// 输入: PCM 或 WAV 格式音频数据（自动去除 WAV 头）
// 输出: 识别的文本字符串
func (c *TencentASRClient) Recognize(audioData []byte) (string, error) {
	if !c.IsConfigured() {
		return "", fmt.Errorf("腾讯云 ASR 未配置")
	}

	// 去除 WAV 头（复用 xunfei.go 中的函数）
	audioData = stripWAVHeader(audioData)

	// Base64 编码
	audioB64 := base64.StdEncoding.EncodeToString(audioData)

	log.Printf("[TencentASR] 识别请求: audio_size=%d bytes", len(audioData))

	// 构建请求体
	reqBody := map[string]interface{}{
		"ProjectId":      0,
		"SubServiceType": 2, // 一句话识别
		"EngSerViceType": "16k_en", // 英文 16kHz
		"SourceType":     1,        // 内联音频数据
		"VoiceFormat":    "pcm",
		"Data":           audioB64,
		"DataLen":        len(audioData),
	}

	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return "", fmt.Errorf("请求序列化失败: %w", err)
	}

	// 调用腾讯云 API
	respBytes, err := tencentCloudRequest(
		"asr", "SentenceRecognition", "2019-06-14",
		c.secretID, c.secretKey, c.region, string(bodyBytes),
	)
	if err != nil {
		return "", fmt.Errorf("腾讯云 ASR 请求失败: %w", err)
	}

	// 解析响应
	var resp struct {
		Response struct {
			Result        string `json:"Result"`
			AudioDuration int    `json:"AudioDuration"`
			RequestId     string `json:"RequestId"`
			Error         *struct {
				Code    string `json:"Code"`
				Message string `json:"Message"`
			} `json:"Error"`
		} `json:"Response"`
	}

	if err := json.Unmarshal(respBytes, &resp); err != nil {
		return "", fmt.Errorf("响应解析失败: %w", err)
	}

	if resp.Response.Error != nil {
		return "", fmt.Errorf("腾讯云 ASR 错误 [%s]: %s",
			resp.Response.Error.Code, resp.Response.Error.Message)
	}

	log.Printf("[TencentASR] 识别成功: text_len=%d, duration=%dms",
		len(resp.Response.Result), resp.Response.AudioDuration)

	return resp.Response.Result, nil
}
