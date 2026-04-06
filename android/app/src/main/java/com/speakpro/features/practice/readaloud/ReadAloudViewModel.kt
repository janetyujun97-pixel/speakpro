package com.speakpro.features.practice.readaloud

import android.media.MediaPlayer
import android.util.Base64
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * 朗读练习阶段
 */
enum class ReadAloudPhase {
    READING,     // 阅读文章 + 听示范
    RECORDING,   // 录音中
    EVALUATING,  // 评测中
    RESULT,      // 显示结果
}

/**
 * 朗读练习 ViewModel
 *
 * 流程：加载多篇文章 → 阅读 + TTS 示范 → 录音 → 评测 → 结果/下一篇
 */
class ReadAloudViewModel : ViewModel() {

    companion object {
        private const val TAG = "ReadAloudVM"
    }

    // ── 阶段 ──

    private val _phase = MutableStateFlow(ReadAloudPhase.READING)
    val phase: StateFlow<ReadAloudPhase> = _phase.asStateFlow()

    // ── 文章数据 ──

    private val _articleTitle = MutableStateFlow("加载中...")
    val articleTitle: StateFlow<String> = _articleTitle.asStateFlow()

    private val _articleText = MutableStateFlow("")
    val articleText: StateFlow<String> = _articleText.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _totalArticles = MutableStateFlow(0)
    val totalArticles: StateFlow<Int> = _totalArticles.asStateFlow()

    // ── 播放状态 ──

    private val _isPlayingDemo = MutableStateFlow(false)
    val isPlayingDemo: StateFlow<Boolean> = _isPlayingDemo.asStateFlow()

    private val _isPlayingStudent = MutableStateFlow(false)
    val isPlayingStudent: StateFlow<Boolean> = _isPlayingStudent.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── 评分 ──

    private val _overallScore = MutableStateFlow(0.0)
    val overallScore: StateFlow<Double> = _overallScore.asStateFlow()

    private val _pronunciationScore = MutableStateFlow(0.0)
    val pronunciationScore: StateFlow<Double> = _pronunciationScore.asStateFlow()

    private val _fluencyScore = MutableStateFlow(0.0)
    val fluencyScore: StateFlow<Double> = _fluencyScore.asStateFlow()

    private val _completenessScore = MutableStateFlow(0.0)
    val completenessScore: StateFlow<Double> = _completenessScore.asStateFlow()

    private val _aiFeedback = MutableStateFlow<String?>(null)
    val aiFeedback: StateFlow<String?> = _aiFeedback.asStateFlow()

    // ── 波形 ──

    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData: StateFlow<List<Float>> = _waveformData.asStateFlow()

    // ── 内部 ──

    private var articles: List<ArticleItem> = emptyList()
    private var mediaPlayer: MediaPlayer? = null

    val isLastArticle: Boolean
        get() = _currentIndex.value >= articles.size - 1

    init {
        loadQuestions()
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    // ── 加载题目 ──

    private fun loadQuestions() {
        viewModelScope.launch {
            try {
                val api = nestRetrofit.create(ReadAloudApi::class.java)
                val response = api.getQuestions(examType = "IELTS", limit = 20)
                val items = response.data?.items
                if (!items.isNullOrEmpty()) {
                    articles = items.map { ArticleItem(it.id, it.topic ?: "Reading Passage", it.promptText) }
                } else {
                    articles = defaultArticles
                }
            } catch (e: Exception) {
                Log.w(TAG, "文章加载失败", e)
                articles = defaultArticles
            }
            _totalArticles.value = articles.size
            loadCurrentArticle()
        }
    }

    private fun loadCurrentArticle() {
        val idx = _currentIndex.value
        if (idx < articles.size) {
            _articleTitle.value = articles[idx].title
            _articleText.value = articles[idx].text
        }
    }

    fun nextArticle() {
        if (_currentIndex.value >= articles.size - 1) return
        _currentIndex.value += 1
        resetForNewArticle()
        loadCurrentArticle()
    }

    private fun resetForNewArticle() {
        _phase.value = ReadAloudPhase.READING
        _overallScore.value = 0.0
        _pronunciationScore.value = 0.0
        _fluencyScore.value = 0.0
        _completenessScore.value = 0.0
        _aiFeedback.value = null
        _errorMessage.value = null
        _waveformData.value = emptyList()
    }

    fun retryRecording() {
        resetForNewArticle()
    }

    // ── TTS 示范播放 ──

    fun playDemo() {
        if (_isPlayingDemo.value) {
            stopAudio()
            _isPlayingDemo.value = false
            return
        }
        if (_articleText.value.isEmpty()) return
        _isPlayingDemo.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                val speed = when {
                    _playbackSpeed.value <= 0.5f -> 30
                    _playbackSpeed.value >= 1.5f -> 70
                    else -> 50
                }
                val api = goRetrofit.create(ReadAloudApi::class.java)
                val resp = api.synthesize(TtsRequestBody(_articleText.value, speed))
                val ttsData = resp.data

                if (ttsData == null || ttsData.audioB64.isBlank()) {
                    _isPlayingDemo.value = false
                    _errorMessage.value = "TTS 返回数据无效"
                    return@launch
                }

                val audioBytes = Base64.decode(ttsData.audioB64, Base64.DEFAULT)
                playAudioBytes(audioBytes) {
                    _isPlayingDemo.value = false
                }
            } catch (e: Exception) {
                _isPlayingDemo.value = false
                _errorMessage.value = "TTS 播放失败"
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
        // 模拟播放
        viewModelScope.launch {
            delay(2000)
            _isPlayingStudent.value = false
        }
    }

    // ── 录音 ──

    fun startRecording() {
        _errorMessage.value = null
        _isRecording.value = true
        _phase.value = ReadAloudPhase.RECORDING
        generateFakeWaveform()
    }

    fun stopRecording() {
        _isRecording.value = false
        _phase.value = ReadAloudPhase.EVALUATING
        evaluateAudio()
    }

    // ── 评测 ──

    private fun evaluateAudio() {
        viewModelScope.launch {
            try {
                val api = goRetrofit.create(ReadAloudApi::class.java)
                val resp = api.evaluate(
                    EvalRequestBody(audioB64 = "", referenceText = _articleText.value),
                )
                val result = resp.data
                if (result != null) {
                    _pronunciationScore.value = result.pronunciationScore?.overall ?: 0.0
                    _fluencyScore.value = result.pronunciationScore?.fluency ?: 0.0
                    _completenessScore.value = result.pronunciationScore?.integrity ?: 0.0
                    _overallScore.value = _pronunciationScore.value * 0.4 +
                        _fluencyScore.value * 0.3 + _completenessScore.value * 0.3
                }
                _phase.value = ReadAloudPhase.RESULT

                // AI 反馈（可选）
                try {
                    val fbResp = api.feedback(
                        FeedbackRequestBody(
                            sessionId = "read_aloud_${System.currentTimeMillis()}",
                            transcript = _articleText.value,
                            referenceText = _articleText.value,
                        ),
                    )
                    _aiFeedback.value = fbResp.data?.aiFeedback
                } catch (_: Exception) { /* AI 反馈失败不影响评分 */ }
            } catch (e: Exception) {
                _errorMessage.value = "评测失败: ${e.message}"
                _phase.value = ReadAloudPhase.RESULT
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
                Log.e(TAG, "播放失败", e)
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
                _waveformData.value = List(50) { (Math.random() * 0.8f + 0.1f).toFloat() }
                delay(100)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mediaPlayer?.release()
    }

    // ── 默认文章 ──

    private val defaultArticles = listOf(
        ArticleItem("d1", "Climate Change",
            "Climate change is one of the most pressing issues of our time. Scientists around the world have observed significant changes in global temperatures over the past century. The effects of rising temperatures include more frequent extreme weather events, rising sea levels, and shifts in ecosystems that affect biodiversity."),
        ArticleItem("d2", "Artificial Intelligence",
            "Artificial intelligence has transformed the way we live and work. From voice assistants to self-driving cars, AI technologies are becoming increasingly integrated into our daily lives. While these advancements offer tremendous benefits, they also raise important questions about privacy, employment, and ethics."),
        ArticleItem("d3", "Space Exploration",
            "The exploration of space has captivated human imagination for centuries. Recent developments in rocket technology have made space travel more accessible than ever before. Private companies are now competing alongside government agencies to push the boundaries of what is possible in space."),
        ArticleItem("d4", "Sustainable Energy",
            "The transition to sustainable energy sources is crucial for the future of our planet. Solar and wind power have become increasingly cost-effective alternatives to fossil fuels. Many countries are setting ambitious targets to achieve carbon neutrality within the next few decades."),
        ArticleItem("d5", "Global Education",
            "Education is widely recognized as one of the most powerful tools for reducing poverty and inequality. Access to quality education varies significantly across different regions of the world. Technology has the potential to bridge this gap by providing learning opportunities to underserved communities."),
    )
}

// ── 内部模型 ──

private data class ArticleItem(val id: String, val title: String, val text: String)

private data class QListResponse(val items: List<QItem>? = null)
private data class QItem(
    val id: String,
    val topic: String? = null,
    @SerializedName("prompt_text") val promptText: String,
)

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
    val integrity: Double? = null,
    val intonation: Double? = null,
)

private data class FeedbackRequestBody(
    @SerializedName("session_id") val sessionId: String,
    val transcript: String,
    @SerializedName("reference_text") val referenceText: String,
)

private data class FeedbackResponse(
    @SerializedName("ai_feedback") val aiFeedback: String? = null,
    val corrections: List<String>? = null,
)

private interface ReadAloudApi {
    @GET(Endpoints.Questions.LIST)
    suspend fun getQuestions(
        @Query("exam_type") examType: String,
        @Query("limit") limit: Int,
    ): ApiResponse<QListResponse>

    @POST(Endpoints.Tts.SYNTHESIZE)
    suspend fun synthesize(@Body body: TtsRequestBody): ApiResponse<TtsResponseData>

    @POST(Endpoints.Assessment.EVALUATE)
    suspend fun evaluate(@Body body: EvalRequestBody): ApiResponse<EvalResponse>

    @POST(Endpoints.Assessment.FEEDBACK)
    suspend fun feedback(@Body body: FeedbackRequestBody): ApiResponse<FeedbackResponse>
}
