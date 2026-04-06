package com.speakpro.features.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 进度视图模型 — 占位实现，Sprint 2 完善
 */
@HiltViewModel
class ProgressViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _totalSessions = MutableStateFlow(0)
    val totalSessions: StateFlow<Int> = _totalSessions.asStateFlow()

    private val _averageScore = MutableStateFlow(0.0)
    val averageScore: StateFlow<Double> = _averageScore.asStateFlow()

    private val _streakDays = MutableStateFlow(0)
    val streakDays: StateFlow<Int> = _streakDays.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        fetchProgress()
    }

    fun fetchProgress() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getPracticeStats()
                val data = response.data
                if (data != null) {
                    _totalSessions.value = data.totalSessions
                    _averageScore.value = data.averageScore ?: 0.0
                    _streakDays.value = data.streakDays ?: 0
                }
            } catch (_: Exception) {
                // 加载失败静默处理
            } finally {
                _isLoading.value = false
            }
        }
    }
}
