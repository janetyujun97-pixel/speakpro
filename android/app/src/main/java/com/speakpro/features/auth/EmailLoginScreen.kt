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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * 邮箱 + 密码登录（旧流程）—— 走 LoginViewModel.login() 调 /auth/login。
 * 成功后 isLoggedIn=true，由 AuthNavGraph 外层触发 onAuthenticated。
 */
@Composable
fun EmailLoginScreen(
    vm: LoginViewModel,
    onBack: () -> Unit,
    onGoForgot: () -> Unit,
) {
    val email by vm.email.collectAsState()
    val password by vm.password.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()
    var showPassword by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = SpPrimary,
                modifier = Modifier
                    .size(22.dp)
                    .clickable(onClick = onBack),
            )
            Spacer(Modifier.width(14.dp))
            Eyebrow("SPEAKPRO · EMAIL SIGN-IN")
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))

        Spacer(Modifier.height(32.dp))

        Text("Sign in with", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 32.sp)
        Text(
            "email.",
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "用已注册邮箱与密码登录 —— 老用户兼容通道。",
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(36.dp))

        Eyebrow("EMAIL · 邮箱")
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = email,
            onValueChange = vm::onEmailChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = TextStyle(
                color = SpPrimary,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 16.sp,
            ),
            decorationBox = { inner ->
                Box {
                    if (email.isEmpty()) {
                        Text(
                            "you@speakpro.app",
                            color = SpMuted.copy(alpha = 0.5f),
                            fontFamily = FraunceFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpPrimary))

        Spacer(Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("PASSWORD · 密码")
            Spacer(Modifier.weight(1f))
            Text(
                if (showPassword) "隐藏" else "显示",
                color = SpMuted,
                fontSize = 11.sp,
                fontFamily = InterFamily,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable { showPassword = !showPassword },
            )
        }
        Spacer(Modifier.height(10.dp))
        BasicTextField(
            value = password,
            onValueChange = vm::onPasswordChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation =
                if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            textStyle = TextStyle(
                color = SpPrimary,
                fontFamily = InterFamily,
                fontSize = 16.sp,
            ),
            decorationBox = { inner ->
                Box {
                    if (password.isEmpty()) {
                        Text(
                            "••••••••",
                            color = SpMuted.copy(alpha = 0.5f),
                            fontFamily = InterFamily,
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpPrimary))

        Spacer(Modifier.height(28.dp))
        SpPrimaryButton(
            text = "登录",
            enabled = vm.isFormValid() && !isLoading,
            loading = isLoading,
            onClick = vm::login,
        )

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = null,
                    tint = SpError,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(errorMessage ?: "", color = SpError, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "忘记密码",
                color = SpPrimary,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onGoForgot),
            )
        }

        Spacer(Modifier.height(30.dp))
    }
}
