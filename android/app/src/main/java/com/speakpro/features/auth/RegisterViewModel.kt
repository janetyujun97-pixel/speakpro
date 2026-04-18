package com.speakpro.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.core.storage.TokenManager
import com.speakpro.data.models.RegisterPhoneRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 手机号注册提交 ViewModel。
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _success = MutableStateFlow(false)
    val success: StateFlow<Boolean> = _success.asStateFlow()

    fun register(phone: String, code: String, name: String, password: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.registerWithPhone(
                    RegisterPhoneRequest(phone = phone, code = code, name = name, password = password),
                )
                if (resp.code == 0 && resp.data != null) {
                    val data = resp.data
                    TokenManager.saveTokens(data.accessToken, data.refreshToken)
                    TokenManager.saveUserInfo(
                        id = data.user.id,
                        name = data.user.name,
                        email = data.user.email ?: "",
                        role = data.user.role,
                    )
                    _success.value = true
                } else {
                    _errorMessage.value = resp.message.ifEmpty { "注册失败" }
                }
            } catch (e: retrofit2.HttpException) {
                _errorMessage.value = "服务器错误 (${e.code()})"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "注册失败"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
