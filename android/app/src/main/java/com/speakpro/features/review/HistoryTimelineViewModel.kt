package com.speakpro.features.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.data.models.PracticeSessionListItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

/**
 * 历史时间线 VM —— 拉 /practice/sessions 后按天分组。
 * 播放控制走 [HistoryPlaybackController]（用 ExoPlayer，直接播 URL）。
 */
@HiltViewModel
class HistoryTimelineViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    data class Group(
        val dateLabel: String,
        val items: List<PracticeSessionListItem>,
    )

    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** 正在解析签名 URL 的 session id（UI 显示 loading 态） */
    private val _resolvingId = MutableStateFlow<String?>(null)
    val resolvingId: StateFlow<String?> = _resolvingId.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.getPracticeSessions()
                val list: List<PracticeSessionListItem> = resp.data ?: emptyList()
                _groups.value = groupByDay(list)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** 解析播放 URL：先看 item.audioUrl，空则调 /sessions/:id/audio */
    suspend fun resolveAudioUrl(item: PracticeSessionListItem): String? {
        if (!item.audioUrl.isNullOrEmpty()) return item.audioUrl
        _resolvingId.value = item.id
        try {
            val resp = apiService.getSessionAudio(item.id)
            return resp.data?.audioUrl
        } catch (e: Exception) {
            _errorMessage.value = e.localizedMessage
            return null
        } finally {
            _resolvingId.value = null
        }
    }

    // ========== 分组 ==========

    private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME
    private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M 月 d 日 EEEE", Locale.CHINA)

    private fun groupByDay(sessions: List<PracticeSessionListItem>): List<Group> {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val yesterday = today.minusDays(1)

        val grouped = sessions.groupBy { item ->
            parseLocalDate(item.createdAt, zone)
        }

        return grouped
            .entries
            .sortedByDescending { it.key }
            .map { (day, items) ->
                val label = when (day) {
                    today -> "今天 · " + day.format(dayFormatter)
                    yesterday -> "昨天 · " + day.format(dayFormatter)
                    else -> day.format(dayFormatter)
                }
                Group(
                    dateLabel = label,
                    items = items.sortedByDescending { it.createdAt },
                )
            }
    }

    private fun parseLocalDate(iso: String, zone: ZoneId): LocalDate {
        return try {
            Instant.parse(iso).atZone(zone).toLocalDate()
        } catch (_: Exception) {
            LocalDate.now(zone)
        }
    }
}
