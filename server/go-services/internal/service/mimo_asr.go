package service

import (
	"bytes"
	"encoding/base64"
	"encoding/binary"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"time"

	"github.com/speakpro/go-services/internal/config"
)

// MiMoASRClient 小米 MiMo ASR 客户端（chat/completions 兼容接口，input_audio 输入）
type MiMoASRClient struct {
	endpoint   string
	apiKey     string
	modelName  string
	httpClient *http.Client
}

func NewMiMoASRClient(cfg *config.Config) *MiMoASRClient {
	apiKey := cfg.MiMoASRAPIKey
	if apiKey == "" {
		apiKey = cfg.MiMoAPIKey // 回退到通用 MiMo API Key
	}

	endpoint := cfg.MiMoASREndpoint
	if endpoint == "" {
		endpoint = "https://api.xiaomimimo.com/v1/chat/completions"
	}

	return &MiMoASRClient{
		endpoint:  endpoint,
		apiKey:    apiKey,
		modelName: cfg.MiMoASRModel,
		httpClient: &http.Client{
			Timeout: 60 * time.Second,
		},
	}
}

// IsConfigured 检查是否已配置
func (c *MiMoASRClient) IsConfigured() bool {
	return c.apiKey != ""
}

// mimoASRRequest MiMo ASR 请求体
type mimoASRRequest struct {
	Model    string           `json:"model"`
	Messages []mimoASRMessage `json:"messages"`
	Options  mimoASROptions   `json:"asr_options"`
}

type mimoASRMessage struct {
	Role    string        `json:"role"`
	Content []mimoASRPart `json:"content"`
}

type mimoASRPart struct {
	Type       string        `json:"type"` // input_audio
	InputAudio mimoASRSource `json:"input_audio"`
}

type mimoASRSource struct {
	Data string `json:"data"` // data:{MIME_TYPE};base64,{BASE64_AUDIO}
}

type mimoASROptions struct {
	Language string `json:"language"` // auto / en / zh ...
}

// mimoASRResponse MiMo ASR 响应体（OpenAI 兼容）
type mimoASRResponse struct {
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

// Recognize 语音识别 — 将 PCM/WAV 音频转写为文本
// 裸 PCM 会先补 WAV 头（16kHz 单声道 16bit），再以 data URI 形式提交
func (c *MiMoASRClient) Recognize(audioData []byte) (string, error) {
	if !c.IsConfigured() {
		return "", fmt.Errorf("MiMo ASR 未配置")
	}

	wavData := ensureWAVHeader(audioData)
	dataURI := "data:audio/wav;base64," + base64.StdEncoding.EncodeToString(wavData)

	reqBody := mimoASRRequest{
		Model: c.modelName,
		Messages: []mimoASRMessage{
			{
				Role: "user",
				Content: []mimoASRPart{
					{Type: "input_audio", InputAudio: mimoASRSource{Data: dataURI}},
				},
			},
		},
		Options: mimoASROptions{Language: "auto"},
	}

	body, err := json.Marshal(reqBody)
	if err != nil {
		return "", fmt.Errorf("请求序列化失败: %w", err)
	}

	log.Printf("[MiMoASR] 识别请求: audio_size=%d bytes", len(wavData))

	req, err := http.NewRequest(http.MethodPost, c.endpoint, bytes.NewReader(body))
	if err != nil {
		return "", fmt.Errorf("创建请求失败: %w", err)
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("api-key", c.apiKey)

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return "", fmt.Errorf("请求 MiMo ASR 失败: %w", err)
	}
	defer resp.Body.Close()

	respData, err := io.ReadAll(resp.Body)
	if err != nil {
		return "", fmt.Errorf("读取响应失败: %w", err)
	}

	if resp.StatusCode != http.StatusOK {
		return "", fmt.Errorf("MiMo ASR HTTP %d: %s", resp.StatusCode, string(respData))
	}

	var result mimoASRResponse
	if err := json.Unmarshal(respData, &result); err != nil {
		return "", fmt.Errorf("响应解析失败: %w", err)
	}

	if result.Error != nil {
		return "", fmt.Errorf("MiMo ASR API 错误 [%s]: %s", result.Error.Code, result.Error.Message)
	}

	if len(result.Choices) == 0 {
		return "", fmt.Errorf("MiMo ASR 返回空结果")
	}

	text := result.Choices[0].Message.Content
	log.Printf("[MiMoASR] 识别成功: text_len=%d", len(text))
	return text, nil
}

// ensureWAVHeader 若输入为裸 PCM 则补 16kHz 单声道 16bit WAV 头，已是 WAV 则原样返回
func ensureWAVHeader(data []byte) []byte {
	if len(data) > 12 && string(data[0:4]) == "RIFF" && string(data[8:12]) == "WAVE" {
		return data
	}

	const (
		sampleRate    = 16000
		channels      = 1
		bitsPerSample = 16
	)
	byteRate := sampleRate * channels * bitsPerSample / 8
	blockAlign := channels * bitsPerSample / 8

	buf := bytes.NewBuffer(make([]byte, 0, 44+len(data)))
	buf.WriteString("RIFF")
	binary.Write(buf, binary.LittleEndian, uint32(36+len(data)))
	buf.WriteString("WAVE")
	buf.WriteString("fmt ")
	binary.Write(buf, binary.LittleEndian, uint32(16))
	binary.Write(buf, binary.LittleEndian, uint16(1)) // PCM
	binary.Write(buf, binary.LittleEndian, uint16(channels))
	binary.Write(buf, binary.LittleEndian, uint32(sampleRate))
	binary.Write(buf, binary.LittleEndian, uint32(byteRate))
	binary.Write(buf, binary.LittleEndian, uint16(blockAlign))
	binary.Write(buf, binary.LittleEndian, uint16(bitsPerSample))
	buf.WriteString("data")
	binary.Write(buf, binary.LittleEndian, uint32(len(data)))
	buf.Write(data)
	return buf.Bytes()
}
