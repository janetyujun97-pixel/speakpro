package com.speakpro.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.data.models.DimensionScores
import com.speakpro.data.models.PracticeStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** 推荐练习项 */
data class RecommendedItem(
    val title: String,
    val route: String,
)

/**
 * 首页视图模型 — 拉取练习统计和待办作业
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _todayProgress = MutableStateFlow(0f)
    val todayProgress: StateFlow<Float> = _todayProgress.asStateFlow()

    private val _recommendedItems = MutableStateFlow<List<RecommendedItem>>(emptyList())
    val recommendedItems: StateFlow<List<RecommendedItem>> = _recommendedItems.asStateFlow()

    private val _pendingHomework = MutableStateFlow<List<String>>(emptyList())
    val pendingHomework: StateFlow<List<String>> = _pendingHomework.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val dailyGoal = 3 // 每日目标练习次数

    init {
        fetchHomeData()
    }

    fun fetchHomeData() {
        viewModelScope.launch {
            _isLoading.value = true
            // 并行获取统计和作业
            launch { fetchStats() }
            launch { fetchPendingHomework() }
            _isLoading.value = false
        }
    }

    // ── 练习统计 ──

    private suspend fun fetchStats() {
        try {
            val response = apiService.getPracticeStats()
            val data = response.data ?: return

            _streakDays.value = data.streakDays ?: minOf(data.recent.last7Days, 7)
            val todayCount = data.todaySessionCount ?: (data.recent.last7Days / 7)
            _todayProgress.value = minOf(todayCount.toFloat() / dailyGoal, 1f)
            _recommendedItems.value = generateRecommendations(data.dimensions)
        } catch (e: Exception) {
            // 默认推荐
            _recommendedItems.value = listOf(
                RecommendedItem("AI 对话练习", "practice/conversation"),
                RecommendedItem("跟读发音训练", "practice/followread"),
                RecommendedItem("模考模拟", "practice/mockexam"),
            )
        }
    }

    // ── 待完成作业 ──

    private suspend fun fetchPendingHomework() {
        try {
            val response = apiService.getAssignments(status = "pending")
            val pending = (response.data ?: emptyList()).filter { !it.isCompleted }
            _pendingHomework.value = pending.take(3).map { hw ->
                if (hw.dueDate != null) "${hw.title} (截止 ${hw.dueDate})" else hw.title
            }
        } catch (e: Exception) {
            // 作业加载失败，静默处理
        }
    }

    // ── 推荐逻辑 ──

    private fun generateRecommendations(dimensions: DimensionScores?): List<RecommendedItem> {
        val dim = dimensions ?: return listOf(
            RecommendedItem("AI 对话练习", "practice/conversation"),
            RecommendedItem("发音训练", "practice/followread"),
            RecommendedItem("模考练习", "practice/mockexam"),
        )

        val recs = mutableListOf<RecommendedItem>()
        if (dim.pronunciation < 70) recs.add(RecommendedItem("发音跟读训练 — 提升准确度", "practice/followread"))
        if (dim.fluency < 70) recs.add(RecommendedItem("AI 对话练习 — 提升流利度", "practice/conversation"))
        if (dim.grammar < 70) recs.add(RecommendedItem("朗读练习 — 强化语法表达", "practice/readaloud"))
        if (dim.content < 70) recs.add(RecommendedItem("模考练习 — 提升内容组织", "practice/mockexam"))
        if (recs.isEmpty()) recs.add(RecommendedItem("继续保持！挑战更高难度", "practice/conversation"))
        return recs.take(3)
    }
}
