package handler

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
	"github.com/speakpro/go-services/internal/service"
	"github.com/speakpro/go-services/pkg/ws"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

// ConversationHandler 处理 WebSocket AI 对话
type ConversationHandler struct {
	hub            *ws.Hub
	orchestrator   *service.Orchestrator
	xunfei         *service.XunfeiClient
	qwen           *service.QwenClient
	fishTTS        *service.FishTTSClient
	sessionManager *service.SessionManager
	nestBaseURL    string
}

func NewConversationHandler(hub *ws.Hub, orch *service.Orchestrator, xunfei *service.XunfeiClient, qwen *service.QwenClient, fishTTS *service.FishTTSClient) *ConversationHandler {
	cfg := config.Load()
	return &ConversationHandler{
		hub:            hub,
		orchestrator:   orch,
		xunfei:         xunfei,
		qwen:           qwen,
		fishTTS:        fishTTS,
		sessionManager: service.NewSessionManager(),
		nestBaseURL:    cfg.NestAPIBaseURL,
	}
}

// Connect 建立 WebSocket 连接进行 AI 对话
func (h *ConversationHandler) Connect(c *gin.Context) {
	sessionID := c.Param("sessionId")
	if sessionID == "" {
		c.JSON(400, gin.H{"error": "缺少 sessionId 路径参数"})
		return
	}

	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("[Conversation] WebSocket 升级失败: %v", err)
		return
	}

	userID, _ := c.Get("userId")

	client := ws.NewClient(h.hub, conn, userID.(string))
	client.SessionID = sessionID
	h.hub.Register <- client

	log.Printf("[Conversation] 新连接: user=%s, session=%s", userID, sessionID)

	// 启动读写协程
	go client.WritePump()
	go client.ReadPump(func(msgType int, data []byte) {
		h.handleMessage(client, msgType, data)
	})
}

// handleMessage 路由 WebSocket 消息
func (h *ConversationHandler) handleMessage(client *ws.Client, msgType int, data []byte) {
	// 解析消息信封
	var msg model.WSClientMessage
	if err := json.Unmarshal(data, &msg); err != nil {
		log.Printf("[Conversation] JSON 解析失败: %v", err)
		h.sendError(client, "INVALID_JSON", "消息格式错误")
		return
	}

	switch msg.Type {
	case model.MsgTypeSessionInit:
		h.handleSessionInit(client, msg.Data)
	case model.MsgTypeAudioChunk:
		h.handleAudioChunk(client, msg.Data)
	case model.MsgTypeAudioComplete:
		h.handleAudioComplete(client, msg.Data)
	case model.MsgTypeText:
		h.handleTextMessage(client, msg.Data)
	case model.MsgTypePong:
		// 心跳响应，无需处理
	default:
		log.Printf("[Conversation] 未知消息类型: %s", msg.Type)
		h.sendError(client, "UNKNOWN_TYPE", fmt.Sprintf("未知消息类型: %s", msg.Type))
	}
}

// handleSessionInit 处理会话初始化
func (h *ConversationHandler) handleSessionInit(client *ws.Client, rawData json.RawMessage) {
	var initData model.SessionInitData
	if err := json.Unmarshal(rawData, &initData); err != nil {
		log.Printf("[Conversation] session_init 数据解析失败: %v", err)
		h.sendError(client, "INVALID_DATA", "session_init 数据格式错误")
		return
	}

	log.Printf("[Conversation] 会话初始化: session=%s, exam=%s, section=%s, mode=%s",
		initData.SessionID, initData.ExamType, initData.Section, initData.Mode)

	// 创建会话状态
	h.sessionManager.CreateSession(initData.SessionID, initData.ExamType, initData.Section, initData.Mode)
	client.SessionID = initData.SessionID

	// 生成考官开场白
	greeting := h.generateGreeting(initData.ExamType, initData.Section)

	// 记录到对话历史
	h.sessionManager.AppendMessage(initData.SessionID, "examiner", greeting)

	// 计算时间限制
	timeLimit := h.getTimeLimit(initData.ExamType, initData.Section)

	// TTS 合成开场白语音
	var greetingTTSB64 string
	ttsBytes, ttsErr := h.synthesizeTTS(greeting)
	if ttsErr != nil {
		log.Printf("[Conversation] 开场白 TTS 失败（降级为纯文本）: %v", ttsErr)
	} else if len(ttsBytes) > 0 {
		greetingTTSB64 = base64.StdEncoding.EncodeToString(ttsBytes)
	}

	// 发送 session_ready 响应
	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeSessionReady,
		Data: model.SessionReadyData{
			SessionID:        initData.SessionID,
			ExaminerGreeting: greeting,
			TimeLimitSec:     timeLimit,
			GreetingTTSB64:   greetingTTSB64,
		},
	})
}

// handleAudioChunk 累积音频数据
func (h *ConversationHandler) handleAudioChunk(client *ws.Client, rawData json.RawMessage) {
	var chunkData model.AudioChunkData
	if err := json.Unmarshal(rawData, &chunkData); err != nil {
		h.sendError(client, "INVALID_DATA", "audio_chunk 数据格式错误")
		return
	}

	// base64 解码
	audioBytes, err := base64.StdEncoding.DecodeString(chunkData.AudioB64)
	if err != nil {
		h.sendError(client, "DECODE_FAILED", "base64 解码失败")
		return
	}

	// 累积到会话缓冲区
	h.sessionManager.AccumulateAudio(client.SessionID, audioBytes)

	if chunkData.Sequence%50 == 0 {
		log.Printf("[Conversation] 音频累积中: session=%s, seq=%d, chunk_size=%d",
			client.SessionID, chunkData.Sequence, len(audioBytes))
	}
}

// handleAudioComplete 音频结束 → 执行评测流水线
func (h *ConversationHandler) handleAudioComplete(client *ws.Client, rawData json.RawMessage) {
	var completeData model.AudioCompleteData
	if err := json.Unmarshal(rawData, &completeData); err != nil {
		h.sendError(client, "INVALID_DATA", "audio_complete 数据格式错误")
		return
	}

	sessionID := client.SessionID
	if completeData.SessionID != "" {
		sessionID = completeData.SessionID
	}

	// 获取累积的音频数据
	audioData := h.sessionManager.GetAccumulatedAudio(sessionID)
	if len(audioData) == 0 {
		h.sendError(client, "NO_AUDIO", "没有收到音频数据")
		return
	}

	log.Printf("[Conversation] 开始评测: session=%s, audio_size=%d bytes", sessionID, len(audioData))

	// 发送处理进度
	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeProcessing,
		Data: model.ProcessingData{Step: "transcribing", Message: "正在识别语音..."},
	})

	// 在 goroutine 中执行评测（避免阻塞 ReadPump）
	go h.runEvaluationPipeline(client, sessionID, audioData, completeData.ReferenceText)
}

// runEvaluationPipeline 异步执行完整评测流水线
func (h *ConversationHandler) runEvaluationPipeline(client *ws.Client, sessionID string, audioData []byte, referenceText string) {
	state := h.sessionManager.GetSession(sessionID)
	if state == nil {
		h.sendError(client, "SESSION_NOT_FOUND", "会话不存在")
		return
	}

	// 步骤 1: ASR 语音转写
	transcript, err := h.xunfei.Recognize(audioData)
	if err != nil {
		log.Printf("[Conversation] ASR 失败: %v", err)
		h.sendError(client, "ASR_FAILED", "语音识别失败，请重试")
		h.sessionManager.ClearAudioBuffer(sessionID)
		return
	}

	// 发送转写结果
	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeTranscript,
		Data: model.TranscriptData{Text: transcript, IsFinal: true},
	})

	// 记录学生发言到对话历史
	h.sessionManager.AppendMessage(sessionID, "student", transcript)

	// 步骤 2: 发送评测进度
	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeProcessing,
		Data: model.ProcessingData{Step: "evaluating", Message: "正在评测发音..."},
	})

	// 使用 orchestrator 执行完整评测
	ref := referenceText
	if ref == "" {
		ref = transcript // 如果没有参考文本，用转写结果做自评
	}
	result, err := h.orchestrator.EvaluateAudio(audioData, ref)
	if err != nil {
		log.Printf("[Conversation] 评测流水线失败: %v", err)
		h.sendError(client, "EVALUATION_FAILED", "评测失败")
		h.sessionManager.ClearAudioBuffer(sessionID)
		return
	}

	// 发送评分更新
	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeScoreUpdate,
		Data: model.ScoreUpdateData{
			Pronunciation: &result.Pronunciation,
			Grammar:       &result.Grammar,
			Content:       &result.Content,
			Overall:       result.Overall,
			AIFeedback:    result.AIFeedback,
		},
	})

	// 步骤 3: 如果是对话模式，生成考官追问
	if state.Mode == "conversation" {
		_ = client.SendJSON(model.WSServerMessage{
			Type: model.MsgTypeProcessing,
			Data: model.ProcessingData{Step: "generating_response", Message: "考官正在思考..."},
		})

		history := h.sessionManager.GetHistory(sessionID)
		examinerReply, err := h.orchestrator.GenerateExaminerResponse(history, state.ExamType, state.Section)
		if err != nil {
			log.Printf("[Conversation] 考官回复生成失败: %v", err)
			examinerReply = "I see. Can you elaborate on that point?"
		}

		// 记录考官回复
		h.sessionManager.AppendMessage(sessionID, "examiner", examinerReply)

		// TTS 合成考官语音
		var ttsB64 string
		ttsBytes, ttsErr := h.synthesizeTTS(examinerReply)
		if ttsErr != nil {
			log.Printf("[Conversation] TTS 合成失败（降级为纯文本）: %v", ttsErr)
		} else if len(ttsBytes) > 0 {
			ttsB64 = base64.StdEncoding.EncodeToString(ttsBytes)
			log.Printf("[Conversation] TTS 合成成功: %d bytes", len(ttsBytes))
		}

		_ = client.SendJSON(model.WSServerMessage{
			Type: model.MsgTypeExaminer,
			Data: model.ExaminerData{
				Text:        examinerReply,
				TTSAudioB64: ttsB64,
			},
		})
	}

	// 步骤 4: 回写评分到 NestJS（异步，不阻塞客户端）
	go h.callbackScores(sessionID, result)

	// 清空音频缓冲区，准备下一轮录音
	h.sessionManager.ClearAudioBuffer(sessionID)

	log.Printf("[Conversation] 评测完成: session=%s, overall=%.1f", sessionID, result.Overall)
}

// handleTextMessage 处理文本消息（备用）
func (h *ConversationHandler) handleTextMessage(client *ws.Client, rawData json.RawMessage) {
	var textData model.TextMessageData
	if err := json.Unmarshal(rawData, &textData); err != nil {
		h.sendError(client, "INVALID_DATA", "text 数据格式错误")
		return
	}

	sessionID := client.SessionID
	state := h.sessionManager.GetSession(sessionID)
	if state == nil {
		h.sendError(client, "SESSION_NOT_FOUND", "会话不存在，请先发送 session_init")
		return
	}

	// 记录学生文本消息
	h.sessionManager.AppendMessage(sessionID, "student", textData.Content)

	// 生成考官回复
	history := h.sessionManager.GetHistory(sessionID)
	examinerReply, err := h.orchestrator.GenerateExaminerResponse(history, state.ExamType, state.Section)
	if err != nil {
		log.Printf("[Conversation] 文本回复生成失败: %v", err)
		h.sendError(client, "AI_FAILED", "AI 回复生成失败")
		return
	}

	h.sessionManager.AppendMessage(sessionID, "examiner", examinerReply)

	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeExaminer,
		Data: model.ExaminerData{Text: examinerReply},
	})
}

// callbackScores 将评分回写到 NestJS
func (h *ConversationHandler) callbackScores(sessionID string, result *model.AssessmentResult) {
	url := fmt.Sprintf("%s/practice/sessions/%s/scores", h.nestBaseURL, sessionID)

	payload := map[string]interface{}{
		"pronunciationScore": result.Pronunciation,
		"fluencyScore":       result.Fluency,
		"grammarScore":       result.Grammar,
		"contentScore":       result.Content,
		"overallScore":       result.Overall,
		"aiFeedback":         result.AIFeedback,
		"transcript":         result.Transcript,
	}
	body, err := json.Marshal(payload)
	if err != nil {
		log.Printf("[Conversation] 评分序列化失败: %v", err)
		return
	}

	req, err := http.NewRequest("PATCH", url, bytes.NewReader(body))
	if err != nil {
		log.Printf("[Conversation] 构建回写请求失败: %v", err)
		return
	}
	req.Header.Set("Content-Type", "application/json")
	// 内部服务调用，可以加一个内部 Token 或跳过认证
	// 这里暂时不带 JWT，NestJS 需要对内部回调路由放行

	resp, err := http.DefaultClient.Do(req)
	if err != nil {
		log.Printf("[Conversation] NestJS 评分回写失败: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		respBody, _ := io.ReadAll(resp.Body)
		log.Printf("[Conversation] NestJS 回写返回 %d: %s", resp.StatusCode, string(respBody))
		return
	}

	log.Printf("[Conversation] 评分已回写到 NestJS: session=%s", sessionID)
}

// sendError 发送错误消息到客户端
func (h *ConversationHandler) sendError(client *ws.Client, code, message string) {
	_ = client.SendJSON(model.WSServerMessage{
		Type: model.MsgTypeError,
		Data: model.ErrorData{Code: code, Message: message},
	})
}

// generateGreeting 根据考试类型生成考官开场白
func (h *ConversationHandler) generateGreeting(examType, section string) string {
	switch examType {
	case "IELTS":
		switch section {
		case "Part1":
			return "Good afternoon. My name is the AI examiner. I'd like to ask you some questions about yourself and your interests. Let's begin. Can you tell me your full name, please?"
		case "Part2":
			return "Now, I'm going to give you a topic and I'd like you to talk about it for one to two minutes. Before you talk, you'll have one minute to think about what you're going to say. Here is your topic card."
		case "Part3":
			return "We've been talking about your personal experiences. I'd like to discuss some more general questions related to this topic. Let's consider this from a broader perspective."
		default:
			return "Good afternoon. Let's begin the speaking test."
		}
	case "TOEFL":
		switch section {
		case "Independent":
			return "In this task, you will be asked to give your opinion on a topic. You will have 15 seconds to prepare and 45 seconds to speak. Listen carefully to the question."
		case "Integrated":
			return "In this task, you will read a short passage and listen to a lecture on the same topic. You will then be asked to combine the information. You will have 30 seconds to prepare and 60 seconds to speak."
		default:
			return "Welcome to the TOEFL speaking practice. Let's begin."
		}
	default:
		return "Hello! Welcome to SpeakPro. Let's start your speaking practice."
	}
}

// synthesizeTTS 合成语音 — 优先 Fish Audio，失败回退讯飞
func (h *ConversationHandler) synthesizeTTS(text string) ([]byte, error) {
	if h.fishTTS != nil && h.fishTTS.IsConfigured() {
		data, err := h.fishTTS.Synthesize(text, 1.0)
		if err == nil {
			return data, nil
		}
		log.Printf("[Conversation] Fish Audio TTS 失败，回退讯飞: %v", err)
	}
	return h.xunfei.Synthesize(text, "", 50)
}

// getTimeLimit 获取不同考试类型/部分的时间限制（秒）
func (h *ConversationHandler) getTimeLimit(examType, section string) int {
	switch examType {
	case "IELTS":
		switch section {
		case "Part1":
			return 300 // 5 分钟
		case "Part2":
			return 180 // 3 分钟（含 1 分钟准备）
		case "Part3":
			return 300 // 5 分钟
		}
	case "TOEFL":
		switch section {
		case "Independent":
			return 60 // 45 秒回答 + 15 秒准备
		case "Integrated":
			return 90 // 60 秒回答 + 30 秒准备
		}
	}
	return 120 // 默认 2 分钟
}
