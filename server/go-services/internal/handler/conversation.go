package handler

import (
	"log"
	"net/http"

	"github.com/gin-gonic/gin"
	"github.com/gorilla/websocket"
	"github.com/speakpro/go-services/internal/service"
	"github.com/speakpro/go-services/pkg/ws"
)

var upgrader = websocket.Upgrader{
	CheckOrigin: func(r *http.Request) bool { return true },
}

type ConversationHandler struct {
	hub          *ws.Hub
	orchestrator *service.Orchestrator
}

func NewConversationHandler(hub *ws.Hub, orch *service.Orchestrator) *ConversationHandler {
	return &ConversationHandler{hub: hub, orchestrator: orch}
}

// Connect 建立 WebSocket 连接进行 AI 对话
func (h *ConversationHandler) Connect(c *gin.Context) {
	conn, err := upgrader.Upgrade(c.Writer, c.Request, nil)
	if err != nil {
		log.Printf("WebSocket 升级失败: %v", err)
		return
	}

	userID, _ := c.Get("userId")

	client := ws.NewClient(h.hub, conn, userID.(string))
	h.hub.Register <- client

	// 启动读写协程
	go client.WritePump()
	go client.ReadPump(func(msgType int, data []byte) {
		// TODO: 解析客户端消息类型
		// - audio_chunk: 流式音频 → 讯飞 ASR 实时转写
		// - text: 文本消息 → 通义千问对话
		// 处理后通过 client.Send 返回结果
		_ = msgType
		_ = data
	})
}
