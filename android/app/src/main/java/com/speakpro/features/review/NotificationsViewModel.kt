package com.speakpro.features.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.data.models.NotificationItem
import com.speakpro.data.models.NotificationPrefs
import com.speakpro.data.models.UpdatePrefsRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _items = MutableStateFlow<List<NotificationItem>>(emptyList())
    val items: StateFlow<List<NotificationItem>> = _items.asStateFlow()

    private val _unread = MutableStateFlow(0)
    val unread: StateFlow<Int> = _unread.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.getNotifications()
                _items.value = resp.data?.items ?: emptyList()
                _unread.value = resp.data?.unread ?: 0
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            try {
                apiService.markAllNotificationsRead()
                _items.value = _items.value.map { it.copy(isRead = true) }
                _unread.value = 0
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }

    fun markRead(id: String) {
        val idx = _items.value.indexOfFirst { it.id == id }
        if (idx < 0 || _items.value[idx].isRead) return
        viewModelScope.launch {
            try {
                apiService.markNotificationRead(id)
                _items.value = _items.value.mapIndexed { i, item ->
                    if (i == idx) item.copy(isRead = true) else item
                }
                _unread.value = (_unread.value - 1).coerceAtLeast(0)
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            }
        }
    }
}

@HiltViewModel
class NotificationPrefsViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _prefs = MutableStateFlow(
        NotificationPrefs(userId = "", quietStart = "22:30:00", quietEnd = "07:30:00", pushEnabled = true),
    )
    val prefs: StateFlow<NotificationPrefs> = _prefs.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun load() {
        viewModelScope.launch {
            try {
                val resp = apiService.getNotificationPrefs()
                val data = resp.data
                if (data != null) _prefs.value = data
            } catch (_: Exception) {
                // 首次没记录，用默认值
            }
        }
    }

    fun updateQuietStart(s: String) { _prefs.value = _prefs.value.copy(quietStart = s) }
    fun updateQuietEnd(s: String)   { _prefs.value = _prefs.value.copy(quietEnd = s) }
    fun updatePushEnabled(b: Boolean) { _prefs.value = _prefs.value.copy(pushEnabled = b) }

    fun save(onDone: () -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            _errorMessage.value = null
            try {
                apiService.updateNotificationPrefs(
                    UpdatePrefsRequest(
                        quietStart = _prefs.value.quietStart,
                        quietEnd = _prefs.value.quietEnd,
                        pushEnabled = _prefs.value.pushEnabled,
                    ),
                )
                onDone()
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage
            } finally {
                _isSaving.value = false
            }
        }
    }
}
