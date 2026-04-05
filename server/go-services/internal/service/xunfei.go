package service

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"log"
	"net/http"
	"net/url"
	"strings"
	"time"

	"github.com/gorilla/websocket"
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

// buildAuthURL 构建讯飞 WebSocket 鉴权 URL
// 签名算法: HMAC-SHA256，参考: https://www.xfyun.cn/doc/asr/voicedictation/API.html
func (c *XunfeiClient) buildAuthURL(rawURL string) (string, error) {
	u, err := url.Parse(rawURL)
	if err != nil {
		return "", fmt.Errorf("解析 URL 失败: %w", err)
	}

	// RFC1123 时间格式
	date := time.Now().UTC().Format(http.TimeFormat)

	// 构建签名原串: host + date + request-line
	host := u.Host
	path := u.RequestURI()
	signStr := fmt.Sprintf("host: %s\ndate: %s\nGET %s HTTP/1.1", host, date, path)

	// HMAC-SHA256 签名
	mac := hmac.New(sha256.New, []byte(c.apiSecret))
	mac.Write([]byte(signStr))
	sig := base64.StdEncoding.EncodeToString(mac.Sum(nil))

	// 构建 Authorization 字段
	auth := fmt.Sprintf(
		`api_key="%s", algorithm="hmac-sha256", headers="host date request-line", signature="%s"`,
		c.apiKey, sig,
	)
	authEncoded := base64.StdEncoding.EncodeToString([]byte(auth))

	// 拼接最终 URL 参数
	params := url.Values{}
	params.Set("authorization", authEncoded)
	params.Set("date", date)
	params.Set("host", host)

	return fmt.Sprintf("%s?%s", rawURL, params.Encode()), nil
}

// Recognize 实时语音识别 (ASR) — 讯飞 IAT WebSocket API
// 将 PCM/RAW 格式音频转写为英文文本
func (c *XunfeiClient) Recognize(audioData []byte) (string, error) {
	if c.appID == "" {
		return "", errors.New("讯飞 AppID 未配置")
	}

	authURL, err := c.buildAuthURL("wss://iat-api.xfyun.cn/v2/iat")
	if err != nil {
		return "", err
	}

	conn, _, err := websocket.DefaultDialer.Dial(authURL, nil)
	if err != nil {
		return "", fmt.Errorf("讯飞 ASR WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 自动去除 WAV 头
	audioData = stripWAVHeader(audioData)

	// 首帧：携带业务配置 + 首段音频
	chunkSize := 1280
	firstChunk := audioData
	if len(firstChunk) > chunkSize {
		firstChunk = audioData[:chunkSize]
	}

	firstFrame := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": c.appID,
		},
		"business": map[string]interface{}{
			"language": "en_us",
			"domain":   "iat",
			"accent":   "mandarin",
			"vad_eos":  10000, // 端点检测超时 ms
			"dwa":      "wpgs", // 动态修正
		},
		"data": map[string]interface{}{
			"status":   0, // 0=首帧
			"format":   "audio/L16;rate=16000",
			"encoding": "raw",
			"audio":    base64.StdEncoding.EncodeToString(firstChunk),
		},
	}
	if err := conn.WriteJSON(firstFrame); err != nil {
		return "", fmt.Errorf("发送 ASR 首帧失败: %w", err)
	}

	// 发送剩余音频帧（每帧 1280 字节 = 40ms @ 16kHz）
	for i := chunkSize; i < len(audioData); i += chunkSize {
		end := min(i+chunkSize, len(audioData))
		status := 1 // 中间帧
		if end >= len(audioData) {
			status = 2 // 末帧
		}
		frame := map[string]interface{}{
			"data": map[string]interface{}{
				"status":   status,
				"format":   "audio/L16;rate=16000",
				"encoding": "raw",
				"audio":    base64.StdEncoding.EncodeToString(audioData[i:end]),
			},
		}
		if err := conn.WriteJSON(frame); err != nil {
			return "", fmt.Errorf("发送 ASR 音频帧失败: %w", err)
		}
	}

	// 接收识别结果 — 处理 wpgs 动态修正模式
	// wpgs 模式下，每次返回的 result 有 pgs 字段：
	//   pgs="apd" → 追加（append），将本次 ws 追加到已有结果
	//   pgs="rpl" → 替换（replace），用本次 ws 替换 rg 指定范围的旧结果
	// 我们用 segments 数组按句子编号(sn)存储，最后拼接
	segments := make(map[int]string)
	var maxSN int

	for {
		var result struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Data    struct {
				Status int `json:"status"`
				Result struct {
					Pgs string `json:"pgs"` // apd=追加, rpl=替换
					Rg  []int  `json:"rg"`  // 替换范围 [start, end]
					Sn  int    `json:"sn"`  // 句子编号
					Ws  []struct {
						Cw []struct {
							W string `json:"w"`
						} `json:"cw"`
					} `json:"ws"`
				} `json:"result"`
			} `json:"data"`
		}
		if err := conn.ReadJSON(&result); err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				break
			}
			return "", fmt.Errorf("读取 ASR 结果失败: %w", err)
		}
		if result.Code != 0 {
			return "", fmt.Errorf("讯飞 ASR 错误 %d: %s (请确认已开通实时语音转写能力)", result.Code, result.Message)
		}

		// 拼接当前帧的文本
		var segText strings.Builder
		for _, ws := range result.Data.Result.Ws {
			for _, cw := range ws.Cw {
				segText.WriteString(cw.W)
			}
		}

		sn := result.Data.Result.Sn
		pgs := result.Data.Result.Pgs

		if pgs == "rpl" && len(result.Data.Result.Rg) == 2 {
			// 替换模式：删除 rg 范围内的旧 segment，写入新的
			for i := result.Data.Result.Rg[0]; i <= result.Data.Result.Rg[1]; i++ {
				delete(segments, i)
			}
		}

		segments[sn] = segText.String()
		if sn > maxSN {
			maxSN = sn
		}

		if result.Data.Status == 2 {
			break
		}
	}

	// 按 sn 顺序拼接最终结果
	var finalText strings.Builder
	for i := 0; i <= maxSN; i++ {
		if text, ok := segments[i]; ok {
			finalText.WriteString(text)
		}
	}

	return strings.TrimSpace(finalText.String()), nil
}

// Assess 发音评测 — 讯飞 ISE (Intelligent Speech Evaluation) WebSocket API
// 返回发音、流利度、完整度等多维度评分
func (c *XunfeiClient) Assess(audioData []byte, referenceText string) (*model.PronunciationScore, error) {
	if c.appID == "" {
		return nil, errors.New("讯飞 AppID 未配置")
	}

	// 自动检测并去除 WAV 文件头（讯飞 ISE 需要原始 PCM 数据）
	audioData = stripWAVHeader(audioData)

	authURL, err := c.buildAuthURL("wss://ise-api.xfyun.cn/v2/open-ise")
	if err != nil {
		return nil, err
	}

	conn, _, err := websocket.DefaultDialer.Dial(authURL, nil)
	if err != nil {
		return nil, fmt.Errorf("讯飞 ISE WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 参考文本需要 UTF-8 BOM
	refTextWithBOM := "\uFEFF" + referenceText

	// === 阶段 1: SSB（配置参数 + 参考文本，不含音频）===
	ssbFrame := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": c.appID,
		},
		"business": map[string]interface{}{
			"sub":      "ise",
			"ent":      "en_vip",
			"category": "read_sentence",
			"text":     refTextWithBOM,
			"tte":      "utf-8",
			"cmd":      "ssb",
			"auf":      "audio/L16;rate=16000",
			"aue":      "raw",
			"rstcd":    "utf8",
		},
		"data": map[string]interface{}{
			"status": 0,
		},
	}
	if err := conn.WriteJSON(ssbFrame); err != nil {
		return nil, fmt.Errorf("发送 ISE ssb 帧失败: %w", err)
	}

	// === 阶段 2: AUW（分帧上传音频数据）===
	chunkSize := 1280
	totalChunks := (len(audioData) + chunkSize - 1) / chunkSize
	for i := 0; i < len(audioData); i += chunkSize {
		end := min(i+chunkSize, len(audioData))
		chunkIdx := i / chunkSize

		// aus: 1=首段, 2=中间, 4=末段
		aus := 2
		if chunkIdx == 0 {
			aus = 1
		}
		if chunkIdx == totalChunks-1 {
			aus = 4
		}

		// data.status: 1=中间帧, 2=末帧
		status := 1
		if chunkIdx == totalChunks-1 {
			status = 2
		}

		frame := map[string]interface{}{
			"business": map[string]interface{}{
				"cmd": "auw",
				"aus": aus,
			},
			"data": map[string]interface{}{
				"status": status,
				"data":   base64.StdEncoding.EncodeToString(audioData[i:end]),
			},
		}
		if err := conn.WriteJSON(frame); err != nil {
			return nil, fmt.Errorf("发送 ISE auw 帧失败: %w", err)
		}
	}

	// === 阶段 3: TTP（请求返回评测结果）===
	ttpFrame := map[string]interface{}{
		"business": map[string]interface{}{
			"cmd": "ttp",
		},
	}
	if err := conn.WriteJSON(ttpFrame); err != nil {
		return nil, fmt.Errorf("发送 ISE ttp 帧失败: %w", err)
	}

	// 接收评测结果（ISE 返回 base64 编码的 XML）
	var lastData string
	for {
		var frame struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Data    struct {
				Status int    `json:"status"`
				Data   string `json:"data"` // base64 XML
			} `json:"data"`
		}
		if err := conn.ReadJSON(&frame); err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				break
			}
			return nil, fmt.Errorf("读取 ISE 结果失败: %w", err)
		}
		if frame.Code != 0 {
			// 11200=服务未开通, 请前往 https://console.xfyun.cn 开通"语音评测"
			return nil, fmt.Errorf("讯飞 ISE 错误 %d: %s (请确认已开通语音评测能力)", frame.Code, frame.Message)
		}
		if frame.Data.Data != "" {
			lastData = frame.Data.Data
		}
		if frame.Data.Status == 2 {
			break
		}
	}

	return parseISEScore(lastData), nil
}

// parseISEScore 解析讯飞 ISE 返回的 base64 XML 评分结果
// ISE 返回的 XML 包含 totalscore、fluency_score、integrity_score 等属性
func parseISEScore(encoded string) *model.PronunciationScore {
	if encoded == "" {
		return &model.PronunciationScore{Overall: 0}
	}

	xmlData, err := base64.StdEncoding.DecodeString(encoded)
	if err != nil {
		return &model.PronunciationScore{Overall: 0}
	}

	xmlStr := string(xmlData)
	// read_chapter 标签中的属性（带下划线）
	overall := extractXMLAttr(xmlStr, "total_score")
	fluency := extractXMLAttr(xmlStr, "fluency_score")
	integrity := extractXMLAttr(xmlStr, "integrity_score")
	accuracy := extractXMLAttr(xmlStr, "accuracy_score")

	// 各维度推算（ISE XML 中还有更细粒度的 phone 评分）
	return &model.PronunciationScore{
		Overall:    overall,
		Fluency:    fluency,
		Integrity:  integrity,
		Stress:     accuracy,              // 准确度映射到重音
		Intonation: overall * 0.95,        // 语调评分估算
		Phonemes:   []model.PhonemeScore{}, // TODO: 解析音素级评分
	}
}

// extractXMLAttr 从 XML 字符串中提取指定属性的浮点值
// 例: totalscore="85.5" → 85.5
func extractXMLAttr(xmlStr, attr string) float64 {
	key := attr + `="`
	idx := strings.Index(xmlStr, key)
	if idx < 0 {
		return 0
	}
	rest := xmlStr[idx+len(key):]
	end := strings.Index(rest, `"`)
	if end < 0 {
		return 0
	}
	var val float64
	fmt.Sscanf(rest[:end], "%f", &val)
	return val
}

// Synthesize TTS 语音合成 — 讯飞 TTS WebSocket API
// text: 待合成文本（英文）; voice: 音色; speed: 语速(0-100)
// 返回 PCM 原始音频数据
func (c *XunfeiClient) Synthesize(text string, voice string, speed int) ([]byte, error) {
	if c.appID == "" {
		return nil, errors.New("讯飞 AppID 未配置")
	}

	if voice == "" {
		voice = "x4_enus_luna_assist" // 默认英文音色（Luna，美式英语，自然流畅）
	}
	if speed <= 0 || speed > 100 {
		speed = 50 // 默认中等语速
	}

	// 尝试主音色，失败则回退到备选音色
	voices := []string{voice, "x4_enus_luna_assist", "catherine", "x4_lingxiaolu_oral"}
	var lastErr error
	for _, v := range voices {
		data, err := c.doSynthesize(text, v, speed)
		if err == nil {
			return data, nil
		}
		lastErr = err
		log.Printf("[TTS] 音色 %s 合成失败: %v，尝试下一个...", v, err)
	}
	return nil, fmt.Errorf("所有 TTS 音色均失败: %w", lastErr)
}

// doSynthesize 执行单次 TTS 合成
func (c *XunfeiClient) doSynthesize(text string, voice string, speed int) ([]byte, error) {
	authURL, err := c.buildAuthURL("wss://tts-api.xfyun.cn/v2/tts")
	if err != nil {
		return nil, err
	}

	conn, _, err := websocket.DefaultDialer.Dial(authURL, nil)
	if err != nil {
		return nil, fmt.Errorf("讯飞 TTS WebSocket 连接失败: %w", err)
	}
	defer conn.Close()

	// 使用 SSML 包装英文文本，增强自然度
	// ssml 可以控制停顿、语调、语速，让英文读起来更自然
	ssmlText := fmt.Sprintf(`<speak><prosody rate="%d%%">%s</prosody></speak>`, speed, text)
	isSSML := true

	// 发送合成请求（TTS 只需发一帧请求）
	business := map[string]interface{}{
		"aue":    "raw",                  // 输出格式: PCM
		"auf":    "audio/L16;rate=16000", // 采样率
		"vcn":    voice,                  // 发音人
		"speed":  speed,                  // 语速 (0-100)
		"volume": 60,                     // 音量稍大
		"pitch":  50,                     // 音调
		"tte":    "utf8",                 // 文本编码
		"sfl":    1,                      // 流式返回
	}

	// 如果不支持 SSML（某些音色），回退到纯文本
	var encodedText string
	if isSSML {
		business["tte"] = "utf8"
		encodedText = base64.StdEncoding.EncodeToString([]byte(ssmlText))
	} else {
		encodedText = base64.StdEncoding.EncodeToString([]byte(text))
	}

	reqFrame := map[string]interface{}{
		"common": map[string]interface{}{
			"app_id": c.appID,
		},
		"business": business,
		"data": map[string]interface{}{
			"status": 2, // TTS 只有一帧请求，status=2 表示结束
			"text":   encodedText,
		},
	}
	if err := conn.WriteJSON(reqFrame); err != nil {
		return nil, fmt.Errorf("发送 TTS 请求失败: %w", err)
	}

	// 接收音频帧并拼接
	var audioBuf []byte
	for {
		var frame struct {
			Code    int    `json:"code"`
			Message string `json:"message"`
			Data    struct {
				Status int    `json:"status"`
				Audio  string `json:"audio"` // base64 PCM
				Ced    string `json:"ced"`   // 合成进度
			} `json:"data"`
		}
		if err := conn.ReadJSON(&frame); err != nil {
			if websocket.IsCloseError(err, websocket.CloseNormalClosure) {
				break
			}
			return nil, fmt.Errorf("读取 TTS 音频帧失败: %w", err)
		}
		if frame.Code != 0 {
			// 错误码说明: 11200=授权失败(服务未开通), 10313=AppID与Key不匹配
			// 10014=参数错误, 11201=日期格式错误
			// 请前往 https://console.xfyun.cn 确认已开通 TTS 能力
			return nil, fmt.Errorf("讯飞 TTS 错误 %d: %s (请确认讯飞控制台已开通语音合成能力)", frame.Code, frame.Message)
		}
		if frame.Data.Audio != "" {
			chunk, err := base64.StdEncoding.DecodeString(frame.Data.Audio)
			if err == nil {
				audioBuf = append(audioBuf, chunk...)
			}
		}
		if frame.Data.Status == 2 {
			break
		}
	}

	return audioBuf, nil
}

// stripWAVHeader 检测 WAV 文件头（"RIFF"...44 字节）并去除，返回纯 PCM 数据
// 如果不是 WAV 格式则原样返回
func stripWAVHeader(data []byte) []byte {
	if len(data) > 44 &&
		string(data[0:4]) == "RIFF" &&
		string(data[8:12]) == "WAVE" {
		// 找到 "data" 子块的位置
		for i := 12; i < len(data)-8; i++ {
			if string(data[i:i+4]) == "data" {
				dataSize := int(data[i+4]) | int(data[i+5])<<8 | int(data[i+6])<<16 | int(data[i+7])<<24
				pcmStart := i + 8
				if pcmStart < len(data) {
					end := pcmStart + dataSize
					if end > len(data) {
						end = len(data)
					}
					log.Printf("[WAV] 去除 WAV 头: header=%d bytes, pcm=%d bytes", pcmStart, end-pcmStart)
					return data[pcmStart:end]
				}
			}
		}
		// 简单回退：跳过标准 44 字节头
		log.Printf("[WAV] 使用标准 44 字节头偏移")
		return data[44:]
	}
	return data
}
