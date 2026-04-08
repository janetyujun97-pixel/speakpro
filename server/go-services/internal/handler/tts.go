package handler

import (
	"encoding/base64"
	"fmt"
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/service"
)

type TTSHandler struct {
	xunfei          *service.XunfeiClient
	fishTTS         *service.FishTTSClient
	mimoTTS         *service.MiMoTTSClient
	defaultProvider string
}

func NewTTSHandler(xunfei *service.XunfeiClient, fishTTS *service.FishTTSClient, mimoTTS *service.MiMoTTSClient) *TTSHandler {
	cfg := config.Load()
	return &TTSHandler{
		xunfei:          xunfei,
		fishTTS:         fishTTS,
		mimoTTS:         mimoTTS,
		defaultProvider: cfg.DefaultTTSProvider,
	}
}

// Synthesize 语音合成 — 支持多 TTS 模型选择
// 请求: {"text": "...", "speed": 50, "provider": "mimo|fish|xunfei"}
// 响应: {"audioB64": "...", "format": "wav|mp3|pcm", "provider": "实际使用的提供商"}
func (h *TTSHandler) Synthesize(c *gin.Context) {
	var req struct {
		Text     string  `json:"text" binding:"required"`
		Speed    float64 `json:"speed"`
		Provider string  `json:"provider"` // 可选：覆盖默认提供商
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "参数错误: text 不能为空",
		})
		return
	}

	// 确定使用哪个提供商
	provider := req.Provider
	if provider == "" {
		provider = h.defaultProvider
	}
	if provider == "" {
		provider = "mimo" // 兜底默认
	}

	// 速度归一化
	speed := 1.0
	if req.Speed > 0 && req.Speed <= 100 {
		speed = 0.5 + (req.Speed/100.0)*1.5
	}

	// 按优先级尝试合成
	audioData, format, usedProvider, err := h.synthesizeWithFallback(req.Text, speed, provider)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "语音合成失败: " + err.Error(),
		})
		return
	}

	sampleRate := 44100
	if format == "pcm" {
		sampleRate = 16000
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"audioB64":   base64.StdEncoding.EncodeToString(audioData),
			"format":     format,
			"sampleRate": sampleRate,
			"provider":   usedProvider,
		},
	})
}

// synthesizeWithFallback 按优先级尝试 TTS 合成，失败自动降级
// 返回: (音频数据, 格式, 实际提供商, 错误)
func (h *TTSHandler) synthesizeWithFallback(text string, speed float64, preferred string) ([]byte, string, string, error) {
	// 构建尝试顺序：preferred → 其他可用提供商
	providers := h.buildProviderOrder(preferred)

	var lastErr error
	for _, p := range providers {
		data, format, err := h.tryProvider(p, text, speed)
		if err == nil {
			return data, format, p, nil
		}
		log.Printf("[TTS] %s 合成失败: %v，尝试下一个...", p, err)
		lastErr = err
	}

	return nil, "", "", lastErr
}

func (h *TTSHandler) buildProviderOrder(preferred string) []string {
	all := []string{"mimo", "fish", "xunfei"}
	result := []string{preferred}
	for _, p := range all {
		if p != preferred {
			result = append(result, p)
		}
	}
	return result
}

func (h *TTSHandler) tryProvider(provider, text string, speed float64) ([]byte, string, error) {
	switch provider {
	case "mimo":
		if h.mimoTTS != nil && h.mimoTTS.IsConfigured() {
			data, err := h.mimoTTS.Synthesize(text, speed)
			return data, "wav", err
		}
		return nil, "", fmt.Errorf("MiMo TTS 未配置")

	case "fish":
		if h.fishTTS != nil && h.fishTTS.IsConfigured() {
			data, err := h.fishTTS.Synthesize(text, speed)
			return data, "mp3", err
		}
		return nil, "", fmt.Errorf("Fish Audio 未配置")

	case "xunfei":
		xunfeiSpeed := int(speed * 50) // 1.0 → 50
		if xunfeiSpeed <= 0 {
			xunfeiSpeed = 50
		}
		data, err := h.xunfei.Synthesize(text, "", xunfeiSpeed)
		return data, "pcm", err

	default:
		return nil, "", fmt.Errorf("未知 TTS 提供商: %s", provider)
	}
}
