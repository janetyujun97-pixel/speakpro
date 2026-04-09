package service

import "github.com/speakpro/go-services/internal/model"

// ASRClient 语音识别（ASR）接口
type ASRClient interface {
	// Recognize 将音频数据转写为文本
	Recognize(audioData []byte) (string, error)
}

// ISEClient 语音评测（ISE）接口
type ISEClient interface {
	// Assess 对音频进行发音评测，返回多维度评分
	Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error)
}

// LLMClient 大语言模型接口 — 提供口语评测所需的全部 AI 能力
type LLMClient interface {
	// Chat AI 考官对话
	Chat(history []model.ConversationMessage, examType string, section string) (string, error)

	// ScoreContent 内容评分
	ScoreContent(transcript, question, examType, section string) (*model.ContentScore, error)

	// CorrectGrammar 语法纠错
	CorrectGrammar(transcript string) (*model.GrammarScore, error)

	// GenerateFeedback 生成综合口语反馈报告
	GenerateFeedback(transcript string, referenceText string, pronScore *model.PronunciationScore) (string, error)

	// GenerateRevisedAnswer 生成修订后的答案
	GenerateRevisedAnswer(transcript, question string) (*model.RevisedAnswer, error)

	// GenerateMindMap 生成思维导图结构
	GenerateMindMap(question, examType, section string) (*model.MindMapData, error)

	// GenerateKeywordsAndSamples 生成关键词和样例答案
	GenerateKeywordsAndSamples(question, transcript, examType string) ([]model.KeywordItem, []string, error)
}
