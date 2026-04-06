package com.speakpro.features.practice.mockexam

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiClient.goRetrofit
import com.speakpro.core.network.ApiClient.nestRetrofit
import com.speakpro.core.network.Endpoints
import com.speakpro.data.models.ApiResponse
import com.speakpro.data.models.FullEvaluateResult
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 模考阶段
 */
enum class ExamPhase {
    LOADING,
    READY,
    IN_PROGRESS,
    EVALUATING,
    SHOWING_RESULT,
    SECTION_TRANSITION,
    FINISHED,
}

/**
 * 每题的丰富评分数据
 */
data class EnrichedQuestionScore(
    val part: Int,
    val question: String,
    val score: Double,
    val audioFilePath: String? = null,
    val fullResult: FullEvaluateResult? = null,
)

/**
 * 模考 ViewModel
 *
 * 加载真实题目 → 逐题录音 → 完整 AI 评测 → 分 Part 汇总
 */
class MockExamViewModel : ViewModel() {

    companion object {
        private const val TAG = "MockExamVM"
    }

    // ── 阶段 ──

    private val _phase = MutableStateFlow(ExamPhase.LOADING)
    val phase: StateFlow<ExamPhase> = _phase.asStateFlow()

    // ── 题目数据 ──

    private val _currentPart = MutableStateFlow(1)
    val currentPart: StateFlow<Int> = _currentPart.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentQuestion = MutableStateFlow("")
    val currentQuestion: StateFlow<String> = _currentQuestion.asStateFlow()

    private val _subQuestions = MutableStateFlow<List<String>>(emptyList())
    val subQuestions: StateFlow<List<String>> = _subQuestions.asStateFlow()

    // ── 状态 ──

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _remainingTime = MutableStateFlow(60)
    val remainingTime: StateFlow<Int> = _remainingTime.asStateFlow()

    private val _evaluationProgress = MutableStateFlow("")
    val evaluationProgress: StateFlow<String> = _evaluationProgress.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── 当前题结果 ──

    private val _currentResult = MutableStateFlow<FullEvaluateResult?>(null)
    val currentResult: StateFlow<FullEvaluateResult?> = _currentResult.asStateFlow()

    // ── 考试结果 ──

    private val _examScores = MutableStateFlow<List<EnrichedQuestionScore>>(emptyList())
    val examScores: StateFlow<List<EnrichedQuestionScore>> = _examScores.asStateFlow()

    private val _overallScore = MutableStateFlow(0.0)
    val overallScore: StateFlow<Double> = _overallScore.asStateFlow()

    // ── 部分过渡 ──

    private val _transitionFromPart = MutableStateFlow(0)
    val transitionFromPart: StateFlow<Int> = _transitionFromPart.asStateFlow()

    private val _transitionToPart = MutableStateFlow(0)
    val transitionToPart: StateFlow<Int> = _transitionToPart.asStateFlow()

    // ── 波形 ──

    private val _waveformData = MutableStateFlow<List<Float>>(emptyList())
    val waveformData: StateFlow<List<Float>> = _waveformData.asStateFlow()

    // ── 内部 ──

    private var questions: List<ExamQuestion> = emptyList()
    private var timerJob: Job? = null

    val totalQuestions: Int get() = questions.size

    val progress: Float
        get() = if (totalQuestions > 0) _currentQuestionIndex.value.toFloat() / totalQuestions else 0f

    val formattedTime: String
        get() {
            val min = _remainingTime.value / 60
            val sec = _remainingTime.value % 60
            return "%d:%02d".format(min, sec)
        }

    val partAverages: List<Pair<Int, Double>>
        get() = listOf(1, 2, 3).map { part ->
            val scores = _examScores.value.filter { it.part == part }.map { it.score }
            val avg = if (scores.isEmpty()) 0.0 else scores.average()
            part to avg
        }

    init {
        loadExamQuestions()
    }

    // ── 加载题目 ──

    fun loadExamQuestions() {
        _phase.value = ExamPhase.LOADING
        _examScores.value = emptyList()
        _currentQuestionIndex.value = 0
        _overallScore.value = 0.0

        viewModelScope.launch {
            val allQuestions = mutableListOf<ExamQuestion>()

            val parts = listOf(
                Triple(1, "Part1", 3),
                Triple(2, "Part2", 1),
                Triple(3, "Part3", 2),
            )

            for ((part, section, count) in parts) {
                try {
                    val api = nestRetrofit.create(MockExamApi::class.java)
                    val resp = api.getQuestions(
                        examType = "IELTS",
                        section = section,
                        limit = count,
                    )
                    resp.data?.items?.forEach { item ->
                        allQuestions.add(
                            ExamQuestion(
                                id = item.id,
                                part = part,
                                question = item.promptText,
                                subs = emptyList(),
                                timeLimit = 180,
                            ),
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Part$part 题目加载失败", e)
                }
            }

            if (allQuestions.isEmpty()) {
                allQuestions.addAll(defaultQuestions)
            }

            questions = allQuestions
            loadCurrentQuestion()
            _phase.value = ExamPhase.READY
        }
    }

    // ── 暂停/恢复 ──

    fun togglePause() {
        _isPaused.value = !_isPaused.value
        if (_isPaused.value) {
            timerJob?.cancel()
        } else if (_isRecording.value) {
            startTimer()
        }
    }

    // ── 录音 ──

    fun startRecording() {
        if (_isPaused.value) return
        _errorMessage.value = null
        _isRecording.value = true
        _phase.value = ExamPhase.IN_PROGRESS
        startTimer()
        generateFakeWaveform()
    }

    fun stopRecording() {
        _isRecording.value = false
        _waveformData.value = emptyList()
        timerJob?.cancel()
        evaluateQuestion()
    }

    // ── 完整评测 ──

    private fun evaluateQuestion() {
        viewModelScope.launch {
            _phase.value = ExamPhase.EVALUATING
            _evaluationProgress.value = "正在识别语音..."

            try {
                val currentQ = questions[_currentQuestionIndex.value]
                _evaluationProgress.value = "AI 正在分析您的回答..."

                val api = goRetrofit.create(MockExamApi::class.java)
                val resp = api.fullEvaluate(
                    FullEvalBody(
                        sessionId = "mock_${System.currentTimeMillis()}",
                        examType = "IELTS",
                        section = "Part${_currentPart.value}",
                        questionText = _currentQuestion.value,
                        studentAudioB64 = "",
                    ),
                )

                val result = resp.data
                _currentResult.value = result

                val score = result?.overallScore ?: 0.0
                _examScores.value = _examScores.value + EnrichedQuestionScore(
                    part = _currentPart.value,
                    question = _currentQuestion.value,
                    score = score,
                    fullResult = result,
                )

                _phase.value = ExamPhase.SHOWING_RESULT
            } catch (e: Exception) {
                _examScores.value = _examScores.value + EnrichedQuestionScore(
                    part = _currentPart.value,
                    question = _currentQuestion.value,
                    score = 0.0,
                )
                _errorMessage.value = "评测失败: ${e.message}"
                _phase.value = ExamPhase.SHOWING_RESULT
            }
        }
    }

    // ── 导航 ──

    fun dismissResult() {
        _currentResult.value = null
        nextQuestion()
    }

    fun redoCurrentQuestion() {
        if (_examScores.value.isNotEmpty()) {
            _examScores.value = _examScores.value.dropLast(1)
        }
        _currentResult.value = null
        _phase.value = ExamPhase.READY
    }

    private fun nextQuestion() {
        if (_currentQuestionIndex.value >= questions.size - 1) {
            endExam()
            return
        }

        val currentPartNum = questions[_currentQuestionIndex.value].part
        val nextIndex = _currentQuestionIndex.value + 1
        val nextPartNum = questions[nextIndex].part

        if (nextPartNum != currentPartNum) {
            _transitionFromPart.value = currentPartNum
            _transitionToPart.value = nextPartNum
            _phase.value = ExamPhase.SECTION_TRANSITION
            return
        }

        advanceToNextQuestion()
    }

    fun continueAfterTransition() {
        advanceToNextQuestion()
    }

    private fun advanceToNextQuestion() {
        _currentQuestionIndex.value += 1
        loadCurrentQuestion()
        _phase.value = ExamPhase.READY
    }

    fun endExam() {
        timerJob?.cancel()
        _phase.value = ExamPhase.FINISHED

        val validScores = _examScores.value.filter { it.score > 0 }
        if (validScores.isNotEmpty()) {
            _overallScore.value = validScores.map { it.score }.average()
        }
    }

    private fun loadCurrentQuestion() {
        val idx = _currentQuestionIndex.value
        if (idx < questions.size) {
            val q = questions[idx]
            _currentPart.value = q.part
            _currentQuestion.value = q.question
            _subQuestions.value = q.subs
            _remainingTime.value = q.timeLimit
        }
    }

    // ── 计时器 ──

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_remainingTime.value > 0) {
                delay(1000)
                if (!_isPaused.value) {
                    _remainingTime.value -= 1
                }
            }
            stopRecording()
        }
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
        timerJob?.cancel()
    }

    // ── 默认题目 ──

    private val defaultQuestions = listOf(
        ExamQuestion("d1", 1, "Do you work or are you a student?", emptyList(), 180),
        ExamQuestion("d2", 1, "What do you enjoy most about your studies?", emptyList(), 180),
        ExamQuestion("d3", 2, "Describe a place you have visited that you particularly liked.",
            listOf("Where is it?", "When did you go there?", "What did you do there?", "Why did you like it?"), 180),
        ExamQuestion("d4", 3, "Do you think tourism is good for a country's economy?", emptyList(), 180),
    )
}

// ── 内部模型 ──

private data class ExamQuestion(
    val id: String,
    val part: Int,
    val question: String,
    val subs: List<String>,
    val timeLimit: Int,
)

private data class QListResponse(val items: List<QItem>? = null)
private data class QItem(
    val id: String,
    @SerializedName("prompt_text") val promptText: String,
)

private data class FullEvalBody(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("exam_type") val examType: String,
    val section: String,
    @SerializedName("question_text") val questionText: String,
    @SerializedName("student_audio_b64") val studentAudioB64: String? = null,
)

// ── Retrofit API ──

private interface MockExamApi {
    @GET(Endpoints.Questions.LIST)
    suspend fun getQuestions(
        @Query("exam_type") examType: String,
        @Query("section") section: String,
        @Query("limit") limit: Int,
    ): ApiResponse<QListResponse>

    @POST(Endpoints.Assessment.FULL_EVALUATE)
    suspend fun fullEvaluate(@Body body: FullEvalBody): ApiResponse<FullEvaluateResult>
}
