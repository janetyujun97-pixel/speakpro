package service

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"

	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// MiMoLLMClient 小米 MiMo LLM 客户端（OpenAI 兼容接口）
type MiMoLLMClient struct {
	endpoint   string
	apiKey     string
	modelName  string
	httpClient *http.Client
}

func NewMiMoLLMClient(cfg *config.Config) *MiMoLLMClient {
	apiKey := cfg.MiMoLLMAPIKey
	if apiKey == "" {
		apiKey = cfg.MiMoAPIKey // 回退到通用 MiMo API Key
	}

	endpoint := cfg.MiMoLLMEndpoint
	if endpoint == "" {
		endpoint = "https://api.xiaomimimo.com/v1/chat/completions"
	}

	return &MiMoLLMClient{
		endpoint:  endpoint,
		apiKey:    apiKey,
		modelName: cfg.MiMoLLMModel,
		httpClient: &http.Client{
			Timeout: 90 * time.Second,
		},
	}
}

// mimoLLMMessage 避免与 mimo_tts.go 中的 mimoMessage 冲突
type mimoLLMMessage struct {
	Role    string `json:"role"`    // system / user / assistant
	Content string `json:"content"`
}

// mimoLLMChatRequest OpenAI 兼容接口请求体
type mimoLLMChatRequest struct {
	Model    string           `json:"model"`
	Messages []mimoLLMMessage `json:"messages"`
	Stream   bool             `json:"stream"`
}

// mimoLLMChatResponse OpenAI 兼容接口响应体
type mimoLLMChatResponse struct {
	Choices []struct {
		Message struct {
			Content string `json:"content"`
		} `json:"message"`
	} `json:"choices"`
	Error *struct {
		Message string `json:"message"`
		Code    string `json:"code"`
	} `json:"error"`
}

// callLLM 发起 MiMo LLM API 请求
func (c *MiMoLLMClient) callLLM(messages []mimoLLMMessage) (string, error) {
	reqBody := mimoLLMChatRequest{
		Model:    c.modelName,
		Messages: messages,
		Stream:   false,
	}

	body, err := json.Marshal(reqBody)
	if err != nil {
		return "", fmt.Errorf("序列化请求失败: %w", err)
	}

	req, err := http.NewRequest(
		http.MethodPost,
		c.endpoint,
		bytes.NewReader(body),
	)
	if err != nil {
		return "", fmt.Errorf("创建请求失败: %w", err)
	}

	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+c.apiKey)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("请求 MiMo LLM 失败: %w", err)
	}
	defer resp.Body.Close()

	respData, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("读取响应失败: %w", err)
	}

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("MiMo LLM HTTP %d: %s", resp.StatusCode, string(respData))
	}

	var result mimoLLMChatResponse
	if err := json.Unmarshal(respData, &result); err != nil {
		return "", fmt.Errorf("解析响应失败: %w", err)
	}

	if result.Error != nil {
		return "", fmt.Errorf("MiMo LLM API 错误 [%s]: %s", result.Error.Code, result.Error.Message)
	}

	if len(result.Choices) == 0 {
		return "", errors.New("MiMo LLM 返回空结果")
	}

	return strings.TrimSpace(result.Choices[0].Message.Content), nil
}

// Chat AI 考官对话 — 根据考试类型和历史对话生成考官回复
func (c *MiMoLLMClient) Chat(history []model.ConversationMessage, examType string, section string) (string, error) {
	if c.apiKey == "" {
		return "", errors.New("MiMo LLM API Key 未配置")
	}

	systemPrompt := buildExaminerPrompt(examType, section)

	messages := []mimoLLMMessage{
		{Role: "system", Content: systemPrompt},
	}

	// 注入历史对话（最近 10 轮）
	start := 0
	if len(history) > 10 {
		start = len(history) - 10
	}
	for _, msg := range history[start:] {
		data, ok := msg.Data.(string)
		if !ok {
			if b, err := json.Marshal(msg.Data); err == nil {
				data = string(b)
			}
		}
		role := "user"
		if msg.Type == "examiner" {
			role = "assistant"
		}
		messages = append(messages, mimoLLMMessage{Role: role, Content: data})
	}

	return c.callLLM(messages)
}

// ScoreContent 内容评分 — 分析回答的相关性、词汇、连贯性
func (c *MiMoLLMClient) ScoreContent(transcript, question, examType, section string) (*model.ContentScore, error) {
	if c.apiKey == "" {
		return nil, errors.New("MiMo LLM API Key 未配置")
	}

	prompt := fmt.Sprintf(ContentScoringPrompt, question, transcript, examType, section)

	messages := []mimoLLMMessage{
		{Role: "system", Content: "You are an expert IELTS/TOEFL speaking evaluator. Always respond with valid JSON only."},
		{Role: "user", Content: prompt},
	}

	respStr, err := c.callLLM(messages)
	if err != nil {
		return nil, err
	}

	respStr = cleanJSONResponse(respStr)

	var result model.ContentScore
	if err := json.Unmarshal([]byte(respStr), &result); err != nil {
		return &model.ContentScore{Score: 60, Relevance: 60, Vocabulary: 60, Coherence: 60}, nil
	}

	return &result, nil
}

// CorrectGrammar 语法纠错 — 对 ASR 转写文本进行详细语法分析
func (c *MiMoLLMClient) CorrectGrammar(transcript string) (*model.GrammarScore, error) {
	if c.apiKey == "" {
		return nil, errors.New("MiMo LLM API Key 未配置")
	}

	prompt := fmt.Sprintf(`Analyze the following English speech transcript for grammar errors.
Return a JSON object with this exact structure:
{
  "score": <float 0-100, overall grammar score on a 100-point scale>,
  "errors": [
    {
      "text": "<the incorrect phrase>",
      "type": "<error type: tense/agreement/article/preposition/other>",
      "suggestion": "<corrected version>"
    }
  ],
  "corrections": ["<full corrected sentence 1>", ...]
}

IMPORTANT: The "score" field must be on a 0-100 scale (not 0-10).
For example: perfect grammar = 95-100, minor errors = 70-85, major errors = 40-60, barely intelligible = 10-30.
Only return valid JSON, no markdown or extra text.

Transcript: %s`, transcript)

	messages := []mimoLLMMessage{
		{
			Role:    "system",
			Content: "You are an expert English grammar checker. Always respond with valid JSON only.",
		},
		{Role: "user", Content: prompt},
	}

	respStr, err := c.callLLM(messages)
	if err != nil {
		return nil, err
	}

	respStr = cleanJSONResponse(respStr)

	var result model.GrammarScore
	if err := json.Unmarshal([]byte(respStr), &result); err != nil {
		return &model.GrammarScore{
			Score:       5.0,
			Errors:      []model.GramError{},
			Corrections: []string{transcript},
		}, nil
	}

	return &result, nil
}

// GenerateFeedback 生成综合口语反馈报告
func (c *MiMoLLMClient) GenerateFeedback(transcript string, referenceText string, pronScore *model.PronunciationScore) (string, error) {
	if c.apiKey == "" {
		return "", errors.New("MiMo LLM API Key 未配置")
	}

	pronSummary := "未获取发音评分"
	if pronScore != nil {
		pronSummary = fmt.Sprintf(
			"整体发音评分: %.1f/100，流利度: %.1f/100，完整度: %.1f/100",
			pronScore.Overall, pronScore.Fluency, pronScore.Integrity,
		)
	}

	prompt := fmt.Sprintf(`You are an experienced IELTS/TOEFL speaking examiner.
Provide comprehensive feedback in Chinese for the following student response.

Reference text/question: %s
Student's response: %s
Pronunciation analysis: %s

Please provide feedback in the following format:
**优点 (Strengths):**
- [List 2-3 strengths]

**需要改进 (Areas for Improvement):**
- [List 2-3 specific issues]

**改进建议 (Suggestions):**
- [List 2-3 actionable tips]

**示例表达 (Better Expression):**
[Provide a model answer or improved version]

Keep feedback concise, specific, and encouraging.`, referenceText, transcript, pronSummary)

	messages := []mimoLLMMessage{
		{
			Role:    "system",
			Content: "You are a professional IELTS/TOEFL speaking coach. Provide feedback in Chinese.",
		},
		{Role: "user", Content: prompt},
	}

	return c.callLLM(messages)
}

// GenerateRevisedAnswer 生成修订后的答案
func (c *MiMoLLMClient) GenerateRevisedAnswer(transcript, question string) (*model.RevisedAnswer, error) {
	if c.apiKey == "" {
		return nil, errors.New("MiMo LLM API Key 未配置")
	}

	prompt := fmt.Sprintf(`You are an expert IELTS/TOEFL speaking coach. The student answered the following question:

Question: %s
Student's answer: %s

Provide an improved/corrected version that fixes all grammar errors, improves vocabulary, and enhances coherence while keeping the student's original ideas and structure.

Return ONLY valid JSON:
{"text": "<improved answer text>", "wordCount": <number>, "sentenceCount": <number>}`, question, transcript)

	messages := []mimoLLMMessage{
		{Role: "system", Content: "You are an expert English speaking coach. Always respond with valid JSON only."},
		{Role: "user", Content: prompt},
	}

	respStr, err := c.callLLM(messages)
	if err != nil {
		return nil, err
	}

	respStr = cleanJSONResponse(respStr)
	var result model.RevisedAnswer
	if err := json.Unmarshal([]byte(respStr), &result); err != nil {
		return &model.RevisedAnswer{Text: respStr, WordCount: len(strings.Fields(respStr)), SentenceCount: strings.Count(respStr, ".") + 1}, nil
	}
	return &result, nil
}

// GenerateMindMap 生成思维导图结构
func (c *MiMoLLMClient) GenerateMindMap(question, examType, section string) (*model.MindMapData, error) {
	if c.apiKey == "" {
		return nil, errors.New("MiMo LLM API Key 未配置")
	}

	prompt := fmt.Sprintf(`Generate a structured outline/mind map for answering this %s %s speaking question:

"%s"

The mind map should show the ideal answer structure with main points, supporting details, and examples. Limit to 3 levels deep.

Return ONLY valid JSON:
{
  "title": "<main topic/thesis>",
  "children": [
    {
      "label": "<main point>",
      "detail": "<brief explanation>",
      "children": [
        {"label": "<supporting detail>", "detail": "<example or evidence>"}
      ]
    }
  ]
}`, examType, section, question)

	messages := []mimoLLMMessage{
		{Role: "system", Content: "You are an expert speaking test coach. Always respond with valid JSON only."},
		{Role: "user", Content: prompt},
	}

	respStr, err := c.callLLM(messages)
	if err != nil {
		return nil, err
	}

	respStr = cleanJSONResponse(respStr)
	var result model.MindMapData
	if err := json.Unmarshal([]byte(respStr), &result); err != nil {
		return &model.MindMapData{Title: "Answer Structure", Children: []model.MindMapNode{{Label: "Main Point"}}}, nil
	}
	return &result, nil
}

// GenerateKeywordsAndSamples 生成关键词和样例答案
func (c *MiMoLLMClient) GenerateKeywordsAndSamples(question, transcript, examType string) ([]model.KeywordItem, []string, error) {
	if c.apiKey == "" {
		return nil, nil, errors.New("MiMo LLM API Key 未配置")
	}

	prompt := fmt.Sprintf(`Analyze this %s speaking question and the student's response. Provide useful vocabulary and sample answers.

Question: %s
Student's response: %s

Return ONLY valid JSON:
{
  "keywords": [
    {
      "word": "<advanced vocabulary word>",
      "phonetic": "/<IPA pronunciation>/",
      "partOfSpeech": "<noun/verb/adj/adv>",
      "definition": "<clear definition in English>",
      "exampleSentence": "<example sentence using this word in context of the question>"
    }
  ],
  "sampleAnswers": [
    "<complete high-scoring sample answer paragraph 1>",
    "<complete high-scoring sample answer paragraph 2>"
  ]
}

Provide 5-8 keywords and 2-3 sample answer paragraphs.`, examType, question, transcript)

	messages := []mimoLLMMessage{
		{Role: "system", Content: "You are an expert IELTS/TOEFL vocabulary and speaking coach. Always respond with valid JSON only."},
		{Role: "user", Content: prompt},
	}

	respStr, err := c.callLLM(messages)
	if err != nil {
		return nil, nil, err
	}

	respStr = cleanJSONResponse(respStr)
	var result struct {
		Keywords      []model.KeywordItem `json:"keywords"`
		SampleAnswers []string            `json:"sampleAnswers"`
	}
	if err := json.Unmarshal([]byte(respStr), &result); err != nil {
		return []model.KeywordItem{}, []string{}, nil
	}
	return result.Keywords, result.SampleAnswers, nil
}
