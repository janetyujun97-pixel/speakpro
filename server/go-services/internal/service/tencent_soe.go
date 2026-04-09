package service

import (
	"crypto/rand"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"

	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/model"
)

// TencentSOEClient 腾讯云智聆口语评测客户端
// 使用 TransmitOralProcessWithInit API（初始化+传输一体化）
type TencentSOEClient struct {
	secretID  string
	secretKey string
	region    string
}

func NewTencentSOEClient(cfg *config.Config) *TencentSOEClient {
	return &TencentSOEClient{
		secretID:  cfg.TencentSecretID,
		secretKey: cfg.TencentSecretKey,
		region:    cfg.TencentRegion,
	}
}

// IsConfigured 检查是否已配置
func (c *TencentSOEClient) IsConfigured() bool {
	return c.secretID != "" && c.secretKey != ""
}

// Assess 发音评测 — 评估学生发音的准确度、流利度、完整度
// 输入: PCM/WAV 音频数据 + 参考文本
// 输出: 发音评分（0-100 各维度）
func (c *TencentSOEClient) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	if !c.IsConfigured() {
		return nil, fmt.Errorf("腾讯云 SOE 未配置")
	}

	// 去除 WAV 头
	audioData = stripWAVHeader(audioData)

	// 生成唯一 SessionId
	sessionID := generateUUID()

	// Base64 编码
	audioB64 := base64.StdEncoding.EncodeToString(audioData)

	log.Printf("[TencentSOE] 评测请求: audio_size=%d bytes, ref_text_len=%d, session=%s",
		len(audioData), len(referenceText), sessionID)

	// 构建请求体
	reqBody := map[string]interface{}{
		"SessionId":       sessionID,
		"RefText":         referenceText,
		"WorkMode":        1,   // 非流式（一次性发送）
		"EvalMode":        1,   // 句子评测
		"ScoreCoeff":      2.0, // 中等严格度（1.0=宽松/儿童, 4.0=严格）
		"ServerType":      0,   // 英文
		"IsEnd":           1,   // 单次请求
		"VoiceFileType":   3,   // base64 编码
		"VoiceEncodeType": 1,   // PCM
		"UserVoiceData":   audioB64,
		"IsQuery":         0,
	}

	bodyBytes, err := json.Marshal(reqBody)
	if err != nil {
		return nil, fmt.Errorf("请求序列化失败: %w", err)
	}

	// 调用腾讯云 SOE API
	respBytes, err := tencentCloudRequest(
		"soe", "TransmitOralProcessWithInit", "2018-07-24",
		c.secretID, c.secretKey, c.region, string(bodyBytes),
	)
	if err != nil {
		return nil, fmt.Errorf("腾讯云 SOE 请求失败: %w", err)
	}

	// 解析响应
	var resp struct {
		Response struct {
			PronAccuracy   float64 `json:"PronAccuracy"`
			PronFluency    float64 `json:"PronFluency"`
			PronCompletion float64 `json:"PronCompletion"`
			SuggestedScore float64 `json:"SuggestedScore"`
			RequestId      string  `json:"RequestId"`
			Error          *struct {
				Code    string `json:"Code"`
				Message string `json:"Message"`
			} `json:"Error"`
		} `json:"Response"`
	}

	if err := json.Unmarshal(respBytes, &resp); err != nil {
		return nil, fmt.Errorf("响应解析失败: %w", err)
	}

	if resp.Response.Error != nil {
		return nil, fmt.Errorf("腾讯云 SOE 错误 [%s]: %s",
			resp.Response.Error.Code, resp.Response.Error.Message)
	}

	// 映射到 PronunciationScore（腾讯 SOE 评分范围为 0-100）
	score := &model.PronunciationScore{
		Overall:    resp.Response.PronAccuracy,
		Fluency:    resp.Response.PronFluency,
		Integrity:  resp.Response.PronCompletion,
		Stress:     resp.Response.PronAccuracy,
		Intonation: resp.Response.PronAccuracy * 0.95, // 估算语调分
		Phonemes:   []model.PhonemeScore{},
	}

	// 如果建议分数可用，用它作为 Overall
	if resp.Response.SuggestedScore > 0 {
		score.Overall = resp.Response.SuggestedScore
	}

	log.Printf("[TencentSOE] 评测成功: overall=%.1f, fluency=%.1f, completion=%.1f, suggested=%.1f",
		resp.Response.PronAccuracy, resp.Response.PronFluency,
		resp.Response.PronCompletion, resp.Response.SuggestedScore)

	return score, nil
}

// generateUUID 生成 UUID v4
func generateUUID() string {
	b := make([]byte, 16)
	_, _ = rand.Read(b)
	b[6] = (b[6] & 0x0f) | 0x40 // Version 4
	b[8] = (b[8] & 0x3f) | 0x80 // Variant 10
	return fmt.Sprintf("%08x-%04x-%04x-%04x-%012x",
		b[0:4], b[4:6], b[6:8], b[8:10], b[10:16])
}
