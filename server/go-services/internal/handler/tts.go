package handler

import (
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
func (h *TTSHandler) Synthesize(c *gin.Context) {
	var req struct {
		Text  string `json:"text" binding:"required"`
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

	if req.Voice == "" {
		req.Voice = "x4_enus_luna_assist" // 默认英文女声
	}
	if req.Speed == 0 {
		req.Speed = 5 // 中等语速
	}

	// TODO: 调用讯飞 TTS API
	// TODO: 将合成音频上传 OSS 并返回 URL

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"audio_url": "TODO: OSS URL",
			"duration":  0,
		},
	})
}
