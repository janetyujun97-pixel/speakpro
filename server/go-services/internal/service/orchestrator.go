package service

import "github.com/speakpro/go-services/internal/model"

// Orchestrator AI 编排服务 - 协调多个 AI 服务完成完整评测流水线
//
// 流水线：学生录音 → 讯飞 ASR(转写) → 讯飞评测(发音评分)
//                                      → 通义千问(语法纠错+评分生成)
//                                      → 讯飞 TTS(示范发音)
//                                      → 返回综合评估结果
type Orchestrator struct {
	xunfei *XunfeiClient
	qwen   *QwenClient
}

func NewOrchestrator(xunfei *XunfeiClient, qwen *QwenClient) *Orchestrator {
	return &Orchestrator{xunfei: xunfei, qwen: qwen}
}

// EvaluateAudio 执行完整的音频评测流水线
func (o *Orchestrator) EvaluateAudio(audioData []byte, referenceText string) (*model.AssessmentResult, error) {
	// 步骤 1: ASR 语音转写
	transcript, err := o.xunfei.Recognize(audioData)
	if err != nil {
		return nil, err
	}

	// 步骤 2: 发音评测
	pronScore, err := o.xunfei.Assess(audioData, referenceText)
	if err != nil {
		return nil, err
	}

	// 步骤 3: AI 语法纠错 + 评分
	grammarResult, err := o.qwen.CorrectGrammar(transcript)
	if err != nil {
		return nil, err
	}

	// 步骤 4: AI 内容评分 + 综合反馈
	feedback, err := o.qwen.GenerateFeedback(transcript, referenceText, pronScore)
	if err != nil {
		return nil, err
	}

	// 组装综合结果
	result := &model.AssessmentResult{
		Pronunciation: *pronScore,
		Grammar:       *grammarResult,
		Overall:       calculateOverall(pronScore, grammarResult),
		AIFeedback:    feedback,
		Transcript:    transcript,
	}

	return result, nil
}

// GenerateExaminerResponse 生成 AI 考官回复（用于对话模式）
func (o *Orchestrator) GenerateExaminerResponse(history []model.ConversationMessage, examType string, section string) (string, error) {
	return o.qwen.Chat(history, examType, section)
}

func calculateOverall(pron *model.PronunciationScore, gram *model.GrammarScore) float64 {
	// 加权平均：发音 40%, 语法 20%, 流利度 20%, 内容 20%
	return pron.Overall*0.4 + gram.Score*0.2 + pron.Fluency*0.2 + pron.Overall*0.2
}
