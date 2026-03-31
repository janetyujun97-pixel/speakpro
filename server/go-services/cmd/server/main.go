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

	// 初始化 AI 服务
	xunfeiClient := service.NewXunfeiClient(cfg)
	qwenClient := service.NewQwenClient(cfg)
	orchestrator := service.NewOrchestrator(xunfeiClient, qwenClient)

	// WebSocket 管理器
	hub := ws.NewHub()
	go hub.Run()

	// Gin 路由
	r := gin.Default()
	r.Use(cors.Default())

	api := r.Group("/api/v1")

	// 健康检查（无需认证）
	api.GET("/health", func(c *gin.Context) {
		c.JSON(200, gin.H{"status": "ok", "service": "go-services"})
	})

	// 需要认证的路由
	authorized := api.Group("")
	authorized.Use(middleware.JWTAuth(cfg.JWTSecret))
	{
		// 音频上传
		audioHandler := handler.NewAudioHandler(orchestrator)
		authorized.POST("/practice/audio", audioHandler.Upload)

		// WebSocket AI 对话
		convHandler := handler.NewConversationHandler(hub, orchestrator)
		authorized.GET("/conversation/connect", convHandler.Connect)

		// 发音评测
		assessHandler := handler.NewAssessmentHandler(orchestrator)
		authorized.POST("/assessment/evaluate", assessHandler.Evaluate)
		authorized.POST("/assessment/feedback", assessHandler.Feedback)

		// TTS 语音合成
		ttsHandler := handler.NewTTSHandler(xunfeiClient)
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
