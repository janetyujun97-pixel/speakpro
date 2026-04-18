package com.speakpro.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.data.models.ResetPasswordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 手机 OTP + 新密码 重置 ViewModel。
 */
@HiltViewModel
class ResetPasswordViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun resetPassword(phone: String, code: String, newPassword: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.resetPassword(
                    ResetPasswordRequest(
                        phone = phone,
                        code = code,
                        newPassword = newPassword,
                    ),
                )
                if (resp.code == 0) {
                    _success.value = true
                } else {
                    _errorMessage.value = resp.message.ifEmpty { "重置失败" }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "重置失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
