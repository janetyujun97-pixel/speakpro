package middleware

import (
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

// RateLimiter 基于 IP 的令牌桶限流中间件
func RateLimiter(maxRequests int, window time.Duration) gin.HandlerFunc {
	type client struct {
		count    int
		resetAt  time.Time
	}

	var (
		mu      sync.Mutex
		clients = make(map[string]*client)
	)

	// 定期清理过期记录
	go func() {
		ticker := time.NewTicker(window)
		defer ticker.Stop()
		for range ticker.C {
			mu.Lock()
			now := time.Now()
			for ip, c := range clients {
				if now.After(c.resetAt) {
					delete(clients, ip)
				}
			}
			mu.Unlock()
		}
	}()

	return func(c *gin.Context) {
		ip := c.ClientIP()
		mu.Lock()

		cl, exists := clients[ip]
		if !exists || time.Now().After(cl.resetAt) {
			clients[ip] = &client{
				count:   1,
				resetAt: time.Now().Add(window),
			}
			mu.Unlock()
			c.Next()
			return
		}

		if cl.count >= maxRequests {
			mu.Unlock()
			c.JSON(http.StatusTooManyRequests, gin.H{
				"code":    429,
				"message": "请求过于频繁，请稍后再试",
			})
			c.Abort()
			return
		}

		cl.count++
		mu.Unlock()
		c.Next()
	}
}
