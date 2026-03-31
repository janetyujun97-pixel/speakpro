package handler

import (
	"io"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/speakpro/go-services/internal/service"
)

type AssessmentHandler struct {
	orchestrator *service.Orchestrator
	xunfei       *service.XunfeiClient
	qwen         *service.QwenClient
}

func NewAssessmentHandler(orch *service.Orchestrator, xunfei *service.XunfeiClient, qwen *service.QwenClient) *AssessmentHandler {
	return &AssessmentHandler{orchestrator: orch, xunfei: xunfei, qwen: qwen}
}

// Evaluate 发音评测接口 — 只做发音评测，不做全流水线
// 表单字段: audio(文件), reference_text(字符串), category(字符串, 可选)
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
	if referenceText == "" {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "reference_text 不能为空",
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

	// 调用讯飞发音评测
	pronScore, err := h.xunfei.Assess(audioData, referenceText)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "发音评测失败: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"reference_text":     referenceText,
			"pronunciation_score": pronScore,
		},
	})
}

// Feedback 根据 session_id 生成 AI 综合反馈
// JSON 请求体: {"session_id": "...", "transcript": "...", "reference_text": "..."}
func (h *AssessmentHandler) Feedback(c *gin.Context) {
	var req struct {
		SessionID     string `json:"session_id"     binding:"required"`
		Transcript    string `json:"transcript"     binding:"required"`
		ReferenceText string `json:"reference_text"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{
			"code":    400,
			"message": "参数错误: session_id 和 transcript 为必填项",
		})
		return
	}

	// 先进行语法纠错
	grammarScore, err := h.qwen.CorrectGrammar(req.Transcript)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "语法分析失败: " + err.Error(),
		})
		return
	}

	// 生成综合反馈
	feedback, err := h.qwen.GenerateFeedback(req.Transcript, req.ReferenceText, nil)
	if err != nil {
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "生成反馈失败: " + err.Error(),
		})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data": gin.H{
			"session_id":    req.SessionID,
			"grammar_score": grammarScore,
			"ai_feedback":   feedback,
		},
	})
}
