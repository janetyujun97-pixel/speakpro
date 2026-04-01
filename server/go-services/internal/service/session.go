package service

import (
	"sync"

	"github.com/speakpro/go-services/internal/model"
)

// ConversationState 单个会话的运行时状态
type ConversationState struct {
	SessionID string
	ExamType  string
	Section   string
	Mode      string
	History   []model.ConversationMessage // 对话历史（考官 + 学生）
	AudioBuf  []byte                      // 累积的音频数据
}

// SessionManager 管理所有活跃的 WebSocket 会话状态（内存版）
// Phase 2 Sprint 8 将升级为 Redis 后端
type SessionManager struct {
	mu       sync.RWMutex
	sessions map[string]*ConversationState
}

// NewSessionManager 创建会话管理器
func NewSessionManager() *SessionManager {
	return &SessionManager{
		sessions: make(map[string]*ConversationState),
	}
}

// CreateSession 创建新会话状态
func (sm *SessionManager) CreateSession(sessionID, examType, section, mode string) *ConversationState {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	state := &ConversationState{
		SessionID: sessionID,
		ExamType:  examType,
		Section:   section,
		Mode:      mode,
		History:   make([]model.ConversationMessage, 0),
		AudioBuf:  make([]byte, 0),
	}
	sm.sessions[sessionID] = state
	return state
}

// GetSession 获取会话状态
func (sm *SessionManager) GetSession(sessionID string) *ConversationState {
	sm.mu.RLock()
	defer sm.mu.RUnlock()
	return sm.sessions[sessionID]
}

// AppendMessage 追加一条对话消息到历史
func (sm *SessionManager) AppendMessage(sessionID, role, text string) {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	state, ok := sm.sessions[sessionID]
	if !ok {
		return
	}
	state.History = append(state.History, model.ConversationMessage{
		Type: role, // "examiner" 或 "student"
		Data: text,
	})
}

// AccumulateAudio 累积音频数据块
func (sm *SessionManager) AccumulateAudio(sessionID string, chunk []byte) {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	state, ok := sm.sessions[sessionID]
	if !ok {
		return
	}
	state.AudioBuf = append(state.AudioBuf, chunk...)
}

// GetAccumulatedAudio 获取累积的完整音频数据
func (sm *SessionManager) GetAccumulatedAudio(sessionID string) []byte {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	state, ok := sm.sessions[sessionID]
	if !ok {
		return nil
	}
	return state.AudioBuf
}

// ClearAudioBuffer 清空音频缓冲区（评测完成后调用）
func (sm *SessionManager) ClearAudioBuffer(sessionID string) {
	sm.mu.Lock()
	defer sm.mu.Unlock()

	state, ok := sm.sessions[sessionID]
	if !ok {
		return
	}
	state.AudioBuf = make([]byte, 0)
}

// GetHistory 获取对话历史
func (sm *SessionManager) GetHistory(sessionID string) []model.ConversationMessage {
	sm.mu.RLock()
	defer sm.mu.RUnlock()

	state, ok := sm.sessions[sessionID]
	if !ok {
		return nil
	}
	// 返回副本
	history := make([]model.ConversationMessage, len(state.History))
	copy(history, state.History)
	return history
}

// CleanupSession 清理会话状态
func (sm *SessionManager) CleanupSession(sessionID string) {
	sm.mu.Lock()
	defer sm.mu.Unlock()
	delete(sm.sessions, sessionID)
}
