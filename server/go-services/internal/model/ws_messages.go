package model

import "encoding/json"

// ========================
// 客户端 → 服务端 消息
// ========================

// WSClientMessage 通用客户端消息信封
type WSClientMessage struct {
	Type string          `json:"type"` // session_init, audio_chunk, audio_complete, text, pong
	Data json.RawMessage `json:"data,omitempty"`
}

// SessionInitData 会话初始化数据
type SessionInitData struct {
	SessionID string `json:"session_id"`
	ExamType  string `json:"exam_type"` // IELTS, TOEFL
	Section   string `json:"section"`   // Part1, Part2, Part3, Independent, Integrated
	Mode      string `json:"mode"`      // conversation, read_aloud, follow_read, mock_exam
}

// AudioChunkData 音频块数据（base64 编码的 int16 PCM）
type AudioChunkData struct {
	Sequence int    `json:"sequence"`
	AudioB64 string `json:"audio_b64"`
	IsFinal  bool   `json:"is_final"`
}

// AudioCompleteData 音频结束信号
type AudioCompleteData struct {
	SessionID     string `json:"session_id"`
	ReferenceText string `json:"reference_text,omitempty"`
}

// TextMessageData 文本消息
type TextMessageData struct {
	Content string `json:"content"`
}

// ========================
// 服务端 → 客户端 消息
// ========================

// WSServerMessage 通用服务端消息信封
type WSServerMessage struct {
	Type string      `json:"type"` // session_ready, transcript, examiner, score_update, error, ping
	Data interface{} `json:"data"`
}

// SessionReadyData 会话就绪响应
type SessionReadyData struct {
	SessionID        string `json:"session_id"`
	ExaminerGreeting string `json:"examiner_greeting"`
	TimeLimitSec     int    `json:"time_limit_sec"`
	GreetingTTSB64   string `json:"greeting_tts_b64,omitempty"`
}

// TranscriptData 实时转写结果
type TranscriptData struct {
	Text    string `json:"text"`
	IsFinal bool   `json:"is_final"`
}

// ExaminerData 考官回复
type ExaminerData struct {
	Text       string `json:"text"`
	TTSAudioB64 string `json:"tts_audio_b64,omitempty"`
}

// ScoreUpdateData 评分更新
type ScoreUpdateData struct {
	Pronunciation *PronunciationScore `json:"pronunciation,omitempty"`
	Grammar       *GrammarScore       `json:"grammar,omitempty"`
	Content       *ContentScore       `json:"content,omitempty"`
	Overall       float64             `json:"overall"`
	AIFeedback    string              `json:"ai_feedback,omitempty"`
}

// ErrorData 错误消息
type ErrorData struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

// ProcessingData 处理进度（可选）
type ProcessingData struct {
	Step    string `json:"step"`    // transcribing, evaluating, generating_feedback
	Message string `json:"message"`
}

// ========================
// 辅助常量
// ========================

// 客户端消息类型
const (
	MsgTypeSessionInit   = "session_init"
	MsgTypeAudioChunk    = "audio_chunk"
	MsgTypeAudioComplete = "audio_complete"
	MsgTypeText          = "text"
	MsgTypePong          = "pong"
)

// 服务端消息类型
const (
	MsgTypeSessionReady = "session_ready"
	MsgTypeTranscript   = "transcript"
	MsgTypeExaminer     = "examiner"
	MsgTypeScoreUpdate  = "score_update"
	MsgTypeError        = "error"
	MsgTypePing         = "ping"
	MsgTypeProcessing   = "processing"
)
