package com.speakpro.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.speakpro.core.network.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 手机 OTP 相关流程的共享 ViewModel。
 *
 * 承担：
 *   - 手机号 / 验证码表单状态
 *   - 下发 OTP（登录 /auth/send-otp 或 重置 /auth/request-reset）
 *   - 60s 倒计时重发
 *   - 验证 OTP（不消费；register-phone / reset-password 里会再校验）
 *
 * 同一实例贯穿 Login → OTP → Register / NewPassword 三屏。
 */
@HiltViewModel
class PhoneAuthViewModel @Inject constructor(
    private val apiService: ApiService,
) : ViewModel() {

    private val phoneRegex = Regex("^1[3-9]\\d{9}$")

    // ── State ──

    private val _phone = MutableStateFlow("")
    val phone: StateFlow<String> = _phone.asStateFlow()

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code.asStateFlow()

    private val _cooldownSec = MutableStateFlow(0)
    val cooldownSec: StateFlow<Int> = _cooldownSec.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    private val _isVerifying = MutableStateFlow(false)
    val isVerifying: StateFlow<Boolean> = _isVerifying.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private var cooldownJob: Job? = null

    // ── Input ──

    fun onPhoneChange(value: String) {
        _phone.value = value.filter { it.isDigit() }.take(11)
        _errorMessage.value = null
    }

    fun onCodeChange(value: String) {
        _code.value = value.filter { it.isDigit() }.take(6)
        _errorMessage.value = null
    }

    val isPhoneValid: Boolean get() = phoneRegex.matches(_phone.value)
    val isCodeValid: Boolean get() = _code.value.length == 6

    // ── Actions ──

    /** 下发验证码（登录 / 注册共用）。onSuccess 在调用方跳转下一屏前执行。 */
    fun sendOtp(onSuccess: () -> Unit = {}) {
        if (!isPhoneValid) {
            _errorMessage.value = "请输入有效的手机号"
            return
        }
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.sendOtp(SendOtpBodyOf(_phone.value))
                if (resp.code != 0) {
                    _errorMessage.value = resp.message.ifEmpty { "发送失败" }
                } else {
                    startCooldown(resp.data?.cooldownSec ?: 60)
                    onSuccess()
                }
            } catch (e: retrofit2.HttpException) {
                _errorMessage.value = "发送失败 (${e.code()})"
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "发送失败"
            } finally {
                _isSending.value = false
            }
        }
    }

    /** 忘记密码专用：调 /auth/request-reset */
    fun sendResetOtp(onSuccess: () -> Unit = {}) {
        if (!isPhoneValid) {
            _errorMessage.value = "请输入有效的手机号"
            return
        }
        viewModelScope.launch {
            _isSending.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.requestReset(RequestResetBodyOf(_phone.value))
                if (resp.code != 0) {
                    _errorMessage.value = resp.message.ifEmpty { "发送失败" }
                } else {
                    startCooldown(resp.data?.cooldownSec ?: 60)
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "发送失败"
            } finally {
                _isSending.value = false
            }
        }
    }

    /** 验证 OTP（不消费）。成功后 onSuccess 决定下一步（register / reset）。 */
    fun verifyOtp(onSuccess: () -> Unit) {
        if (!isCodeValid) {
            _errorMessage.value = "请输入 6 位验证码"
            return
        }
        viewModelScope.launch {
            _isVerifying.value = true
            _errorMessage.value = null
            try {
                val resp = apiService.verifyOtp(VerifyOtpBodyOf(_phone.value, _code.value))
                if (resp.code != 0) {
                    _errorMessage.value = resp.message.ifEmpty { "验证失败" }
                } else {
                    onSuccess()
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "验证失败"
            } finally {
                _isVerifying.value = false
            }
        }
    }

    fun clearError() { _errorMessage.value = null }

    // ── Internal ──

    private fun startCooldown(seconds: Int) {
        cooldownJob?.cancel()
        _cooldownSec.value = seconds
        cooldownJob = viewModelScope.launch {
            while (_cooldownSec.value > 0) {
                delay(1_000)
                _cooldownSec.value -= 1
            }
        }
    }

    override fun onCleared() {
        cooldownJob?.cancel()
        super.onCleared()
    }
}

// 匿名型小帮手：避免在 Screen 层重复构造 body
private fun SendOtpBodyOf(phone: String) = com.speakpro.data.models.SendOtpRequest(phone)
private fun RequestResetBodyOf(phone: String) = com.speakpro.data.models.RequestResetRequest(phone)
private fun VerifyOtpBodyOf(phone: String, code: String) =
    com.speakpro.data.models.VerifyOtpRequest(phone, code)
