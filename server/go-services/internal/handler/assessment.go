package handler

import (
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/speakpro/go-services/internal/service"
)

type AssessmentHandler struct {
	orchestrator *service.Orchestrator
}

func NewAssessmentHandler(orch *service.Orchestrator) *AssessmentHandler {
	return &AssessmentHandler{orchestrator: orch}
}

// Evaluate 发音评测接口
func (h *AssessmentHandler) Evaluate(c *gin.Context) {
	file, err := c.FormFile("audio")
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "音频文件缺失",
		})
		return
	}

	referenceText := c.PostForm("reference_text")
	category := c.PostForm("category") // read_sentence, read_word

	_ = file
	_ = category

	// TODO: 调用讯飞语音评测 API
	// TODO: 返回音素级评分结果

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"reference_text": referenceText,
			"status":         "evaluated",
		},
	})
}

// Feedback 生成 AI 综合反馈
func (h *AssessmentHandler) Feedback(c *gin.Context) {
	var req struct {
		SessionID string `json:"session_id" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "参数错误",
		})
		return
	}

	// TODO: 从数据库读取评分数据
	// TODO: 调用通义千问生成综合反馈和改进建议

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"session_id": req.SessionID,
			"feedback":   "TODO: AI 生成反馈",
		},
	})
}
