package com.speakpro.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * 忘记密码入口：输入手机号 → 发送 reset OTP → 跳到 OTP 校验屏。
 */
@Composable
fun ForgotScreen(
    phoneVM: PhoneAuthViewModel,
    onBack: () -> Unit,
    onOtpSent: () -> Unit,
) {
    val phone by phoneVM.phone.collectAsState()
    val isSending by phoneVM.isSending.collectAsState()
    val errorMessage by phoneVM.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = SpPrimary,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
        }

        Spacer(Modifier.height(8.dp))
        Eyebrow("RECOVER · 找回密码")
        Spacer(Modifier.height(14.dp))
        Text("Reset your", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 32.sp)
        Text(
            "password.",
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "输入注册手机号，我们会发送验证码给你。",
            color = SpMuted,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(32.dp))

        Eyebrow("MOBILE · 手机号")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "+86",
                color = SpPrimary,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(10.dp))
            Box(Modifier.width(1.dp).height(14.dp).background(SpLine))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = phone,
                onValueChange = phoneVM::onPhoneChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true,
                textStyle = TextStyle(
                    color = SpPrimary,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                ),
                decorationBox = { inner ->
                    Box {
                        if (phone.isEmpty()) {
                            Text(
                                "138 0013 8000",
                                color = SpMuted.copy(alpha = 0.5f),
                                fontFamily = FraunceFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 16.sp,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpPrimary))

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage ?: "", color = SpError, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))

        SpPrimaryButton(
            text = "获取验证码",
            enabled = phoneVM.isPhoneValid,
            loading = isSending,
            onClick = { phoneVM.sendResetOtp { onOtpSent() } },
        )
        Spacer(Modifier.height(24.dp))
    }
}

/**
 * 新密码 / 确认密码 —— OTP 校验成功后跳这里。
 */
@Composable
fun NewPasswordScreen(
    phoneVM: PhoneAuthViewModel,
    viewModel: ResetPasswordViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val success by viewModel.success.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    if (success) {
        LaunchedEffect(Unit) { onSuccess() }
    }

    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    val matched = newPassword == confirmPassword && newPassword.isNotEmpty()
    val canSubmit = newPassword.length >= 6 &&
            newPassword.any { it.isLetter() } &&
            newPassword.any { it.isDigit() } &&
            matched

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = SpPrimary,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
        }

        Spacer(Modifier.height(8.dp))
        Eyebrow("RECOVER · 找回密码")
        Spacer(Modifier.height(14.dp))
        Text("Set a new", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 32.sp)
        Text(
            "password.",
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "为安全考虑，请设置不同于过往 3 次的密码。\n至少 6 位 · 字母 + 数字 · 推荐加符号。",
            color = SpMuted,
            fontSize = 12.sp,
        )

        Spacer(Modifier.height(30.dp))

        Eyebrow("NEW PASSWORD · 新密码")
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            textStyle = TextStyle(color = SpPrimary, fontFamily = InterFamily, fontSize = 16.sp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpPrimary))

        Spacer(Modifier.height(12.dp))

        RulesList(newPassword)

        Spacer(Modifier.height(22.dp))

        Eyebrow("CONFIRM · 确认密码")
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = TextStyle(color = SpPrimary, fontFamily = InterFamily, fontSize = 16.sp),
                modifier = Modifier.weight(1f),
            )
            if (confirmPassword.isNotEmpty()) {
                Text(
                    if (matched) "● MATCH" else "● MISMATCH",
                    color = if (matched) SpMoss else SpError,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage ?: "", color = SpError, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))

        SpPrimaryButton(
            text = "更新并重新登录",
            enabled = canSubmit,
            loading = isLoading,
            onClick = {
                viewModel.resetPassword(
                    phone = phoneVM.phone.value,
                    code = phoneVM.code.value,
                    newPassword = newPassword,
                )
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun RulesList(pwd: String) {
    val rules = listOf(
        "至少 6 位" to (pwd.length >= 6),
        "含字母" to pwd.any { it.isLetter() },
        "含数字" to pwd.any { it.isDigit() },
        "含符号（推荐）" to pwd.any { !it.isLetterOrDigit() && !it.isWhitespace() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rules.forEach { (label, ok) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ok) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = SpMoss,
                        modifier = Modifier.size(12.dp),
                    )
                } else {
                    Box(Modifier.size(12.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(label, color = if (ok) SpMoss else SpMuted, fontSize = 11.sp)
            }
        }
    }
}
