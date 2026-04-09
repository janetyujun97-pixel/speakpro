package service

import (
	"log"

	"github.com/speakpro/go-services/internal/model"
)

// ========================
// FallbackASR — 主备 ASR 客户端
// ========================

// FallbackASR 尝试主 ASR，失败则回退到备用 ASR
type FallbackASR struct {
	primary   ASRClient
	secondary ASRClient
}

func NewFallbackASR(primary, secondary ASRClient) *FallbackASR {
	return &FallbackASR{primary: primary, secondary: secondary}
}

func (f *FallbackASR) Recognize(audioData []byte) (string, error) {
	result, err := f.primary.Recognize(audioData)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackASR] 主 ASR 失败: %v，切换备用", err)
	return f.secondary.Recognize(audioData)
}

// ========================
// FallbackISE — 主备 ISE 客户端
// ========================

// FallbackISE 尝试主 ISE，失败则回退到备用 ISE
type FallbackISE struct {
	primary   ISEClient
	secondary ISEClient
}

func NewFallbackISE(primary, secondary ISEClient) *FallbackISE {
	return &FallbackISE{primary: primary, secondary: secondary}
}

func (f *FallbackISE) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	result, err := f.primary.Assess(audioData, referenceText)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackISE] 主 ISE 失败: %v，切换备用", err)
	return f.secondary.Assess(audioData, referenceText)
}

// ========================
// FallbackLLM — 主备 LLM 客户端
// ========================

// FallbackLLM 尝试主 LLM，失败则回退到备用 LLM
type FallbackLLM struct {
	primary   LLMClient
	secondary LLMClient
}

func NewFallbackLLM(primary, secondary LLMClient) *FallbackLLM {
	return &FallbackLLM{primary: primary, secondary: secondary}
}

func (f *FallbackLLM) Chat(history []model.ConversationMessage, examType string, section string) (string, error) {
	result, err := f.primary.Chat(history, examType, section)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackLLM] 主 LLM Chat 失败: %v，切换备用", err)
	return f.secondary.Chat(history, examType, section)
}

func (f *FallbackLLM) ScoreContent(transcript, question, examType, section string) (*model.ContentScore, error) {
	result, err := f.primary.ScoreContent(transcript, question, examType, section)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackLLM] 主 LLM ScoreContent 失败: %v，切换备用", err)
	return f.secondary.ScoreContent(transcript, question, examType, section)
}

func (f *FallbackLLM) CorrectGrammar(transcript string) (*model.GrammarScore, error) {
	result, err := f.primary.CorrectGrammar(transcript)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackLLM] 主 LLM CorrectGrammar 失败: %v，切换备用", err)
	return f.secondary.CorrectGrammar(transcript)
}

func (f *FallbackLLM) GenerateFeedback(transcript string, referenceText string, pronScore *model.PronunciationScore) (string, error) {
	result, err := f.primary.GenerateFeedback(transcript, referenceText, pronScore)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackLLM] 主 LLM GenerateFeedback 失败: %v，切换备用", err)
	return f.secondary.GenerateFeedback(transcript, referenceText, pronScore)
}

func (f *FallbackLLM) GenerateRevisedAnswer(transcript, question string) (*model.RevisedAnswer, error) {
	result, err := f.primary.GenerateRevisedAnswer(transcript, question)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackLLM] 主 LLM GenerateRevisedAnswer 失败: %v，切换备用", err)
	return f.secondary.GenerateRevisedAnswer(transcript, question)
}

func (f *FallbackLLM) GenerateMindMap(question, examType, section string) (*model.MindMapData, error) {
	result, err := f.primary.GenerateMindMap(question, examType, section)
	if err == nil {
		return result, nil
	}
	log.Printf("[FallbackLLM] 主 LLM GenerateMindMap 失败: %v，切换备用", err)
	return f.secondary.GenerateMindMap(question, examType, section)
}

func (f *FallbackLLM) GenerateKeywordsAndSamples(question, transcript, examType string) ([]model.KeywordItem, []string, error) {
	kw, sa, err := f.primary.GenerateKeywordsAndSamples(question, transcript, examType)
	if err == nil {
		return kw, sa, nil
	}
	log.Printf("[FallbackLLM] 主 LLM GenerateKeywordsAndSamples 失败: %v，切换备用", err)
	return f.secondary.GenerateKeywordsAndSamples(question, transcript, examType)
}
