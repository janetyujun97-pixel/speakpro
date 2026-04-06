package com.speakpro.features.practice.conversation

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.speakpro.core.network.AudioChunkData
import com.speakpro.core.network.AudioCompleteData
import com.speakpro.core.network.Endpoints
import com.speakpro.core.network.SessionInitData
import com.speakpro.core.network.WSClientMessage
import com.speakpro.core.network.WSServerMessage
import com.speakpro.core.network.WsMessageParser
import com.speakpro.core.storage.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * 对话消息模型
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isExaminer: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null,
    val audioData: ByteArray? = null,
    val duration: Double? = null,
    val transcribedText: String? = null,
    val isVoiceMessage: Boolean = audioFilePath != null || audioData != null,
)

/**
 * AI 对话练习 ViewModel
 *
 * 通过 WebSocket 与 Go 服务实时交互：
 * 1. 创建 practice session
 * 2. 连接 WebSocket
 * 3. 发送 session_init
 * 4. 流式推送录音 audio_chunk
 * 5. 录音结束后发送 audio_complete
 * 6. 接收考官回复、评分更新
 */
class ConversationViewModel : ViewModel() {

    companion object {
        private const val TAG = "ConversationVM"
    }

    // ── UI 状态 ──

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isConnecting = MutableStateFlow(false)
    val isConnecting: StateFlow<Boolean> = _isConnecting.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _scores = MutableStateFlow<Map<String, Double>>(emptyMap())
    val scores: StateFlow<Map<String, Double>> = _scores.asStateFlow()

    private val _remainingTime = MutableStateFlow(120)
    val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

    private val _processingStatus = MutableStateFlow("")
    val processingStatus: StateFlow<String> = _processingStatus.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private val _playingMessageId = MutableStateFlow<String?>(null)
    val playingMessageId: StateFlow<String?> = _playingMessageId.asStateFlow()

    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData: StateFlow<List<Float>> = _waveformData.asStateFlow()

    // ── 配置 ──

    var examType: String = "IELTS"
    var section: String = "Part1"

    // ── 内部状态 ──

    private val gson = Gson()
    private var webSocket: WebSocket? = null
    private var sessionId: String? = null
    private var audioSequence = 0
    private var recordingStartTime = 0L
    private var timerJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null

    val formattedRemainingTime: String
        get() {
            val min = _remainingTime.value / 60
            val sec = _remainingTime.value % 60
            return "%d:%02d".format(min, sec)
        }

    // ── WebSocket 客户端 ──

    private val wsClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    // ── 对话生命周期 ──

    /**
     * 开始对话：重置状态 → 连接 WebSocket → 发送 session_init
     */
    fun startConversation() {
        if (_isConnecting.value || _isConnected.value) return

        // 重置
        timerJob?.cancel()
        stopAudio()
        _messages.value = emptyList()
        _scores.value = emptyMap()
        _remainingTime.value = 120
        _currentTranscript.value = ""
        _processingStatus.value = ""
        _isRecording.value = false
        _playingMessageId.value = null
        sessionId = null
        audioSequence = 0

        _isConnecting.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                // 生成一个临时 session ID（正式项目应从 API 获取）
                val sid = "conv_${System.currentTimeMillis()}"
                sessionId = sid

                // 连接 WebSocket
                val wsUrl = Endpoints.Conversation.wsConnect(sid)
                val request = Request.Builder()
                    .url(wsUrl)
                    .apply {
                        TokenManager.accessToken?.let {
                            addHeader("Authorization", "Bearer $it")
                        }
                    }
                    .build()

                webSocket = wsClient.newWebSocket(request, object : WebSocketListener() {
                    override fun onOpen(ws: WebSocket, response: Response) {
                        Log.d(TAG, "WebSocket 已连接")
                    }

                    override fun onMessage(ws: WebSocket, text: String) {
                        viewModelScope.launch(Dispatchers.Main) {
                            handleServerMessage(WsMessageParser.parse(text))
                        }
                    }

                    override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                        viewModelScope.launch(Dispatchers.Main) {
                            handleDisconnect(null)
                        }
                    }

                    override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            handleDisconnect(t)
                        }
                    }
                })

                // 延迟发送 session_init
                delay(500)
                val initMsg = WSClientMessage(
                    type = WSClientMessage.TYPE_SESSION_INIT,
                    data = SessionInitData(
                        sessionId = sid,
                        examType = examType,
                        section = section,
                        mode = "conversation",
                    ),
                )
                webSocket?.send(gson.toJson(initMsg))

            } catch (e: Exception) {
                _errorMessage.value = "连接失败: ${e.message}"
                _isConnecting.value = false
            }
        }
    }

    fun endConversation() {
        timerJob?.cancel()
        webSocket?.close(1000, "用户结束对话")
        webSocket = null
        if (_isRecording.value) {
            _isRecording.value = false
        }
        _isConnected.value = false
        _processingStatus.value = ""
    }

    // ── WebSocket 消息处理 ──

    private fun handleServerMessage(message: WSServerMessage) {
        when (message) {
            is WSServerMessage.SessionReady -> {
                _isConnecting.value = false
                _isConnected.value = true
                _remainingTime.value = message.timeLimitSec

                if (_messages.value.isEmpty()) {
                    val greetingMsg = ChatMessage(
                        text = message.examinerGreeting,
                        isExaminer = true,
                        audioData = message.greetingTtsB64?.let {
                            Base64.decode(it, Base64.DEFAULT)
                        },
                        duration = message.greetingTtsB64?.let {
                            estimateAudioDuration(Base64.decode(it, Base64.DEFAULT).size)
                        },
                    )
                    _messages.value = _messages.value + greetingMsg
                    // 自动播放开场白
                    if (greetingMsg.audioData != null) {
                        playAudio(greetingMsg)
                    }
                }
                startTimer()
            }

            is WSServerMessage.Transcript -> {
                _currentTranscript.value = message.text
                if (message.isFinal) {
                    // 更新最后一条学生语音消息的转写文本
                    val msgs = _messages.value.toMutableList()
                    val lastIdx = msgs.indexOfLast { !it.isExaminer && it.text == "[语音消息]" }
                    if (lastIdx >= 0) {
                        msgs[lastIdx] = msgs[lastIdx].copy(transcribedText = message.text)
                        _messages.value = msgs
                    }
                    _currentTranscript.value = ""
                }
            }

            is WSServerMessage.Examiner -> {
                val audioBytes = message.ttsAudioB64?.let {
                    Base64.decode(it, Base64.DEFAULT)
                }
                val msg = ChatMessage(
                    text = message.text,
                    isExaminer = true,
                    audioData = audioBytes,
                    duration = audioBytes?.let { estimateAudioDuration(it.size) },
                )
                _messages.value = _messages.value + msg
                _processingStatus.value = ""
                // 自动播放考官回复
                if (msg.audioData != null) {
                    playAudio(msg)
                }
            }

            is WSServerMessage.ScoreUpdate -> {
                val newScores = mutableMapOf<String, Double>()
                message.pronunciation?.overall?.let { newScores["发音"] = it }
                message.pronunciation?.fluency?.let { newScores["流利度"] = it }
                message.grammar?.score?.let { s ->
                    newScores["语法"] = if (s <= 10) s * 10 else s
                }
                newScores["总分"] = message.overall
                _scores.value = newScores
                _processingStatus.value = ""
            }

            is WSServerMessage.Error -> {
                _errorMessage.value = "${message.code}: ${message.message}"
                _processingStatus.value = ""
            }

            is WSServerMessage.Processing -> {
                _processingStatus.value = message.message
            }

            is WSServerMessage.Ping -> { /* 忽略 */ }

            is WSServerMessage.Unknown -> {
                Log.w(TAG, "未知消息: ${message.raw}")
            }
        }
    }

    private fun handleDisconnect(error: Throwable?) {
        _isConnected.value = false
        _isConnecting.value = false
        error?.let {
            _errorMessage.value = "连接断开: ${it.message}"
        }
    }

    // ── 录音 ──

    fun startRecording() {
        if (_isRecording.value) return
        _errorMessage.value = null
        audioSequence = 0
        recordingStartTime = System.currentTimeMillis()
        _isRecording.value = true
        // 模拟波形（实际项目中由 AudioRecorder 提供）
        generateFakeWaveform()
    }

    fun stopAndSendAudio() {
        if (!_isRecording.value) return
        _isRecording.value = false
        _waveformData.value = emptyList()

        val duration = (System.currentTimeMillis() - recordingStartTime) / 1000.0

        _messages.value = _messages.value + ChatMessage(
            text = "[语音消息]",
            isExaminer = false,
            duration = duration,
        )

        // 发送 audio_complete
        sessionId?.let { sid ->
            val msg = WSClientMessage(
                type = WSClientMessage.TYPE_AUDIO_COMPLETE,
                data = AudioCompleteData(sessionId = sid, referenceText = null),
            )
            webSocket?.send(gson.toJson(msg))
        }
        _processingStatus.value = "正在处理..."
    }

    /** 模拟波形数据（实际项目应对接 AudioRecorder） */
    private fun generateFakeWaveform() {
        viewModelScope.launch {
            while (_isRecording.value) {
                val newData = List(50) { (Math.random() * 0.8f + 0.1f).toFloat() }
                _waveformData.value = newData
                delay(100)
            }
        }
    }

    // ── 语音播放 ──

    fun playAudio(message: ChatMessage) {
        if (_playingMessageId.value == message.id) {
            stopAudio()
            return
        }
        stopAudio()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val audioData = message.audioData ?: return@launch

                // 检测音频格式
                val isMP3 = audioData.size > 3 && (
                    (audioData[0].toInt() and 0xFF == 0xFF && audioData[1].toInt() and 0xE0 == 0xE0) ||
                    (audioData[0].toInt() == 0x49 && audioData[1].toInt() == 0x44 && audioData[2].toInt() == 0x33)
                )

                if (isMP3) {
                    // MP3：写临时文件后用 MediaPlayer 播放
                    val tmpFile = File.createTempFile("audio_", ".mp3")
                    FileOutputStream(tmpFile).use { it.write(audioData) }

                    viewModelScope.launch(Dispatchers.Main) {
                        mediaPlayer = MediaPlayer().apply {
                            setDataSource(tmpFile.absolutePath)
                            prepare()
                            setOnCompletionListener {
                                _playingMessageId.value = null
                                tmpFile.delete()
                            }
                            start()
                        }
                        _playingMessageId.value = message.id
                    }
                } else {
                    // PCM 16kHz 16bit mono：用 AudioTrack 播放
                    val sampleRate = 16000
                    val bufferSize = AudioTrack.getMinBufferSize(
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                    )
                    val audioTrack = AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize.coerceAtLeast(audioData.size),
                        AudioTrack.MODE_STATIC,
                    )
                    audioTrack.write(audioData, 0, audioData.size)
                    viewModelScope.launch(Dispatchers.Main) {
                        _playingMessageId.value = message.id
                    }
                    audioTrack.play()
                    // 等待播放完毕
                    val durationMs = (audioData.size.toDouble() / 32000.0 * 1000).toLong()
                    delay(durationMs)
                    audioTrack.release()
                    viewModelScope.launch(Dispatchers.Main) {
                        _playingMessageId.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "播放失败", e)
                viewModelScope.launch(Dispatchers.Main) {
                    _playingMessageId.value = null
                }
            }
        }
    }

    fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _playingMessageId.value = null
    }

    // ── 转文字 ──

    fun convertToText(messageId: String) {
        val msgs = _messages.value.toMutableList()
        val idx = msgs.indexOfFirst { it.id == messageId }
        if (idx < 0) return
        if (msgs[idx].transcribedText != null) return

        if (msgs[idx].isExaminer) {
            msgs[idx] = msgs[idx].copy(transcribedText = msgs[idx].text)
        } else if (msgs[idx].text != "[语音消息]") {
            msgs[idx] = msgs[idx].copy(transcribedText = msgs[idx].text)
        } else {
            msgs[idx] = msgs[idx].copy(transcribedText = "（语音识别中，请稍候...）")
        }
        _messages.value = msgs
    }

    // ── 删除消息 ──

    fun deleteMessage(id: String) {
        _messages.value = _messages.value.filter { it.id != id }
    }

    // ── 计时器 ──

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                _remainingTime.value = _remainingTime.value - 1
            }
            endConversation()
        }
    }

    // ── 辅助 ──

    /** 估算 PCM 音频时长（16kHz, 16-bit, mono = 32000 bytes/sec） */
    private fun estimateAudioDuration(dataSize: Int): Double {
        return dataSize / 32000.0
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        webSocket?.close(1000, "ViewModel 销毁")
        mediaPlayer?.release()
    }
}
