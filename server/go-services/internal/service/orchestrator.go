package service

import (
	"encoding/base64"
	"log"
	"math"
	"strings"
	"sync"

	"github.com/speakpro/go-services/internal/model"
)

// Orchestrator AI 编排服务 - 协调多个 AI 服务完成完整评测流水线
//
// 流水线：ASR 转写 → (并行) ISE 发音 · 语法纠错 · 综合反馈 → 返回综合评估结果
type Orchestrator struct {
	asr      ASRClient
	ise      ISEClient
	llm      LLMClient
	fishTTS  *FishTTSClient
	notebook *NotebookClient
	registry *ProviderRegistry
}

// ProviderRegistry 保留所有原始 AI 客户端，供按次覆盖时重新组装 fallback 链
type ProviderRegistry struct {
	TencentASR ASRClient
	XunfeiASR  ASRClient
	TencentISE ISEClient
	XunfeiISE  ISEClient
	MimoLLM    LLMClient
	QwenLLM    LLMClient
}

func NewOrchestrator(asr ASRClient, ise ISEClient, llm LLMClient, fishTTS ...*FishTTSClient) *Orchestrator {
	o := &Orchestrator{asr: asr, ise: ise, llm: llm}
	if len(fishTTS) > 0 {
		o.fishTTS = fishTTS[0]
	}
	return o
}

// WithRegistry 注入原始客户端集合，启用按次覆盖能力
func (o *Orchestrator) WithRegistry(reg *ProviderRegistry) *Orchestrator {
	o.registry = reg
	return o
}

// SetNotebookClient 注入 NestJS 错题本回调客户端；nil 或未配置 secret 时 ReportMisses 为 no-op
func (o *Orchestrator) SetNotebookClient(c *NotebookClient) { o.notebook = c }

// ReportMisses 异步上报低分词到 NestJS 错题本。
func (o *Orchestrator) ReportMisses(userID, sessionID string, items []MissItem) {
	if o.notebook == nil {
		return
	}
	go o.notebook.RecordMiss(userID, sessionID, items)
}

// ── 按次覆盖：空字符串 → 沿用默认；匹配到预设 → 重组 fallback 链 ─────

// PickASR 根据覆盖值返回带 fallback 的 ASR 客户端；空覆盖返回默认
func (o *Orchestrator) PickASR(override string) ASRClient {
	if o.registry == nil || override == "" {
		return o.asr
	}
	switch override {
	case "tencent":
		if o.registry.TencentASR != nil && o.registry.XunfeiASR != nil {
			return NewFallbackASR(o.registry.TencentASR, o.registry.XunfeiASR)
		}
	case "xunfei":
		if o.registry.XunfeiASR != nil && o.registry.TencentASR != nil {
			return NewFallbackASR(o.registry.XunfeiASR, o.registry.TencentASR)
		}
	}
	return o.asr
}

func (o *Orchestrator) PickISE(override string) ISEClient {
	if o.registry == nil || override == "" {
		return o.ise
	}
	switch override {
	case "tencent":
		if o.registry.TencentISE != nil && o.registry.XunfeiISE != nil {
			return NewFallbackISE(o.registry.TencentISE, o.registry.XunfeiISE)
		}
	case "xunfei":
		if o.registry.XunfeiISE != nil && o.registry.TencentISE != nil {
			return NewFallbackISE(o.registry.XunfeiISE, o.registry.TencentISE)
		}
	}
	return o.ise
}

func (o *Orchestrator) PickLLM(override string) LLMClient {
	if o.registry == nil || override == "" {
		return o.llm
	}
	switch override {
	case "mimo":
		if o.registry.MimoLLM != nil && o.registry.QwenLLM != nil {
			return NewFallbackLLM(o.registry.MimoLLM, o.registry.QwenLLM)
		}
	case "qwen":
		if o.registry.QwenLLM != nil && o.registry.MimoLLM != nil {
			return NewFallbackLLM(o.registry.QwenLLM, o.registry.MimoLLM)
		}
	}
	return o.llm
}

// ── 主流水线 ────────────────────────────────────────────────────────

// EvaluateAudio 执行完整的音频评测流水线（默认 provider）
func (o *Orchestrator) EvaluateAudio(audioData []byte, referenceText string) (*model.AssessmentResult, error) {
	return o.evaluateAudio(o.asr, o.ise, o.llm, audioData, referenceText)
}

// EvaluateAudioWithOverrides 按次覆盖 ASR/ISE/LLM；空字符串表示沿用默认
func (o *Orchestrator) EvaluateAudioWithOverrides(audioData []byte, referenceText, asrP, iseP, llmP string) (*model.AssessmentResult, error) {
	return o.evaluateAudio(o.PickASR(asrP), o.PickISE(iseP), o.PickLLM(llmP), audioData, referenceText)
}

func (o *Orchestrator) evaluateAudio(asr ASRClient, ise ISEClient, llm LLMClient, audioData []byte, referenceText string) (*model.AssessmentResult, error) {
	transcript, err := asr.Recognize(audioData)
	if err != nil {
		return nil, err
	}
	log.Printf("[Orchestrator] ASR 转写完成: %q", transcript)
	return o.evaluateWithTranscript(ise, llm, audioData, referenceText, transcript)
}

// EvaluateWithTranscript 外部已做过 ASR 的评测（对话模式用，避免重复 ASR）。
// 并行执行：ISE 发音评测、语法纠错、综合反馈。
func (o *Orchestrator) EvaluateWithTranscript(audioData []byte, referenceText, transcript string) (*model.AssessmentResult, error) {
	return o.evaluateWithTranscript(o.ise, o.llm, audioData, referenceText, transcript)
}

// EvaluateWithTranscriptAndOverrides 对话模式也支持按次覆盖
func (o *Orchestrator) EvaluateWithTranscriptAndOverrides(audioData []byte, referenceText, transcript, iseP, llmP string) (*model.AssessmentResult, error) {
	return o.evaluateWithTranscript(o.PickISE(iseP), o.PickLLM(llmP), audioData, referenceText, transcript)
}

func (o *Orchestrator) evaluateWithTranscript(ise ISEClient, llm LLMClient, audioData []byte, referenceText, transcript string) (*model.AssessmentResult, error) {
	var (
		wg        sync.WaitGroup
		pronScore *model.PronunciationScore
		grammar   *model.GrammarScore
		feedback  string
	)

	wg.Add(3)

	// A: ISE 发音评测
	go func() {
		defer wg.Done()
		ps, err := ise.Assess(audioData, referenceText)
		if err != nil {
			log.Printf("[Orchestrator] ISE 评测失败，使用降级评分: %v", err)
			ps = &model.PronunciationScore{Phonemes: []model.PhonemeScore{}}
		}
		pronScore = ps
	}()

	// B: 语法纠错
	go func() {
		defer wg.Done()
		if transcript == "" {
			grammar = &model.GrammarScore{Score: 0, Errors: []model.GramError{}, Corrections: []string{}}
			return
		}
		gs, err := llm.CorrectGrammar(transcript)
		if err != nil {
			log.Printf("[Orchestrator] 语法纠错失败: %v", err)
			gs = &model.GrammarScore{Score: 0, Errors: []model.GramError{}, Corrections: []string{}}
		}
		grammar = gs
	}()

	// C: 综合反馈
	go func() {
		defer wg.Done()
		if transcript == "" {
			feedback = ""
			return
		}
		fb, err := llm.GenerateFeedback(transcript, referenceText, nil)
		if err != nil {
			log.Printf("[Orchestrator] 反馈生成失败: %v", err)
			fb = "AI 反馈暂不可用"
		}
		feedback = fb
	}()

	wg.Wait()

	return &model.AssessmentResult{
		Pronunciation: *pronScore,
		Grammar:       *grammar,
		Overall:       calculateOverall(pronScore, grammar),
		AIFeedback:    feedback,
		Transcript:    transcript,
	}, nil
}

// GenerateExaminerResponse 生成 AI 考官回复（用于对话模式）
func (o *Orchestrator) GenerateExaminerResponse(history []model.ConversationMessage, examType string, section string) (string, error) {
	return o.llm.Chat(history, examType, section)
}

// ── 完整评测（模考） ────────────────────────────────────────────────

// FullEvaluateAudio 完整评测流水线（默认 provider）
func (o *Orchestrator) FullEvaluateAudio(audioData []byte, referenceText, examType, section string) (*model.FullEvaluateResult, error) {
	return o.fullEvaluateAudio(o.asr, o.ise, o.llm, audioData, referenceText, examType, section)
}

// FullEvaluateAudioWithOverrides 允许按次覆盖
func (o *Orchestrator) FullEvaluateAudioWithOverrides(audioData []byte, referenceText, examType, section, asrP, iseP, llmP string) (*model.FullEvaluateResult, error) {
	return o.fullEvaluateAudio(o.PickASR(asrP), o.PickISE(iseP), o.PickLLM(llmP), audioData, referenceText, examType, section)
}

func (o *Orchestrator) fullEvaluateAudio(asr ASRClient, ise ISEClient, llm LLMClient, audioData []byte, referenceText, examType, section string) (*model.FullEvaluateResult, error) {
	result := &model.FullEvaluateResult{}

	transcript, err := asr.Recognize(audioData)
	if err != nil {
		log.Printf("[FullEvaluate] ASR 失败: %v", err)
		transcript = ""
	}
	result.Transcript = transcript
	result.WordCount = len(strings.Fields(transcript))
	result.SentenceCount = countSentences(transcript)
	log.Printf("[FullEvaluate] ASR 完成: %d 词, %d 句", result.WordCount, result.SentenceCount)

	var wg sync.WaitGroup
	var mu sync.Mutex

	var pronScore *model.PronunciationScore
	var gramScore *model.GrammarScore
	var aiFeedback string
	var revisedAnswer *model.RevisedAnswer
	var mindMap *model.MindMapData
	var keywords []model.KeywordItem
	var sampleAnswers []string

	// A: ISE 发音评测
	wg.Add(1)
	go func() {
		defer wg.Done()
		ps, err := ise.Assess(audioData, referenceText)
		if err != nil {
			log.Printf("[FullEvaluate] ISE 失败: %v", err)
			ps = &model.PronunciationScore{}
		}
		mu.Lock()
		pronScore = ps
		mu.Unlock()
	}()

	// B: 千问串行 — 语法 → 反馈 → 修订答案
	wg.Add(1)
	go func() {
		defer wg.Done()
		if transcript == "" {
			return
		}

		gs, err := llm.CorrectGrammar(transcript)
		if err != nil {
			log.Printf("[FullEvaluate] 语法分析失败: %v", err)
			gs = &model.GrammarScore{Score: 0}
		}
		mu.Lock()
		gramScore = gs
		mu.Unlock()

		fb, err := llm.GenerateFeedback(transcript, referenceText, nil)
		if err != nil {
			log.Printf("[FullEvaluate] 反馈生成失败: %v", err)
			fb = ""
		}
		mu.Lock()
		aiFeedback = fb
		mu.Unlock()

		ra, err := llm.GenerateRevisedAnswer(transcript, referenceText)
		if err != nil {
			log.Printf("[FullEvaluate] 修订答案失败: %v", err)
			return
		}
		mu.Lock()
		revisedAnswer = ra
		mu.Unlock()
	}()

	// C: 千问串行 — 思维导图 → 关键词+样例
	wg.Add(1)
	go func() {
		defer wg.Done()

		mm, err := llm.GenerateMindMap(referenceText, examType, section)
		if err != nil {
			log.Printf("[FullEvaluate] 思维导图失败: %v", err)
		} else {
			mu.Lock()
			mindMap = mm
			mu.Unlock()
		}

		if transcript != "" {
			kw, sa, err := llm.GenerateKeywordsAndSamples(referenceText, transcript, examType)
			if err != nil {
				log.Printf("[FullEvaluate] 关键词生成失败: %v", err)
			} else {
				mu.Lock()
				keywords = kw
				sampleAnswers = sa
				mu.Unlock()
			}
		}
	}()

	wg.Wait()

	result.PronunciationScore = pronScore
	result.GrammarScore = gramScore
	result.AIFeedback = aiFeedback
	result.RevisedAnswer = revisedAnswer
	result.MindMap = mindMap
	result.Keywords = keywords
	result.SampleAnswers = sampleAnswers

	if pronScore != nil && gramScore != nil {
		result.OverallScore = calculateOverall(pronScore, gramScore)
	}

	// Step 3: TTS 合成修订答案语音
	if revisedAnswer != nil && revisedAnswer.Text != "" && o.fishTTS != nil && o.fishTTS.IsConfigured() {
		ttsData, err := o.fishTTS.Synthesize(revisedAnswer.Text, 1.0)
		if err != nil {
			log.Printf("[FullEvaluate] 修订答案 TTS 失败: %v", err)
		} else {
			result.RevisedAudioB64 = base64.StdEncoding.EncodeToString(ttsData)
		}
	}

	log.Printf("[FullEvaluate] 完成: overall=%.1f, transcript_len=%d", result.OverallScore, len(transcript))
	return result, nil
}

func countSentences(text string) int {
	count := 0
	for _, c := range text {
		if c == '.' || c == '!' || c == '?' {
			count++
		}
	}
	if count == 0 && len(text) > 0 {
		count = 1
	}
	return count
}

func calculateOverall(pron *model.PronunciationScore, gram *model.GrammarScore) float64 {
	pronScore := pron.Overall
	fluencyScore := pron.Fluency
	gramScore := gram.Score

	if gramScore > 0 && gramScore <= 10 {
		gramScore = gramScore * 10
	}

	if pronScore == 0 && fluencyScore == 0 {
		return gramScore
	}

	overall := pronScore*0.35 + fluencyScore*0.25 + gramScore*0.25 + pronScore*0.15
	return math.Round(overall*10) / 10
}
