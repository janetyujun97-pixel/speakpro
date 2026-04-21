package main

import (
	"log"
	"os"

	"github.com/gin-contrib/cors"
	"github.com/gin-gonic/gin"
	"github.com/joho/godotenv"
	"github.com/speakpro/go-services/internal/config"
	"github.com/speakpro/go-services/internal/handler"
	"github.com/speakpro/go-services/internal/middleware"
	"github.com/speakpro/go-services/internal/service"
	"github.com/speakpro/go-services/pkg/ws"
)

func main() {
	// 加载环境变量
	_ = godotenv.Load("../.env")

	cfg := config.Load()

	// === 初始化所有 AI 服务 ===

	// 旧服务（保留为回退）
	xunfeiClient := service.NewXunfeiClient(cfg)
	qwenClient := service.NewQwenClient(cfg)

	// 新服务
	tencentASR := service.NewTencentASRClient(cfg)
	tencentSOE := service.NewTencentSOEClient(cfg)
	mimoLLM := service.NewMiMoLLMClient(cfg)

	// TTS 服务
	fishTTSClient := service.NewFishTTSClient(cfg)
	mimoTTSClient := service.NewMiMoTTSClient(cfg)

	// === 构建带回退的 AI 客户端 ===

	var asrClient service.ASRClient
	var iseClient service.ISEClient
	var llmClient service.LLMClient

	// ASR: 腾讯云优先，讯飞回退
	switch cfg.DefaultASRProvider {
	case "tencent":
		asrClient = service.NewFallbackASR(tencentASR, xunfeiClient)
	default:
		asrClient = service.NewFallbackASR(xunfeiClient, tencentASR)
	}

	// ISE: 腾讯云 SOE 优先，讯飞回退
	switch cfg.DefaultISEProvider {
	case "tencent":
		iseClient = service.NewFallbackISE(tencentSOE, xunfeiClient)
	default:
		iseClient = service.NewFallbackISE(xunfeiClient, tencentSOE)
	}

	// LLM: MiMo 优先，千问回退
	switch cfg.DefaultLLMProvider {
	case "mimo":
		llmClient = service.NewFallbackLLM(mimoLLM, qwenClient)
	default:
		llmClient = service.NewFallbackLLM(qwenClient, mimoLLM)
	}

	log.Printf("AI 服务配置: ASR=%s, ISE=%s, LLM=%s, TTS=%s",
		cfg.DefaultASRProvider, cfg.DefaultISEProvider, cfg.DefaultLLMProvider, cfg.DefaultTTSProvider)

	// === Orchestrator（使用接口 + 原始客户端注册表以支持按次覆盖） ===
	registry := &service.ProviderRegistry{
		TencentASR: tencentASR,
		XunfeiASR:  xunfeiClient,
		TencentISE: tencentSOE,
		XunfeiISE:  xunfeiClient,
		MimoLLM:    mimoLLM,
		QwenLLM:    qwenClient,
	}
	orchestrator := service.NewOrchestrator(asrClient, iseClient, llmClient, fishTTSClient).WithRegistry(registry)

	// PR3a —— NestJS 错题本内部回调（凭证未配时 RecordMiss 会变 no-op）
	orchestrator.SetNotebookClient(service.NewNotebookClient(
		cfg.NestAPIBaseURL,
		cfg.InternalSharedSecret,
	))
	if cfg.InternalSharedSecret == "" {
		log.Println("[NotebookClient] INTERNAL_SHARED_SECRET 未配置，错题本回调已禁用")
	}

	// WebSocket 管理器
	hub := ws.NewHub()
	go hub.Run()

	// Gin 路由
	r := gin.Default()
	r.Use(cors.Default())

	api := r.Group("/api/v1")

	// 健康检查
	api.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{
			"status":  "ok",
			"service": "go-services",
			"ai": gin.H{
				"asr": cfg.DefaultASRProvider,
				"ise": cfg.DefaultISEProvider,
				"llm": cfg.DefaultLLMProvider,
				"tts": cfg.DefaultTTSProvider,
			},
		})
	})

	// 需要认证的路由
	authorized := api.Group("")
	authorized.Use(middleware.JWTAuth(cfg.JWTSecret))
	{
		// 音频上传
		audioHandler := handler.NewAudioHandler(orchestrator)
		authorized.POST("/practice/audio", audioHandler.Upload)

		// WebSocket AI 对话
		convHandler := handler.NewConversationHandler(hub, orchestrator, asrClient, llmClient, fishTTSClient, mimoTTSClient, xunfeiClient)
		authorized.GET("/conversation/ws/:sessionId", convHandler.Connect)

		// 发音评测
		assessHandler := handler.NewAssessmentHandler(orchestrator, iseClient, llmClient)
		authorized.POST("/assessment/evaluate", assessHandler.Evaluate)
		authorized.POST("/assessment/full-evaluate", assessHandler.FullEvaluate)
		authorized.POST("/assessment/feedback", assessHandler.Feedback)

		// TTS 语音合成
		ttsHandler := handler.NewTTSHandler(xunfeiClient, fishTTSClient, mimoTTSClient)
		authorized.POST("/tts/synthesize", ttsHandler.Synthesize)
	}

	port := cfg.Port
	if port == "" {
		port = "8080"
	}
	log.Printf("Go 服务已启动: http://localhost:%s/api/v1", port)
	if err := r.Run(":" + port); err != nil {
		log.Fatal(err)
		os.Exit(1)
	}
}
