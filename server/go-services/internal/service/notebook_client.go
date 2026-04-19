package service

import (
	"bytes"
	"encoding/json"
	"fmt"
	"log"
	"net/http"
	"time"
)

// MissItem 单个低分词上报数据
type MissItem struct {
	Word  string  `json:"word"`
	IPA   string  `json:"ipa,omitempty"`
	Score float64 `json:"score"`
	Note  string  `json:"note,omitempty"`
}

// RecordMissRequest /_internal/notebook/record-miss 请求体
type RecordMissRequest struct {
	UserID    string     `json:"userId"`
	SessionID string     `json:"sessionId,omitempty"`
	Items     []MissItem `json:"items"`
}

// NotebookClient 向 NestJS 回调错题本接口。
// 若 baseURL / secret 任一缺失，RecordMiss 变为 no-op（日志提示）。
type NotebookClient struct {
	baseURL string
	secret  string
	http    *http.Client
}

// NewNotebookClient 构造客户端；凭证未就位时仍返回非 nil 实例，调用方统一走 RecordMiss 接口。
func NewNotebookClient(nestBaseURL, secret string) *NotebookClient {
	return &NotebookClient{
		baseURL: nestBaseURL,
		secret:  secret,
		http:    &http.Client{Timeout: 5 * time.Second},
	}
}

// enabled 凭证 + base url 齐备时才真正发请求
func (c *NotebookClient) enabled() bool {
	return c.baseURL != "" && c.secret != ""
}

// RecordMiss 批量上报低分单词（score < 60 由 NestJS 侧再过滤一次）
func (c *NotebookClient) RecordMiss(userID, sessionID string, items []MissItem) {
	if !c.enabled() {
		return
	}
	if len(items) == 0 || userID == "" {
		return
	}

	body, err := json.Marshal(RecordMissRequest{
		UserID:    userID,
		SessionID: sessionID,
		Items:     items,
	})
	if err != nil {
		log.Printf("[NotebookClient] 编码失败: %v", err)
		return
	}

	url := c.baseURL + "/_internal/notebook/record-miss"
	req, err := http.NewRequest(http.MethodPost, url, bytes.NewReader(body))
	if err != nil {
		log.Printf("[NotebookClient] 构造请求失败: %v", err)
		return
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("X-Internal-Secret", c.secret)

	resp, err := c.http.Do(req)
	if err != nil {
		log.Printf("[NotebookClient] 发送失败: %v", err)
		return
	}
	defer resp.Body.Close()

	if resp.StatusCode >= 400 {
		log.Printf("[NotebookClient] 上报状态异常: %d", resp.StatusCode)
		return
	}
}

// ExtractMissesFromISE 从 ISE XML 里提取 <word content="X" total_score="Y"> 作为 miss 候选
// （NestJS 侧会再按阈值 < 60 过滤；这里做一个宽松上限避免把满分词都传上去）。
// 返回的最大 score 带回，便于未来复用；此处按 75 作为 pre-filter 上限。
func ExtractMissesFromISE(xml string) []MissItem {
	if xml == "" {
		return nil
	}
	items := []MissItem{}
	cursor := 0
	for {
		idx := indexFrom(xml, "<word", cursor)
		if idx < 0 {
			break
		}
		end := indexFrom(xml, "/>", idx)
		if end < 0 {
			end = indexFrom(xml, ">", idx)
		}
		if end < 0 {
			break
		}
		tag := xml[idx:end]
		word := extractAttr(tag, "content")
		score := extractAttrFloat(tag, "total_score")
		if word != "" && score > 0 && score < 75 {
			items = append(items, MissItem{Word: word, Score: score})
		}
		cursor = end
	}
	return items
}

// —— 小工具（避免引入额外依赖） ——

func indexFrom(s, sub string, from int) int {
	if from < 0 || from >= len(s) {
		return -1
	}
	rest := s[from:]
	i := 0
	for i+len(sub) <= len(rest) {
		if rest[i:i+len(sub)] == sub {
			return i + from
		}
		i++
	}
	return -1
}

func extractAttr(tag, name string) string {
	key := name + `="`
	i := indexFrom(tag, key, 0)
	if i < 0 {
		return ""
	}
	start := i + len(key)
	end := indexFrom(tag, `"`, start)
	if end < 0 {
		return ""
	}
	return tag[start:end]
}

func extractAttrFloat(tag, name string) float64 {
	s := extractAttr(tag, name)
	if s == "" {
		return 0
	}
	var v float64
	_, _ = fmt.Sscanf(s, "%f", &v)
	return v
}
