package handler

import (
	"encoding/base64"
	"encoding/json"
	"io"
	"log"
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

// Evaluate 发音评测接口
// 支持两种格式：
//   1. multipart form: audio(文件) + reference_text(字符串)
//   2. JSON body: {"audioB64": "base64...", "referenceText": "..."}
func (h *AssessmentHandler) Evaluate(c *gin.Context) {
	var audioData []byte
	var referenceText string

	contentType := c.ContentType()
	log.Printf("[Assessment] Content-Type: %q", contentType)

	if contentType == "application/json" {
		// 先读取原始 body
		rawBody, err := io.ReadAll(c.Request.Body)
		if err != nil || len(rawBody) == 0 {
			log.Printf("[Assessment] 读取 body 失败或为空: err=%v, len=%d", err, len(rawBody))
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "请求体为空"})
			return
		}

		log.Printf("[Assessment] Body 大小: %d bytes, 前100字符: %s", len(rawBody), string(rawBody[:min(100, len(rawBody))]))

		// 手动解析 JSON — 支持 audioB64/audio_b64 两种字段名
		var reqMap map[string]interface{}
		if err := json.Unmarshal(rawBody, &reqMap); err != nil {
			log.Printf("[Assessment] JSON 解析失败: %v", err)
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "JSON 解析失败"})
			return
		}

		// 提取 audioB64（兼容 camelCase 和 snake_case）
		audioB64Str := ""
		if v, ok := reqMap["audioB64"].(string); ok {
			audioB64Str = v
		} else if v, ok := reqMap["audio_b64"].(string); ok {
			audioB64Str = v
		}

		if v, ok := reqMap["referenceText"].(string); ok {
			referenceText = v
		} else if v, ok := reqMap["reference_text"].(string); ok {
			referenceText = v
		}

		if audioB64Str == "" || referenceText == "" {
			log.Printf("[Assessment] 缺少必填字段: audioB64=%d bytes, referenceText=%q, keys=%v",
				len(audioB64Str), referenceText, func() []string {
					keys := make([]string, 0, len(reqMap))
					for k := range reqMap { keys = append(keys, k) }
					return keys
				}())
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "audioB64 和 referenceText 为必填项"})
			return
		}

		decoded, err := base64.StdEncoding.DecodeString(audioB64Str)
		if err != nil {
			log.Printf("[Assessment] base64 解码失败: %v", err)
			c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "audioB64 解码失败"})
			return
		}
		audioData = decoded

	} else {
		// multipart form 格式（Web 端）
		file, err := c.FormFile("audio")
		if err != nil {
			c.JSON(http.StatusBadRequest, gin.H{
				"code":    400,
				"message": "音频文件缺失",
			})
			return
		}

		referenceText = c.PostForm("reference_text")
		if referenceText == "" {
			c.JSON(http.StatusBadRequest, gin.H{
				"code":    400,
				"message": "reference_text 不能为空",
			})
			return
		}

		src, err := file.Open()
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"code":    500,
				"message": "读取音频文件失败",
			})
			return
		}
		defer src.Close()

		audioData, err = io.ReadAll(src)
		if err != nil {
			c.JSON(http.StatusInternalServerError, gin.H{
				"code":    500,
				"message": "读取音频数据失败",
			})
			return
		}
	}

	log.Printf("[Assessment] 开始评测: audio_size=%d, ref_text=%q", len(audioData), referenceText[:min(50, len(referenceText))])

	// 调用讯飞发音评测
	pronScore, err := h.xunfei.Assess(audioData, referenceText)
	if err != nil {
		log.Printf("[Assessment] 评测失败: %v", err)
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
			"referenceText":      referenceText,
			"pronunciationScore": pronScore,
		},
	})
}

// FullEvaluate 完整评测（模考用）— 一次性返回转写+发音+语法+修订+思维导图+关键词+样例
func (h *AssessmentHandler) FullEvaluate(c *gin.Context) {
	rawBody, err := io.ReadAll(c.Request.Body)
	if err != nil || len(rawBody) == 0 {
		c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "请求体为空"})
		return
	}

	var reqMap map[string]interface{}
	if err := json.Unmarshal(rawBody, &reqMap); err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "JSON 解析失败"})
		return
	}

	// 提取字段（兼容 camelCase 和 snake_case）
	audioB64 := getStringField(reqMap, "audioB64", "audio_b64")
	referenceText := getStringField(reqMap, "referenceText", "reference_text")
	examType := getStringField(reqMap, "examType", "exam_type")
	section := getStringField(reqMap, "section", "section")

	if audioB64 == "" || referenceText == "" {
		c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "audioB64 和 referenceText 为必填项"})
		return
	}

	audioData, err := base64.StdEncoding.DecodeString(audioB64)
	if err != nil {
		c.JSON(http.StatusBadRequest, gin.H{"code": 400, "message": "audioB64 解码失败"})
		return
	}

	if examType == "" { examType = "IELTS" }
	if section == "" { section = "Part1" }

	log.Printf("[FullEvaluate] 开始: audio=%d bytes, exam=%s, section=%s", len(audioData), examType, section)

	result, err := h.orchestrator.FullEvaluateAudio(audioData, referenceText, examType, section)
	if err != nil {
		log.Printf("[FullEvaluate] 失败: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{"code": 500, "message": "评测失败: " + err.Error()})
		return
	}

	c.JSON(http.StatusOK, gin.H{
		"code":    0,
		"message": "success",
		"data":    result,
	})
}

func getStringField(m map[string]interface{}, keys ...string) string {
	for _, k := range keys {
		if v, ok := m[k].(string); ok {
			return v
		}
	}
	return ""
}

// Feedback 根据 session_id 生成 AI 综合反馈
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

	grammarScore, err := h.qwen.CorrectGrammar(req.Transcript)
	if err != nil {
		log.Printf("[Assessment] 语法分析失败: %v", err)
		c.JSON(http.StatusInternalServerError, gin.H{
			"code":    500,
			"message": "语法分析失败: " + err.Error(),
		})
		return
	}

	feedback, err := h.qwen.GenerateFeedback(req.Transcript, req.ReferenceText, nil)
	if err != nil {
		log.Printf("[Assessment] 生成反馈失败: %v", err)
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
			"sessionId":    req.SessionID,
			"grammarScore": grammarScore,
			"aiFeedback":   feedback,
		},
	})
}

func min(a, b int) int {
	if a < b {
		return a
	}
	return b
}
