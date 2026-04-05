package service

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"

	"github.com/speakpro/go-services/internal/config"
)

// FishTTSClient Fish Audio TTS 服务客户端
type FishTTSClient struct {
	apiKey  string
	model   string
	voiceID string // reference_id（可选，指定特定音色）
}

func NewFishTTSClient(cfg *config.Config) *FishTTSClient {
	return &FishTTSClient{
		apiKey:  cfg.FishAudioAPIKey,
		model:   cfg.FishAudioModel,
		voiceID: cfg.FishAudioVoiceID,
	}
}

// ttsRequest Fish Audio TTS 请求体
type ttsRequest struct {
	Text        string      `json:"text"`
	ReferenceID string      `json:"reference_id,omitempty"`
	Format      string      `json:"format,omitempty"`
	SampleRate  int         `json:"sample_rate,omitempty"`
	Latency     string      `json:"latency,omitempty"`
	Prosody     *ttsProsody `json:"prosody,omitempty"`
}

type ttsProsody struct {
	Speed  float64 `json:"speed,omitempty"`
	Volume float64 `json:"volume,omitempty"`
}

// Synthesize 调用 Fish Audio TTS API 合成语音
// 返回 MP3 格式音频数据
func (c *FishTTSClient) Synthesize(text string, speed float64) ([]byte, error) {
	if c.apiKey == "" {
		return nil, fmt.Errorf("Fish Audio API Key 未配置")
	}

	reqBody := ttsRequest{
		Text:        text,
		ReferenceID: c.voiceID,
		Format:      "mp3",
		Latency:     "balanced",
	}

	if speed > 0 && speed != 1.0 {
		reqBody.Prosody = &ttsProsody{Speed: speed}
	}

	jsonData, err := json.Marshal(reqBody)
	if err != nil {
		return nil, fmt.Errorf("请求序列化失败: %w", err)
	}

	req, err := http.NewRequest("POST", "https://api.fish.audio/v1/tts", bytes.NewReader(jsonData))
	if err != nil {
		return nil, fmt.Errorf("创建请求失败: %w", err)
	}

	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("model", c.model)

	log.Printf("[FishTTS] 合成请求: text_len=%d, model=%s, voice=%s", len(text), c.model, c.voiceID)

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		return nil, fmt.Errorf("Fish Audio 请求失败: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != 200 {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("Fish Audio 错误 %d: %s", resp.StatusCode, string(body))
	}

	audioData, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("读取音频数据失败: %w", err)
	}

	log.Printf("[FishTTS] 合成成功: audio_size=%d bytes, format=mp3", len(audioData))
	return audioData, nil
}

// IsConfigured 检查是否已配置
func (c *FishTTSClient) IsConfigured() bool {
	return c.apiKey != ""
}
