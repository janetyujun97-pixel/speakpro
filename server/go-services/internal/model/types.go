package model

// PronunciationScore 发音评测结果
type PronunciationScore struct {
	Overall    float64        `json:"overall"`
	Phonemes   []PhonemeScore `json:"phonemes"`
	Stress     float64        `json:"stress"`
	Intonation float64        `json:"intonation"`
	Fluency    float64        `json:"fluency"`
	Integrity  float64        `json:"integrity"`
}

type PhonemeScore struct {
	Phoneme string  `json:"phoneme"`
	Score   float64 `json:"score"`
	Ref     string  `json:"ref"` // 参考音素
}

// AssessmentRequest 评测请求
type AssessmentRequest struct {
	AudioData     []byte `json:"audio_data"`
	ReferenceText string `json:"reference_text"`
	Category      string `json:"category"` // read_sentence, read_word, etc.
}

// AssessmentResult 综合评测结果
type AssessmentResult struct {
	Pronunciation PronunciationScore `json:"pronunciation_score"`
	Fluency       FluencyScore       `json:"fluency_score"`
	Grammar       GrammarScore       `json:"grammar_score"`
	Content       ContentScore       `json:"content_score"`
	Overall       float64            `json:"overall_score"`
	AIFeedback    string             `json:"ai_feedback"`
	Transcript    string             `json:"transcript"`
}

type FluencyScore struct {
	Score   float64  `json:"score"`
	Pace    string   `json:"pace"`
	Fillers int      `json:"fillers"`
	Pauses  []Pause  `json:"pauses"`
}

type Pause struct {
	Start    float64 `json:"start"`
	Duration float64 `json:"duration"`
}

type GrammarScore struct {
	Score       float64      `json:"score"`
	Errors      []GramError  `json:"errors"`
	Corrections []string     `json:"corrections"`
}

type GramError struct {
	Text       string `json:"text"`
	Type       string `json:"type"`
	Suggestion string `json:"suggestion"`
}

type ContentScore struct {
	Score      float64 `json:"score"`
	Relevance  float64 `json:"relevance"`
	Vocabulary float64 `json:"vocabulary"`
	Coherence  float64 `json:"coherence"`
}

// ========================
// 完整评测（模考用）
// ========================

// FullEvaluateRequest 完整评测请求
type FullEvaluateRequest struct {
	AudioB64      string `json:"audioB64"`
	ReferenceText string `json:"referenceText"`
	ExamType      string `json:"examType"`
	Section       string `json:"section"`
	QuestionID    string `json:"questionId,omitempty"`
}

// FullEvaluateResult 完整评测结果
type FullEvaluateResult struct {
	Transcript         string              `json:"transcript"`
	WordCount          int                 `json:"wordCount"`
	SentenceCount      int                 `json:"sentenceCount"`
	PronunciationScore *PronunciationScore `json:"pronunciationScore"`
	GrammarScore       *GrammarScore       `json:"grammarScore"`
	ContentScore       *ContentScore       `json:"contentScore,omitempty"`
	OverallScore       float64             `json:"overallScore"`
	AIFeedback         string              `json:"aiFeedback"`
	RevisedAnswer      *RevisedAnswer      `json:"revisedAnswer"`
	MindMap            *MindMapData        `json:"mindMap"`
	Keywords           []KeywordItem       `json:"keywords"`
	SampleAnswers      []string            `json:"sampleAnswers"`
	RevisedAudioB64    string              `json:"revisedAudioB64,omitempty"`
}

// RevisedAnswer 修订后的答案
type RevisedAnswer struct {
	Text          string `json:"text"`
	WordCount     int    `json:"wordCount"`
	SentenceCount int    `json:"sentenceCount"`
}

// MindMapData 思维导图
type MindMapData struct {
	Title    string        `json:"title"`
	Children []MindMapNode `json:"children"`
}

// MindMapNode 思维导图节点
type MindMapNode struct {
	Label    string        `json:"label"`
	Detail   string        `json:"detail,omitempty"`
	Children []MindMapNode `json:"children,omitempty"`
}

// KeywordItem 关键词
type KeywordItem struct {
	Word            string `json:"word"`
	Phonetic        string `json:"phonetic"`
	PartOfSpeech    string `json:"partOfSpeech"`
	Definition      string `json:"definition"`
	ExampleSentence string `json:"exampleSentence"`
}

// ConversationMessage WebSocket 对话消息
type ConversationMessage struct {
	Type string      `json:"type"` // audio_chunk, transcript, examiner, score_update
	Data interface{} `json:"data"`
}

// TTSRequest 语音合成请求
type TTSRequest struct {
	Text  string `json:"text"`
	Voice string `json:"voice"` // 音色选择
	Speed int    `json:"speed"` // 语速 (1-10)
}

// TTSResponse 语音合成响应
type TTSResponse struct {
	AudioURL string `json:"audio_url"`
	Duration float64 `json:"duration"`
}
