package handler

import (
	"encoding/base64"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/speakpro/go-services/internal/service"
)

type TTSHandler struct {
	xunfei *service.XunfeiClient
}

func NewTTSHandler(xunfei *service.XunfeiClient) *TTSHandler {
	return &TTSHandler{xunfei: xunfei}
}

// Synthesize 调用讯飞 TTS 合成英文语音
// 请求: {"text": "...", "voice": "x4_enus_luna_assist", "speed": 50}
// 响应: {"audio_base64": "...", "format": "pcm", "sample_rate": 16000}
func (h *TTSHandler) Synthesize(c *gin.Context) {
	var req struct {
		Text  string `json:"text"  binding:"required"`
		Voice string `json:"voice"`
		Speed int    `json:"speed"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "参数错误: text 不能为空",
		})
		return
	}

	// 默认参数
	if req.Voice == "" {
		req.Voice = "x4_lingxiaolu_oral" // 默认音色（支持中英文）
	}
	if req.Speed <= 0 || req.Speed > 100 {
		req.Speed = 50 // 中等语速
	}

	// 调用讯飞 TTS
	audioData, err := h.xunfei.Synthesize(req.Text, req.Voice, req.Speed)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "语音合成失败: " + err.Error(),
		})
		return
	}

	// 返回 base64 编码的 PCM 音频
	// 客户端可直接用 AudioPlayer 播放 PCM 数据（16kHz, 16bit, mono）
	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"audio_base64": base64.StdEncoding.EncodeToString(audioData),
			"format":       "pcm",
			"sample_rate":  16000,
			"channels":     1,
			"bit_depth":    16,
			"duration_ms":  estimateDuration(len(audioData)),
		},
	})
}

// estimateDuration 根据 PCM 字节数估算时长（ms）
// 公式: bytes / (sample_rate * channels * bit_depth/8) * 1000
func estimateDuration(bytes int) int {
	// 16kHz, 16bit, mono: 每秒 32000 字节
	return bytes * 1000 / 32000
}
