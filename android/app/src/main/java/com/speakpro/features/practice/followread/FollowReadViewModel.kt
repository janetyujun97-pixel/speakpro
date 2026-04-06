package com.speakpro.features.practice.followread

import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiClient
import com.speakpro.core.network.ApiClient.goRetrofit
import com.speakpro.core.network.ApiClient.nestRetrofit
import com.speakpro.core.network.Endpoints
import com.speakpro.data.models.ApiResponse
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.File

/**
 * 跟读练习阶段
 */
enum class FollowReadPhase {
    READY,       // 准备播放参考音
    LISTENING,   // 播放参考音中
    RECORDING,   // 录音中
    EVALUATING,  // 评测中
    RESULT,      // 显示评测结果
}

/**
 * 单句评分记录
 */
data class SentenceScore(
    val sentence: String,
    val pronunciation: Double,
    val intonation: Double,
    val fluency: Double,
)

/**
 * 跟读练习 ViewModel
 *
 * 流程：加载句子 → 播放参考音 (TTS) → 录音 → 评测 → 显示结果 → 下一句
 */
class FollowReadViewModel : ViewModel() {

    companion object {
        private const val TAG = "FollowReadVM"
    }

    // ── 阶段 ──

    private val _phase = MutableStateFlow(FollowReadPhase.READY)
    val phase: StateFlow<FollowReadPhase> = _phase.asStateFlow()

    // ── 句子数据 ──

    private val _currentSentence = MutableStateFlow("")
    val currentSentence: StateFlow<String> = _currentSentence.asStateFlow()

    private val _currentSentenceIndex = MutableStateFlow(0)
    val currentSentenceIndex: StateFlow<Int> = _currentSentenceIndex.asStateFlow()

    private val _totalSentences = MutableStateFlow(0)
    val totalSentences: StateFlow<Int> = _totalSentences.asStateFlow()

    // ── 评分 ──

    private val _pronunciationScore = MutableStateFlow(0.0)
    val pronunciationScore: StateFlow<Double> = _pronunciationScore.asStateFlow()

    private val _intonationScore = MutableStateFlow(0.0)
    val intonationScore: StateFlow<Double> = _intonationScore.asStateFlow()

    private val _fluencyScore = MutableStateFlow(0.0)
    val fluencyScore: StateFlow<Double> = _fluencyScore.asStateFlow()

    private val _hasScore = MutableStateFlow(false)
    val hasScore: StateFlow<Boolean> = _hasScore.asStateFlow()

    private val _phonemeErrors = MutableStateFlow<List<String>>(emptyList())
    val phonemeErrors: StateFlow<List<String>> = _phonemeErrors.asStateFlow()

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    // ── 评分历史 ──

    private val _scoreHistory = MutableStateFlow<List<SentenceScore>>(emptyList())
    val scoreHistory: StateFlow<List<SentenceScore>> = _scoreHistory.asStateFlow()

    // ── 播放/录音状态 ──

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPlayingReference = MutableStateFlow(false)
    val isPlayingReference: StateFlow<Boolean> = _isPlayingReference.asStateFlow()

    private val _isPlayingStudent = MutableStateFlow(false)
    val isPlayingStudent: StateFlow<Boolean> = _isPlayingStudent.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── 波形 ──

    private val _referenceWaveform = MutableStateFlow<List<Float>>(emptyList())
    val referenceWaveform: StateFlow<List<Float>> = _referenceWaveform.asStateFlow()

    private val _studentWaveform = MutableStateFlow<List<Float>>(emptyList())
    val studentWaveform: StateFlow<List<Float>> = _studentWaveform.asStateFlow()

    // ── 内部数据 ──

    private var sentences: List<SentenceItem> = emptyList()
    private var mediaPlayer: MediaPlayer? = null

    init {
        loadSentences()
    }

    // ── 加载句子 ──

    private fun loadSentences() {
        viewModelScope.launch {
            try {
                val api = nestRetrofit.create(FollowReadApi::class.java)
                val response = api.getQuestions(examType = "IELTS", section = "FollowRead", limit = 10)
                val items = response.data?.items
                if (!items.isNullOrEmpty()) {
                    sentences = items.map { SentenceItem(it.id, it.promptText) }
                } else {
                    sentences = defaultSentences
                }
            } catch (e: Exception) {
                Log.w(TAG, "句子加载失败，使用默认句子", e)
                sentences = defaultSentences
            }
            _totalSentences.value = sentences.size
            loadCurrentSentence()
        }
    }

    private fun loadCurrentSentence() {
        val idx = _currentSentenceIndex.value
        if (idx < sentences.size) {
            _currentSentence.value = sentences[idx].text
        }
    }

    // ── 导航 ──

    fun nextSentence() {
        if (_currentSentenceIndex.value >= sentences.size - 1) return
        _currentSentenceIndex.value += 1
        resetScores()
        loadCurrentSentence()
        _phase.value = FollowReadPhase.READY
    }

    fun retryRecording() {
        resetScores()
        _phase.value = FollowReadPhase.READY
    }

    private fun resetScores() {
        _hasScore.value = false
        _pronunciationScore.value = 0.0
        _intonationScore.value = 0.0
        _fluencyScore.value = 0.0
        _phonemeErrors.value = emptyList()
        _studentWaveform.value = emptyList()
        _referenceWaveform.value = emptyList()
    }

    // ── TTS 参考音播放 + 自动过渡 ──

    fun playReferenceAndTransition() {
        _phase.value = FollowReadPhase.LISTENING
        _errorMessage.value = null
        playReference {
            if (_errorMessage.value != null) {
                _phase.value = FollowReadPhase.READY
                return@playReference
            }
            viewModelScope.launch {
                delay(300)
                _phase.value = FollowReadPhase.RECORDING
            }
        }
    }

    fun playReference(completion: (() -> Unit)? = null) {
        if (_isPlayingReference.value) {
            stopAudio()
            _isPlayingReference.value = false
            return
        }
        if (_currentSentence.value.isEmpty()) return
        _isPlayingReference.value = true

        viewModelScope.launch {
            try {
                val api = goRetrofit.create(FollowReadApi::class.java)
                val resp = api.synthesize(TtsRequestBody(_currentSentence.value, 50))
                val ttsData = resp.data

                if (ttsData == null || ttsData.audioB64.isBlank()) {
                    _isPlayingReference.value = false
                    _errorMessage.value = "TTS 返回数据为空"
                    completion?.invoke()
                    return@launch
                }

                val audioBytes = Base64.decode(ttsData.audioB64, Base64.DEFAULT)
                // 生成参考波形（模拟）
                _referenceWaveform.value = List(60) { (Math.random() * 0.8f + 0.2f).toFloat() }

                playAudioBytes(audioBytes) {
                    _isPlayingReference.value = false
                    completion?.invoke()
                }
            } catch (e: Exception) {
                _isPlayingReference.value = false
                _errorMessage.value = "参考音播放失败"
                completion?.invoke()
            }
        }
    }

    // ── 播放学生录音 ──

    fun playStudentRecording() {
        if (_isPlayingStudent.value) {
            stopAudio()
            _isPlayingStudent.value = false
            return
        }
        _isPlayingStudent.value = true
        // 模拟播放（实际项目对接录音文件路径）
        viewModelScope.launch {
            delay(2000)
            _isPlayingStudent.value = false
        }
    }

    // ── 录音 ──

    fun startRecording() {
        _hasScore.value = false
        _errorMessage.value = null
        _studentWaveform.value = emptyList()
        _isRecording.value = true
        // 模拟录音波形
        generateFakeWaveform()
    }

    fun stopRecording() {
        _isRecording.value = false
        _studentWaveform.value = List(60) { (Math.random() * 0.7f + 0.1f).toFloat() }
        _phase.value = FollowReadPhase.EVALUATING
        evaluateAudio()
    }

    // ── 评测 ──

    private fun evaluateAudio() {
        viewModelScope.launch {
            try {
                val api = goRetrofit.create(FollowReadApi::class.java)
                // 模拟音频数据（实际项目传入真实录音 base64）
                val resp = api.evaluate(
                    EvalRequestBody(
                        audioB64 = "",  // 实际项目填充真实数据
                        referenceText = _currentSentence.value,
                    ),
                )
                val result = resp.data
                if (result != null) {
                    _hasScore.value = true
                    _pronunciationScore.value = result.pronunciationScore?.overall ?: 0.0
                    _intonationScore.value = result.pronunciationScore?.intonation ?: 0.0
                    _fluencyScore.value = result.pronunciationScore?.fluency ?: 0.0

                    _scoreHistory.value = _scoreHistory.value + SentenceScore(
                        sentence = _currentSentence.value,
                        pronunciation = _pronunciationScore.value,
                        intonation = _intonationScore.value,
                        fluency = _fluencyScore.value,
                    )
                }

                _phase.value = FollowReadPhase.RESULT

                if (_currentSentenceIndex.value >= _totalSentences.value - 1) {
                    _isCompleted.value = true
                }
            } catch (e: Exception) {
                _errorMessage.value = "评测失败: ${e.message}"
                _phase.value = FollowReadPhase.RESULT
            }
        }
    }

    // ── 音频辅助 ──

    private fun playAudioBytes(data: ByteArray, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tmpFile = File.createTempFile("tts_", ".mp3")
                tmpFile.writeBytes(data)
                viewModelScope.launch(Dispatchers.Main) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(tmpFile.absolutePath)
                        prepare()
                        setOnCompletionListener {
                            onComplete()
                            tmpFile.delete()
                        }
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "音频播放失败", e)
                viewModelScope.launch(Dispatchers.Main) { onComplete() }
            }
        }
    }

    private fun stopAudio() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun generateFakeWaveform() {
        viewModelScope.launch {
            while (_isRecording.value) {
                _studentWaveform.value = List(60) { (Math.random() * 0.8f + 0.1f).toFloat() }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }

    // ── 默认句子 ──

    private val defaultSentences = listOf(
        SentenceItem("1", "The quick brown fox jumps over the lazy dog."),
        SentenceItem("2", "She sells seashells by the seashore."),
        SentenceItem("3", "How much wood would a woodchuck chuck?"),
        SentenceItem("4", "Peter Piper picked a peck of pickled peppers."),
        SentenceItem("5", "I scream, you scream, we all scream for ice cream."),
    )
}

// ── 内部模型 ──

private data class SentenceItem(val id: String, val text: String)

// ── Retrofit API ──

private data class QListResponse(val items: List<QItem>? = null)
private data class QItem(val id: String, @SerializedName("prompt_text") val promptText: String)

private data class TtsRequestBody(val text: String, val speed: Int)
private data class TtsResponseData(
    @SerializedName("audio_b64") val audioB64: String,
    val format: String? = null,
)

private data class EvalRequestBody(
    @SerializedName("audio_b64") val audioB64: String,
    @SerializedName("reference_text") val referenceText: String,
)

private data class EvalResponse(
    @SerializedName("pronunciation_score") val pronunciationScore: PronScoreResp? = null,
)

private data class PronScoreResp(
    val overall: Double? = null,
    val fluency: Double? = null,
    val intonation: Double? = null,
    val integrity: Double? = null,
)

private interface FollowReadApi {
    @GET(Endpoints.Questions.LIST)
    suspend fun getQuestions(
        @Query("exam_type") examType: String,
        @Query("section") section: String,
        @Query("limit") limit: Int,
    ): ApiResponse<QListResponse>

    @POST(Endpoints.Tts.SYNTHESIZE)
    suspend fun synthesize(@Body body: TtsRequestBody): ApiResponse<TtsResponseData>

    @POST(Endpoints.Assessment.EVALUATE)
    suspend fun evaluate(@Body body: EvalRequestBody): ApiResponse<EvalResponse>
}
