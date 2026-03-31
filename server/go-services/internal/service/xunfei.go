package service

import (
	"errors"

	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// XunfeiClient 讯飞开放平台 API 客户端
// 文档: https://www.xfyun.cn/doc/
type XunfeiClient struct {
	appID     string
	apiKey    string
	apiSecret string
}

func NewXunfeiClient(cfg *config.Config) *XunfeiClient {
	return &XunfeiClient{
		appID:     cfg.XunfeiAppID,
		apiKey:    cfg.XunfeiAPIKey,
		apiSecret: cfg.XunfeiAPISecret,
	}
}

// Recognize 实时语音识别 (ASR)
// 将音频转为文本，支持流式识别
func (c *XunfeiClient) Recognize(audioData []byte) (string, error) {
	if c.appID == "" {
		return "", errors.New("讯飞 AppID 未配置")
	}

	// TODO: 实现讯飞实时语音识别 WebSocket API
	// 1. 构建鉴权 URL (HMAC-SHA256)
	// 2. 建立 WebSocket 连接
	// 3. 发送音频数据帧
	// 4. 接收并拼接识别结果
	// 参考: https://www.xfyun.cn/doc/asr/voicedictation/API.html

	return "", errors.New("讯飞 ASR 尚未实现")
}

// Assess 发音评测
// 返回音素级评分结果
func (c *XunfeiClient) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	if c.appID == "" {
		return nil, errors.New("讯飞 AppID 未配置")
	}

	// TODO: 实现讯飞语音评测 API
	// 1. 构建 HTTP 请求 (音频 + 参考文本)
	// 2. 设置评测参数 (language=en, category=read_sentence)
	// 3. 解析返回结果中的音素评分
	// 参考: https://www.xfyun.cn/doc/Ise/IseAPI.html

	return nil, errors.New("讯飞语音评测尚未实现")
}

// Synthesize TTS 语音合成
// 将文本转为音频（英文朗读示范）
func (c *XunfeiClient) Synthesize(text string, voice string, speed int) ([]byte, error) {
	if c.appID == "" {
		return nil, errors.New("讯飞 AppID 未配置")
	}

	// TODO: 实现讯飞语音合成 API
	// 1. 构建 WebSocket 鉴权 URL
	// 2. 发送合成请求 (文本 + 音色 + 语速)
	// 3. 接收音频数据帧并拼接
	// 参考: https://www.xfyun.cn/doc/tts/online_tts/API.html

	return nil, errors.New("讯飞 TTS 尚未实现")
}
