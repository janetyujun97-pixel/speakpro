package handler

import (
	"encoding/base64"
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/speakpro/go-services/internal/service"
)

type TTSHandler struct {
	xunfei  *service.XunfeiClient
	fishTTS *service.FishTTSClient
}

func NewTTSHandler(xunfei *service.XunfeiClient, fishTTS *service.FishTTSClient) *TTSHandler {
	return &TTSHandler{xunfei: xunfei, fishTTS: fishTTS}
}

// Synthesize 语音合成 — 优先使用 Fish Audio，失败回退到讯飞
// 请求: {"text": "...", "speed": 50}
// 响应: {"audio_b64": "...", "format": "mp3", ...}
func (h *TTSHandler) Synthesize(c *gin.Context) {
	var req struct {
		Text  string  `json:"text" binding:"required"`
		Speed float64 `json:"speed"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "参数错误: text 不能为空",
		})
		return
	}

	// 速度归一化：前端传 0-100，Fish Audio 用 0.5-2.0
	speed := 1.0
	if req.Speed > 0 && req.Speed <= 100 {
		speed = 0.5 + (req.Speed / 100.0) * 1.5 // 0→0.5, 50→1.25, 100→2.0
	}

	// 优先 Fish Audio
	if h.fishTTS != nil && h.fishTTS.IsConfigured() {
		audioData, err := h.fishTTS.Synthesize(req.Text, speed)
		if err == nil {
			c.JSON(http.StatusOK, gin.H{
				"code":    0,
				"message": "success",
				"data": gin.H{
					"audioB64":   base64.StdEncoding.EncodeToString(audioData),
					"format":     "mp3",
					"sampleRate": 44100,
				},
			})
			return
		}
		log.Printf("[TTS] Fish Audio 失败，回退到讯飞: %v", err)
	}

	// 回退到讯飞 TTS
	xunfeiSpeed := 50
	if req.Speed > 0 {
		xunfeiSpeed = int(req.Speed)
	}
	audioData, err := h.xunfei.Synthesize(req.Text, "", xunfeiSpeed)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "语音合成失败: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"audioB64":   base64.StdEncoding.EncodeToString(audioData),
			"format":     "pcm",
			"sampleRate": 16000,
		},
	})
}
