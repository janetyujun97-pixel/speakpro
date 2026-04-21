package com.speakpro.data.models

// Auth v2 —— 手机 OTP / Apple / WeChat + 邮箱 reset。字段命名一律 camelCase，
// 与 NestJS 实际输出一致（后端不做 key 转换）。

// ── 统一 token + user 响应（login / register-phone / apple / wechat） ──

data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserInfoV2,
)

/**
 * 扩展版 UserInfo —— email / phone / avatarUrl 均可空，兼容三方登录。
 * 保留独立类型避免影响已使用旧 [UserInfo] 的代码。
 */
data class UserInfoV2(
    val id: String,
    val name: String,
    val email: String? = null,
    val phone: String? = null,
    val role: String,
    val avatarUrl: String? = null,
)

// ── OTP ──

data class SendOtpRequest(val phone: String)
data class SendOtpResponse(val cooldownSec: Int)

data class VerifyOtpRequest(val phone: String, val code: String)
data class VerifyOtpResponse(val ok: Boolean)

data class RegisterPhoneRequest(
    val phone: String,
    val code: String,
    val name: String,
    val password: String? = null,
)

data class RequestResetRequest(val phone: String)

data class ResetPasswordRequest(
    val phone: String,
    val code: String,
    val newPassword: String,
)
data class ResetPasswordResponse(val ok: Boolean)

// ── Apple / WeChat ──

data class AppleSignInRequest(
    val identityToken: String,
    val authorizationCode: String? = null,
    val nonce: String? = null,
    val name: String? = null,
)

data class WechatSignInRequest(val code: String)
