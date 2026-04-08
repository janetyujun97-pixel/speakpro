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

	// 默认 TTS 提供商：mimo / fish / xunfei
	DefaultTTSProvider string

	// NestJS 内部回调地址
	NestAPIBaseURL string

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
		DefaultTTSProvider: getEnv("DEFAULT_TTS_PROVIDER", "mimo"),
		NestAPIBaseURL:     getEnv("NEST_API_BASE_URL", "http://localhost:3000/api/v1"),
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
