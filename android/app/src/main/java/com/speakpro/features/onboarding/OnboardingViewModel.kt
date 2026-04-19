package com.speakpro.features.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.audio.AudioFileManager
import com.speakpro.core.network.ApiService
import com.speakpro.core.offline.OfflineUploadQueue
import com.speakpro.core.offline.OfflineUploadTask
import com.speakpro.data.models.BaselineRequest
import com.speakpro.data.models.OnbExamType
import com.speakpro.data.models.StudyPlan
import com.speakpro.data.models.UpdateProfileRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Onboarding 8 步共享 ViewModel。
 *
 * 每步更新后 PATCH /onboarding/profile（增量）；录音完成后 /onboarding/baseline；
 * 最后 /onboarding/finalize 让服务端生成 study_plan。
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val apiService: ApiService,
    private val offlineQueue: OfflineUploadQueue,
) : ViewModel() {

    private val _examType = MutableStateFlow<OnbExamType?>(null)
    val examType: StateFlow<OnbExamType?> = _examType.asStateFlow()

    private val _targetScore = MutableStateFlow<Double?>(null)
    val targetScore: StateFlow<Double?> = _targetScore.asStateFlow()

    private val _examDate = MutableStateFlow<LocalDate?>(null)
    val examDate: StateFlow<LocalDate?> = _examDate.asStateFlow()

    private val _selfLevel = MutableStateFlow<Int?>(null)
    val selfLevel: StateFlow<Int?> = _selfLevel.asStateFlow()

    private val _baselineSessionId = MutableStateFlow<String?>(null)
    val baselineSessionId: StateFlow<String?> = _baselineSessionId.asStateFlow()

    private val _studyPlan = MutableStateFlow<StudyPlan?>(null)
    val studyPlan: StateFlow<StudyPlan?> = _studyPlan.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _isFinalizing = MutableStateFlow(false)
    val isFinalizing: StateFlow<Boolean> = _isFinalizing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _completed = MutableStateFlow(false)
    val completed: StateFlow<Boolean> = _completed.asStateFlow()

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    // ── Setters ──

    fun setExamType(v: OnbExamType) { _examType.value = v }
    fun setTargetScore(v: Double) { _targetScore.value = v }
    fun setExamDate(v: LocalDate) { _examDate.value = v }
    fun setSelfLevel(v: Int) { _selfLevel.value = v }

    // ── API calls ──

    /** 拉当前进度；首次用户 profile 不存在时不视为错误 */
    fun hydrate() {
        viewModelScope.launch {
            try {
                val resp = apiService.getOnboardingStatus()
                if (resp.code == 0) {
                    val p = resp.data?.profile
                    if (p != null) {
                        _examType.value = OnbExamType.fromValue(p.examType)
                        _targetScore.value = p.targetScore
                        _examDate.value = p.examDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                        _selfLevel.value = p.selfLevel
                        _baselineSessionId.value = p.baselineSessionId
                        _studyPlan.value = p.studyPlan
                        _completed.value = resp.data.completed
                    }
                }
            } catch (_: Exception) {
                // 无 profile 或网络失败不阻塞
            }
        }
    }

    /** 当前累积字段 PATCH 到服务端。失败不阻塞推进。 */
    fun patchCurrent() {
        viewModelScope.launch {
            _isSyncing.value = true
            _errorMessage.value = null
            try {
                apiService.patchOnboardingProfile(
                    UpdateProfileRequest(
                        examType = _examType.value?.value,
                        targetScore = _targetScore.value,
                        examDate = _examDate.value?.format(dateFormatter),
                        selfLevel = _selfLevel.value,
                    ),
                )
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isSyncing.value = false
            }
        }
    }

    /** 录音完成后写入 baseline。失败时把 audio 入队下次重试 */
    fun submitBaseline(
        sessionId: String?,
        audioUrl: String?,
        transcript: String?,
        localAudioFile: java.io.File? = null,
    ) {
        // PR5 follow-up —— 录音文件先拷到持久缓存（30 条 LRU）
        localAudioFile?.let { AudioFileManager.cacheRecording(it) }

        viewModelScope.launch {
            _isSyncing.value = true
            var failed = false
            try {
                val resp = apiService.postBaseline(
                    BaselineRequest(sessionId = sessionId, audioUrl = audioUrl, transcript = transcript),
                )
                if (resp.code == 0) {
                    _baselineSessionId.value = resp.data?.sessionId
                } else {
                    _errorMessage.value = resp.message.ifEmpty { "基线保存失败" }
                    failed = true
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
                failed = true
            } finally {
                _isSyncing.value = false
            }

            // 失败 + 有本地音频 → 入队离线上传队列，等联网自动重试
            if (failed && audioUrl != null) {
                offlineQueue.enqueue(
                    OfflineUploadTask(
                        audioFilename = audioUrl,
                        sessionId = sessionId,
                    ),
                )
            }
        }
    }

    /** 生成 study_plan 并标记完成 */
    fun finalize() {
        viewModelScope.launch {
            _isFinalizing.value = true
            try {
                val resp = apiService.finalizeOnboarding()
                if (resp.code == 0) {
                    _studyPlan.value = resp.data?.profile?.studyPlan
                    _completed.value = resp.data?.completed == true
                } else {
                    _errorMessage.value = resp.message.ifEmpty { "计划生成失败" }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isFinalizing.value = false
            }
        }
    }

    /** 明确标记完成（UI 上 "开始练习" 按钮点完调一下以兜底） */
    fun markCompleted() { _completed.value = true }

    // ── 目标分校验 ──

    fun isTargetValid(v: Double): Boolean {
        return when (_examType.value) {
            OnbExamType.IELTS -> v in 4.0..9.0 && Math.abs(v * 2 - Math.round(v * 2)) < 1e-6
            OnbExamType.TOEFL -> v in 0.0..30.0
            else -> true
        }
    }
}
