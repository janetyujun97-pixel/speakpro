package com.speakpro.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import com.speakpro.core.storage.TokenManager
import com.speakpro.data.models.LoginRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录视图模型
 *
 * 管理邮箱/密码表单状态、表单校验、登录请求和 Token 持久化。
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    // ── 表单状态 ──

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(TokenManager.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // ── 表单操作 ──

    fun onEmailChange(value: String) {
        _email.value = value
        _errorMessage.value = null
    }

    fun onPasswordChange(value: String) {
        _password.value = value
        _errorMessage.value = null
    }

    /** 表单是否有效 */
    fun isFormValid(): Boolean {
        return _email.value.trim().isNotEmpty()
                && _password.value.isNotEmpty()
                && _email.value.contains("@")
    }

    // ── 登录 ──

    fun login() {
        if (!isFormValid()) {
            _errorMessage.value = "请输入有效的邮箱和密码"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val response = apiService.login(
                    LoginRequest(
                        email = _email.value.trim().lowercase(),
                        password = _password.value,
                    )
                )

                if (response.code == 0 && response.data != null) {
                    val data = response.data
                    // 保存 Token
                    TokenManager.saveTokens(data.accessToken, data.refreshToken)
                    // 保存用户信息
                    TokenManager.saveUserInfo(
                        id = data.user.id,
                        name = data.user.name,
                        email = data.user.email,
                        role = data.user.role,
                    )
                    _isLoggedIn.value = true
                } else {
                    _errorMessage.value =
                        response.message.ifEmpty { "登录失败，请重试" }
                }
            } catch (e: retrofit2.HttpException) {
                _errorMessage.value = when (e.code()) {
                    401 -> "邮箱或密码错误"
                    else -> "服务器错误 (${e.code()})"
                }
            } catch (e: java.io.IOException) {
                _errorMessage.value = "网络连接失败，请检查网络"
            } catch (e: Exception) {
                _errorMessage.value = "登录失败: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ── 登出 ──

    fun logout() {
        TokenManager.clearAll()
        _isLoggedIn.value = false
        _email.value = ""
        _password.value = ""
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
