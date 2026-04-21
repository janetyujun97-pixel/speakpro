package config

import "os"

type Config struct {
	Port      string
	JWTSecret string

	// 数据库
	DBHost     string
	DBPort     string
	DBUser     string
	DBPassword string
	DBName     string

	// Redis
	RedisHost string
	RedisPort string

	// 讯飞
	XunfeiAppID     string
	XunfeiAPIKey    string
	XunfeiAPISecret string

	// 通义千问
	QwenAPIKey string
	QwenModel  string

	// Fish Audio TTS
	FishAudioAPIKey  string
	FishAudioModel   string
	FishAudioVoiceID string

	// MiMo-V2-TTS（小米）
	MiMoAPIKey string
	MiMoModel  string
	MiMoVoice  string

	// MiMo LLM（小米大语言模型）
	MiMoLLMAPIKey  string
	MiMoLLMModel   string
	MiMoLLMEndpoint string

	// 腾讯云 ASR + SOE
	TencentSecretID  string
	TencentSecretKey string
	TencentASRAppID  string
	TencentRegion    string

	// 默认提供商选择
	DefaultTTSProvider string // mimo / fish / xunfei
	DefaultASRProvider string // tencent / xunfei
	DefaultISEProvider string // tencent / xunfei
	DefaultLLMProvider string // mimo / qwen

	// NestJS 内部回调地址
	NestAPIBaseURL string

	// 服务间共享密钥（调用 NestJS /_internal/* 端点时放 X-Internal-Secret）
	InternalSharedSecret string

	// OSS
	OSSRegion          string
	OSSBucket          string
	OSSAccessKeyID     string
	OSSAccessKeySecret string
	OSSEndpoint        string
}

func Load() *Config {
	return &Config{
		Port:               getEnv("GO_PORT", "8080"),
		JWTSecret:          getEnv("JWT_SECRET", ""),
		DBHost:             getEnv("DB_HOST", "localhost"),
		DBPort:             getEnv("DB_PORT", "5432"),
		DBUser:             getEnv("DB_USER", "speakpro"),
		DBPassword:         getEnv("DB_PASSWORD", ""),
		DBName:             getEnv("DB_NAME", "speakpro"),
		RedisHost:          getEnv("REDIS_HOST", "localhost"),
		RedisPort:          getEnv("REDIS_PORT", "6379"),
		XunfeiAppID:        getEnv("XUNFEI_APP_ID", ""),
		XunfeiAPIKey:       getEnv("XUNFEI_API_KEY", ""),
		XunfeiAPISecret:    getEnv("XUNFEI_API_SECRET", ""),
		QwenAPIKey:         getEnv("QWEN_API_KEY", ""),
		QwenModel:          getEnv("QWEN_MODEL", "qwen-max"),
		FishAudioAPIKey:    getEnv("FISH_AUDIO_API_KEY", ""),
		FishAudioModel:     getEnv("FISH_AUDIO_MODEL", "s2-pro"),
		FishAudioVoiceID:   getEnv("FISH_AUDIO_VOICE_ID", ""),
		MiMoAPIKey:         getEnv("MIMO_API_KEY", ""),
		MiMoModel:          getEnv("MIMO_MODEL", "mimo-v2-tts"),
		MiMoVoice:          getEnv("MIMO_VOICE", "default_en"),
		MiMoLLMAPIKey:      getEnv("MIMO_LLM_API_KEY", ""),
		MiMoLLMModel:       getEnv("MIMO_LLM_MODEL", "mimo-llm"),
		MiMoLLMEndpoint:    getEnv("MIMO_LLM_ENDPOINT", ""),
		TencentSecretID:    getEnv("TENCENT_SECRET_ID", ""),
		TencentSecretKey:   getEnv("TENCENT_SECRET_KEY", ""),
		TencentASRAppID:    getEnv("TENCENT_ASR_APP_ID", ""),
		TencentRegion:      getEnv("TENCENT_REGION", "ap-guangzhou"),
		DefaultTTSProvider: getEnv("DEFAULT_TTS_PROVIDER", "mimo"),
		DefaultASRProvider: getEnv("DEFAULT_ASR_PROVIDER", "tencent"),
		DefaultISEProvider: getEnv("DEFAULT_ISE_PROVIDER", "tencent"),
		DefaultLLMProvider: getEnv("DEFAULT_LLM_PROVIDER", "mimo"),
		NestAPIBaseURL:       getEnv("NEST_API_BASE_URL", "http://localhost:3000/api/v1"),
		InternalSharedSecret: getEnv("INTERNAL_SHARED_SECRET", ""),
		OSSRegion:          getEnv("OSS_REGION", "oss-cn-hangzhou"),
		OSSBucket:          getEnv("OSS_BUCKET", "speakpro"),
		OSSAccessKeyID:     getEnv("OSS_ACCESS_KEY_ID", ""),
		OSSAccessKeySecret: getEnv("OSS_ACCESS_KEY_SECRET", ""),
		OSSEndpoint:        getEnv("OSS_ENDPOINT", ""),
	}
}

func getEnv(key, fallback string) string {
	if v := os.Getenv(key); v != "" {
		return v
	}
	return fallback
}
