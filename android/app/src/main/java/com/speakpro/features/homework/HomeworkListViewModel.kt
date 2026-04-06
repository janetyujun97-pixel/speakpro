package com.speakpro.features.homework

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.data.models.HomeworkAssignment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 作业列表视图模型
 */
@HiltViewModel
class HomeworkListViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _assignments = MutableStateFlow<List<HomeworkAssignment>>(emptyList())
    val assignments: StateFlow<List<HomeworkAssignment>> = _assignments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun fetchAssignments() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = apiService.getAssignments()
                _assignments.value = response.data ?: emptyList()
            } catch (_: Exception) {
                // 加载失败静默处理
            } finally {
                _isLoading.value = false
            }
        }
    }
}
