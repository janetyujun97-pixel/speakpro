package com.speakpro.core.network

import android.util.Log
import com.google.gson.Gson
import com.speakpro.core.storage.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * WebSocket 连接状态
 */
enum class WebSocketConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

/**
 * WebSocket 连接管理器 — 基于 OkHttp WebSocket API
 *
 * 负责与 Go 服务的 WebSocket 端点通信，支持：
 * - 自动重连（指数退避，最多 5 次）
 * - 自动响应服务端 Ping
 * - 类型化消息解析（通过 [WsMessageParser]）
 * - 通过 [StateFlow] 暴露连接状态
 *
 * 使用方式：
 * ```kotlin
 * val ws = WebSocketManager()
 * ws.onTypedMessage = { msg ->
 *     when (msg) {
 *         is WSServerMessage.Examiner -> handleExaminer(msg)
 *         is WSServerMessage.Transcript -> handleTranscript(msg)
 *         // ...
 *     }
 * }
 * ws.connect(sessionId = "abc123")
 * ws.sendJson(WSClientMessage(type = "text", data = TextMessageData("Hello")))
 * ```
 */
class WebSocketManager {

    companion object {
        private const val TAG = "WebSocketManager"

        /** 最大重连尝试次数 */
        private const val MAX_RECONNECT_ATTEMPTS = 5

        /** 基础重连延迟（毫秒） */
        private const val BASE_RECONNECT_DELAY_MS = 2000L

        /** OkHttp 正常关闭码 */
        private const val NORMAL_CLOSURE_CODE = 1000
    }

    // ===================== 状态 =====================

    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()

    // ===================== 回调 =====================

    /** 收到原始文本消息时的回调 */
    var onMessage: ((String) -> Unit)? = null

    /** 收到类型化服务端消息时的回调 */
    var onTypedMessage: ((WSServerMessage) -> Unit)? = null

    /** 连接断开时的回调（参数为断开原因，正常关闭时为 null） */
    var onDisconnect: ((Throwable?) -> Unit)? = null

    // ===================== 内部变量 =====================

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var currentSessionId: String? = null
    private var reconnectAttempts = 0
    private var isManualDisconnect = false

    /** 用于延迟重连的调度器 */
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private var reconnectFuture: ScheduledFuture<*>? = null

    /**
     * 共享 OkHttpClient 实例
     *
     * WebSocket 不需要走 Retrofit 的拦截器链，
     * 这里创建一个轻量级的专用 client。
     */
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.SECONDS)   // WebSocket 需要无限读超时
            .writeTimeout(15, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)  // OkHttp 自动 ping 保活
            .build()
    }

    // ===================== 连接 =====================

    /**
     * 建立 WebSocket 连接
     *
     * @param sessionId 会话 ID（对应 Go 服务端的 conversation session）
     */
    fun connect(sessionId: String) {
        if (_connectionState.value == WebSocketConnectionState.CONNECTED) {
            Log.w(TAG, "已存在连接，先断开旧连接")
            disconnect()
        }

        currentSessionId = sessionId
        isManualDisconnect = false
        _connectionState.value = WebSocketConnectionState.CONNECTING

        val wsUrl = Endpoints.Conversation.wsConnect(sessionId)
        Log.i(TAG, "正在连接 WebSocket: $wsUrl")

        val requestBuilder = Request.Builder().url(wsUrl)

        // 附加 JWT Token
        TokenManager.accessToken?.let { token ->
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        webSocket = client.newWebSocket(requestBuilder.build(), createListener())
    }

    // ===================== 断开 =====================

    /**
     * 主动断开连接
     */
    fun disconnect() {
        isManualDisconnect = true
        cancelReconnect()
        reconnectAttempts = 0
        currentSessionId = null

        webSocket?.close(NORMAL_CLOSURE_CODE, "客户端主动断开")
        webSocket = null
        _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    // ===================== 发送 =====================

    /**
     * 发送原始文本消息
     */
    fun send(message: String) {
        if (_connectionState.value != WebSocketConnectionState.CONNECTED) {
            Log.w(TAG, "未连接，无法发送消息")
            return
        }
        val sent = webSocket?.send(message) ?: false
        if (!sent) {
            Log.e(TAG, "消息发送失败")
        }
    }

    /**
     * 将任意对象序列化为 JSON 并发送
     *
     * @param data 要发送的对象（通常是 [WSClientMessage]）
     */
    fun sendJson(data: Any) {
        val json = gson.toJson(data)
        send(json)
    }

    // ===================== WebSocket 监听器 =====================

    private fun createListener(): WebSocketListener = object : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.i(TAG, "WebSocket 已连接")
            _connectionState.value = WebSocketConnectionState.CONNECTED
            reconnectAttempts = 0
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // 通知原始消息回调
            onMessage?.invoke(text)

            // 解析为类型化消息
            val parsed = WsMessageParser.parse(text)
            onTypedMessage?.invoke(parsed)

            // 自动回复 Pong
            if (parsed is WSServerMessage.Ping) {
                val pong = WSClientMessage(type = WSClientMessage.TYPE_PONG)
                sendJson(pong)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket 正在关闭: code=$code, reason=$reason")
            webSocket.close(NORMAL_CLOSURE_CODE, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.i(TAG, "WebSocket 已关闭: code=$code, reason=$reason")
            _connectionState.value = WebSocketConnectionState.DISCONNECTED
            onDisconnect?.invoke(null)

            if (!isManualDisconnect) {
                attemptReconnect()
            }
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket 连接失败: ${t.message}")
            _connectionState.value = WebSocketConnectionState.DISCONNECTED
            onDisconnect?.invoke(t)

            if (!isManualDisconnect) {
                attemptReconnect()
            }
        }
    }

    // ===================== 自动重连 =====================

    private fun attemptReconnect() {
        val sessionId = currentSessionId ?: return

        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "已达到最大重连次数 ($MAX_RECONNECT_ATTEMPTS)，停止重连")
            return
        }

        reconnectAttempts++
        val delay = BASE_RECONNECT_DELAY_MS * reconnectAttempts
        Log.i(TAG, "将在 ${delay}ms 后尝试第 $reconnectAttempts 次重连...")

        reconnectFuture = scheduler.schedule(
            { connect(sessionId) },
            delay,
            TimeUnit.MILLISECONDS,
        )
    }

    private fun cancelReconnect() {
        reconnectFuture?.cancel(false)
        reconnectFuture = null
    }

    // ===================== 资源释放 =====================

    /**
     * 释放所有资源（Application 退出或不再需要时调用）
     */
    fun release() {
        disconnect()
        onMessage = null
        onTypedMessage = null
        onDisconnect = null
        scheduler.shutdownNow()
    }
}
