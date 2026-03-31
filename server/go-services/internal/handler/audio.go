package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/speakpro/go-services/internal/service"
)

type AudioHandler struct {
	orchestrator *service.Orchestrator
}

func NewAudioHandler(orch *service.Orchestrator) *AudioHandler {
	return &AudioHandler{orchestrator: orch}
}

// Upload 处理音频文件上传并触发评测流水线
func (h *AudioHandler) Upload(c *gin.Context) {
	file, err := c.FormFile("audio")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "音频文件缺失",
		})
		return
	}

	sessionID := c.PostForm("session_id")
	referenceText := c.PostForm("reference_text")

	_ = file
	_ = sessionID
	_ = referenceText

	// TODO: 1. 保存音频到 OSS
	// TODO: 2. 调用 orchestrator 进行评测流水线
	// TODO: 3. 将评测结果写入 practice_sessions 表

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"session_id": sessionID,
			"status":     "processing",
		},
	})
}
