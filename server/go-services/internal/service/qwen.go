package service

import (
	"errors"

	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// QwenClient 通义千问 API 客户端
// 文档: https://help.aliyun.com/zh/model-studio/
type QwenClient struct {
	apiKey string
	model  string
}

func NewQwenClient(cfg *config.Config) *QwenClient {
	return &QwenClient{
		apiKey: cfg.QwenAPIKey,
		model:  cfg.QwenModel,
	}
}

// Chat AI 考官对话 - 根据考试类型和历史对话生成回复
func (c *QwenClient) Chat(history []model.ConversationMessage, examType string, section string) (string, error) {
	if c.apiKey == "" {
		return "", errors.New("通义千问 API Key 未配置")
	}

	// TODO: 调用通义千问 Chat API
	// 1. 构建系统 Prompt（角色设定为 IELTS/TOEFL 考官）
	// 2. 注入历史对话
	// 3. 流式输出考官回复
	// API: POST https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation

	return "", errors.New("通义千问对话尚未实现")
}

// CorrectGrammar 语法纠错 - 对 ASR 转写文本进行纠错
func (c *QwenClient) CorrectGrammar(transcript string) (*model.GrammarScore, error) {
	if c.apiKey == "" {
		return nil, errors.New("通义千问 API Key 未配置")
	}

	// TODO: 调用通义千问进行语法纠错
	// Prompt: 分析以下英文口语文本的语法错误，返回 JSON 格式:
	// { score, errors: [{text, type, suggestion}], corrections: [...] }

	return nil, errors.New("通义千问语法纠错尚未实现")
}

// GenerateFeedback 生成综合反馈
func (c *QwenClient) GenerateFeedback(transcript string, referenceText string, pronScore *model.PronunciationScore) (string, error) {
	if c.apiKey == "" {
		return "", errors.New("通义千问 API Key 未配置")
	}

	// TODO: 调用通义千问生成综合反馈
	// 输入: 学生文本 + 参考答案 + 发音评分数据
	// 输出: 优点分析 + 改进建议 + 推荐练习

	return "", errors.New("通义千问反馈生成尚未实现")
}
