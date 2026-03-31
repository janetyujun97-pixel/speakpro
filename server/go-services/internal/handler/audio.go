package handler

import (
	"io"
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

// Upload 处理音频文件上传并触发完整 AI 评测流水线
// 表单字段: audio(文件), session_id(字符串), reference_text(字符串)
func (h *AudioHandler) Upload(c *gin.Context) {
	file, err := c.FormFile("audio")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "音频文件缺失，请上传 audio 字段",
		})
		return
	}

	sessionID := c.PostForm("session_id")
	referenceText := c.PostForm("reference_text")

	if sessionID == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "session_id 不能为空",
		})
		return
	}

	// 读取音频数据
	src, err := file.Open()
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "读取音频文件失败",
		})
		return
	}
	defer src.Close()

	audioData, err := io.ReadAll(src)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "读取音频数据失败",
		})
		return
	}

	// 调用 AI 评测流水线（ASR → 发音评测 → 语法纠错 → 综合反馈）
	result, err := h.orchestrator.EvaluateAudio(audioData, referenceText)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "AI 评测失败: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"session_id": sessionID,
			"status":     "completed",
			"result":     result,
		},
	})
}
