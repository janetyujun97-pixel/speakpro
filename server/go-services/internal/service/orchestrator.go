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
// 流水线：学生录音 → 讯飞 ASR(转写) → 讯飞评测(发音评分)
//                                      → 通义千问(语法纠错+评分生成)
//                                      → 讯飞 TTS(示范发音)
//                                      → 返回综合评估结果
type Orchestrator struct {
	asr      ASRClient
	ise      ISEClient
	llm      LLMClient
	fishTTS  *FishTTSClient
	notebook *NotebookClient
}

func NewOrchestrator(asr ASRClient, ise ISEClient, llm LLMClient, fishTTS ...*FishTTSClient) *Orchestrator {
	o := &Orchestrator{asr: asr, ise: ise, llm: llm}
	if len(fishTTS) > 0 {
		o.fishTTS = fishTTS[0]
	}
	return o
}

// SetNotebookClient 注入 NestJS 错题本回调客户端；nil 或未配置 secret 时 ReportMisses 为 no-op
func (o *Orchestrator) SetNotebookClient(c *NotebookClient) { o.notebook = c }

// ReportMisses 异步上报低分词到 NestJS 错题本。
// - userID / sessionID 由 handler 从 JWT + 请求体提取
// - items 通常来自 ISE XML 的 ExtractMissesFromISE
func (o *Orchestrator) ReportMisses(userID, sessionID string, items []MissItem) {
	if o.notebook == nil {
		return
	}
	go o.notebook.RecordMiss(userID, sessionID, items)
}

// EvaluateAudio 执行完整的音频评测流水线（ASR + 并行 ISE / 语法 / 反馈）
func (o *Orchestrator) EvaluateAudio(audioData []byte, referenceText string) (*model.AssessmentResult, error) {
	transcript, err := o.asr.Recognize(audioData)
	if err != nil {
		return nil, err
	}
	log.Printf("[Orchestrator] ASR 转写完成: %q", transcript)
	return o.EvaluateWithTranscript(audioData, referenceText, transcript)
}

// EvaluateWithTranscript 外部已做过 ASR 的评测（供对话模式下与考官回复并行调用，避免重复 ASR）
//
// 并行执行：ISE 发音评测、语法纠错、综合反馈 —— 合计耗时 ≈ max(各步耗时)，而非串行之和。
func (o *Orchestrator) EvaluateWithTranscript(audioData []byte, referenceText, transcript string) (*model.AssessmentResult, error) {
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
		ps, err := o.ise.Assess(audioData, referenceText)
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
		gs, err := o.llm.CorrectGrammar(transcript)
		if err != nil {
			log.Printf("[Orchestrator] 语法纠错失败: %v", err)
			gs = &model.GrammarScore{Score: 0, Errors: []model.GramError{}, Corrections: []string{}}
		}
		grammar = gs
	}()

	// C: 综合反馈（不依赖 pronScore —— 用 nil 代替以便并行；语气反馈足够）
	go func() {
		defer wg.Done()
		if transcript == "" {
			feedback = ""
			return
		}
		fb, err := o.llm.GenerateFeedback(transcript, referenceText, nil)
		if err != nil {
			log.Printf("[Orchestrator] 反馈生成失败: %v", err)
			fb = "AI 反馈暂不可用"
		}
		feedback = fb
	}()

	wg.Wait()

	result := &model.AssessmentResult{
		Pronunciation: *pronScore,
		Grammar:       *grammar,
		Overall:       calculateOverall(pronScore, grammar),
		AIFeedback:    feedback,
		Transcript:    transcript,
	}
	return result, nil
}

// GenerateExaminerResponse 生成 AI 考官回复（用于对话模式）
func (o *Orchestrator) GenerateExaminerResponse(history []model.ConversationMessage, examType string, section string) (string, error) {
	return o.llm.Chat(history, examType, section)
}

// FullEvaluateAudio 完整评测流水线（模考用）
// 并行执行 ASR + ISE + 语法 + 反馈 + 修订 + 思维导图 + 关键词
func (o *Orchestrator) FullEvaluateAudio(audioData []byte, referenceText, examType, section string) (*model.FullEvaluateResult, error) {
	result := &model.FullEvaluateResult{}

	// Step 1: ASR 转写（必须先完成，后续步骤依赖 transcript）
	transcript, err := o.asr.Recognize(audioData)
	if err != nil {
		log.Printf("[FullEvaluate] ASR 失败: %v", err)
		transcript = ""
	}
	result.Transcript = transcript
	result.WordCount = len(strings.Fields(transcript))
	result.SentenceCount = countSentences(transcript)

	log.Printf("[FullEvaluate] ASR 完成: %d 词, %d 句", result.WordCount, result.SentenceCount)

	// Step 2: 并行执行 3 组任务（减少千问并发数避免限流）
	var wg sync.WaitGroup
	var mu sync.Mutex

	var pronScore *model.PronunciationScore
	var gramScore *model.GrammarScore
	var aiFeedback string
	var revisedAnswer *model.RevisedAnswer
	var mindMap *model.MindMapData
	var keywords []model.KeywordItem
	var sampleAnswers []string

	// 组 A: ISE 发音评测（讯飞，独立通道）
	wg.Add(1)
	go func() {
		defer wg.Done()
		ps, err := o.ise.Assess(audioData, referenceText)
		if err != nil {
			log.Printf("[FullEvaluate] ISE 失败: %v", err)
			ps = &model.PronunciationScore{}
		}
		mu.Lock()
		pronScore = ps
		mu.Unlock()
	}()

	// 组 B: 千问串行 — 语法 → 反馈 → 修订答案（共用上下文，串行更稳定）
	wg.Add(1)
	go func() {
		defer wg.Done()
		if transcript == "" { return }

		// B1: 语法纠错
		gs, err := o.llm.CorrectGrammar(transcript)
		if err != nil {
			log.Printf("[FullEvaluate] 语法分析失败: %v", err)
			gs = &model.GrammarScore{Score: 0}
		}
		mu.Lock()
		gramScore = gs
		mu.Unlock()

		// B2: 综合反馈
		fb, err := o.llm.GenerateFeedback(transcript, referenceText, nil)
		if err != nil {
			log.Printf("[FullEvaluate] 反馈生成失败: %v", err)
			fb = ""
		}
		mu.Lock()
		aiFeedback = fb
		mu.Unlock()

		// B3: 修订答案
		ra, err := o.llm.GenerateRevisedAnswer(transcript, referenceText)
		if err != nil {
			log.Printf("[FullEvaluate] 修订答案失败: %v", err)
			return
		}
		mu.Lock()
		revisedAnswer = ra
		mu.Unlock()
	}()

	// 组 C: 千问串行 — 思维导图 → 关键词+样例
	wg.Add(1)
	go func() {
		defer wg.Done()

		// C1: 思维导图
		mm, err := o.llm.GenerateMindMap(referenceText, examType, section)
		if err != nil {
			log.Printf("[FullEvaluate] 思维导图失败: %v", err)
		} else {
			mu.Lock()
			mindMap = mm
			mu.Unlock()
		}

		// C2: 关键词 + 样例答案
		if transcript != "" {
			kw, sa, err := o.llm.GenerateKeywordsAndSamples(referenceText, transcript, examType)
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

	// 组装结果
	result.PronunciationScore = pronScore
	result.GrammarScore = gramScore
	result.AIFeedback = aiFeedback
	result.RevisedAnswer = revisedAnswer
	result.MindMap = mindMap
	result.Keywords = keywords
	result.SampleAnswers = sampleAnswers

	// 计算综合分
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
	// 所有分数统一为 0-100 百分制
	pronScore := pron.Overall      // ISE 原始 0-100
	fluencyScore := pron.Fluency   // ISE 原始 0-100
	gramScore := gram.Score        // 千问已改为 0-100

	// 兼容旧的 0-10 分制（如果 AI 仍然返回 0-10 范围的分数）
	if gramScore > 0 && gramScore <= 10 {
		gramScore = gramScore * 10
	}

	// 若 ISE 不可用（分数为 0），只用语法评分
	if pronScore == 0 && fluencyScore == 0 {
		return gramScore
	}

	// 加权平均：发音 35%, 流利度 25%, 语法 25%, 内容/综合 15%
	overall := pronScore*0.35 + fluencyScore*0.25 + gramScore*0.25 + pronScore*0.15
	return math.Round(overall*10) / 10 // 保留一位小数
}
