package service

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"

	"github.com/speakpro/go-services/internal/config"
)

// MiMoTTSClient 小米 MiMo-V2-TTS 客户端
// API 文档: https://platform.xiaomimimo.com/#/docs/usage-guide/speech-synthesis
type MiMoTTSClient struct {
	apiKey string
	model  string
	voice  string
	client *http.Client
}

func NewMiMoTTSClient(cfg *config.Config) *MiMoTTSClient {
	return &MiMoTTSClient{
		apiKey: cfg.MiMoAPIKey,
		model:  cfg.MiMoModel,
		voice:  cfg.MiMoVoice,
		client: &http.Client{Timeout: 30 * time.Second},
	}
}

// MiMo API 请求结构（OpenAI chat completions 兼容格式）
type mimoRequest struct {
	Model    string        `json:"model"`
	Messages []mimoMessage `json:"messages"`
	Audio    *mimoAudio    `json:"audio,omitempty"`
}

type mimoMessage struct {
	Role    string `json:"role"`
	Content string `json:"content"`
}

type mimoAudio struct {
	Format string `json:"format,omitempty"`
	Voice  string `json:"voice,omitempty"`
}

// MiMo API 响应结构
type mimoResponse struct {
	Choices []struct {
		Message struct {
			Audio struct {
				Data string `json:"data"` // base64 编码的音频
			} `json:"audio"`
		} `json:"message"`
	} `json:"choices"`
	Error *struct {
		Message string `json:"message"`
		Code    string `json:"code"`
	} `json:"error,omitempty"`
}

// Synthesize 调用 MiMo-V2-TTS 合成语音
// 返回 WAV 格式音频数据
func (c *MiMoTTSClient) Synthesize(text string, speed float64) ([]byte, error) {
	if c.apiKey == "" {
		return nil, fmt.Errorf("MiMo API Key 未配置")
	}

	voice := c.voice
	if voice == "" {
		voice = "default_en" // 默认英文女声
	}

	// 构建请求
	// MiMo TTS 要求 messages 中包含 assistant role
	reqBody := mimoRequest{
		Model: c.model,
		Messages: []mimoMessage{
			{Role: "user", Content: "请朗读以下内容"},
			{Role: "assistant", Content: text},
		},
		Audio: &mimoAudio{
			Format: "wav",
			Voice:  voice,
		},
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return nil, fmt.Errorf("请求序列化失败: %w", err)
	}

	req, err := http.NewRequest("POST", "https://api.xiaomimimo.com/v1/chat/completions", bytes.NewReader(jsonData))
	if err != nil {
		return nil, fmt.Errorf("创建请求失败: %w", err)
	}

	// MiMo 使用 Bearer 认证头
	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	req.Header.Set("Content-Type", "application/json")

	log.Printf("[MiMoTTS] 合成请求: text_len=%d, model=%s, voice=%s", len(text), c.model, voice)

	resp, err := c.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("MiMo 请求失败: %w", err)
	}
	defer resp.Body.Close()

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("读取响应失败: %w", err)
	}

	if resp.StatusCode != 200 {
		return nil, fmt.Errorf("MiMo 错误 %d: %s", resp.StatusCode, string(body[:min(200, len(body))]))
	}

	// 解析响应
	var mimoResp mimoResponse
	if err := json.Unmarshal(body, &mimoResp); err != nil {
		return nil, fmt.Errorf("响应解析失败: %w", err)
	}

	if mimoResp.Error != nil {
		return nil, fmt.Errorf("MiMo API 错误: %s (%s)", mimoResp.Error.Message, mimoResp.Error.Code)
	}

	if len(mimoResp.Choices) == 0 || mimoResp.Choices[0].Message.Audio.Data == "" {
		return nil, fmt.Errorf("MiMo 返回数据为空")
	}

	// base64 解码音频数据
	audioData, err := base64.StdEncoding.DecodeString(mimoResp.Choices[0].Message.Audio.Data)
	if err != nil {
		return nil, fmt.Errorf("音频 base64 解码失败: %w", err)
	}

	log.Printf("[MiMoTTS] 合成成功: audio_size=%d bytes, format=wav", len(audioData))
	return audioData, nil
}

// IsConfigured 检查是否已配置
func (c *MiMoTTSClient) IsConfigured() bool {
	return c.apiKey != ""
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
