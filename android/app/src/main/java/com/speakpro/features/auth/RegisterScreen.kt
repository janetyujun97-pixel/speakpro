package com.speakpro.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.clip
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
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * 手机号注册补全：姓名 + 可选密码 + 协议勾选。
 * 走 /auth/register-phone 完成注册并登录。
 */
@Composable
fun RegisterScreen(
    phoneVM: PhoneAuthViewModel,
    viewModel: RegisterViewModel,
    onBack: () -> Unit,
    onRegistered: () -> Unit,
) {
    val phone by phoneVM.phone.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val success by viewModel.success.collectAsState()

    if (success) {
        // 侧边效应：通知外层切 route
        androidx.compose.runtime.LaunchedEffect(Unit) { onRegistered() }
    }

    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreed by remember { mutableStateOf(false) }

    val canSubmit = name.trim().isNotEmpty() &&
            phoneVM.isPhoneValid &&
            phoneVM.isCodeValid &&
            agreed

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 28.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                tint = SpPrimary,
                modifier = Modifier.size(24.dp).clickable(onClick = onBack),
            )
        }

        Spacer(Modifier.height(8.dp))
        Eyebrow("CREATE ACCOUNT · 注册")
        Spacer(Modifier.height(14.dp))
        Text(
            "A rehearsal space,",
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 32.sp,
        )
        Text(
            "just for you.",
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
        )

        Spacer(Modifier.height(28.dp))

        Field(label = "NAME · 昵称") {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = SpPrimary,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                ),
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) {
                            Text(
                                "Chen Wei",
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
        }

        Spacer(Modifier.height(22.dp))

        Column {
            Eyebrow("MOBILE · 手机号")
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+86",
                    color = SpPrimary,
                    fontFamily = InterFamily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.width(1.dp).height(12.dp).background(SpLine))
                Spacer(Modifier.width(10.dp))
                Text(
                    formatPhoneReg(phone),
                    color = SpPrimary,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 16.sp,
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
        }

        Spacer(Modifier.height(22.dp))

        Field(label = "PASSWORD · 密码（可选）") {
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                textStyle = TextStyle(
                    color = SpPrimary,
                    fontFamily = InterFamily,
                    fontSize = 16.sp,
                ),
                decorationBox = { inner ->
                    Box {
                        if (password.isEmpty()) {
                            Text(
                                "至少 6 位",
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
        }

        Spacer(Modifier.weight(1f))

        if (errorMessage != null) {
            Text(errorMessage ?: "", color = SpError, fontSize = 12.sp)
            Spacer(Modifier.height(8.dp))
        }

        // 协议勾选
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { agreed = !agreed }
                .padding(vertical = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (agreed) SpAccent else androidx.compose.ui.graphics.Color.Transparent)
                    .border(
                        1.5.dp,
                        if (agreed) SpAccent else SpLine,
                        RoundedCornerShape(4.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (agreed) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = null,
                        tint = SpIvory,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "我已阅读并同意《用户协议》与《隐私政策》，理解我的录音数据会用于发音评估，不会用于训练第三方模型。",
                color = SpMuted,
                fontSize = 11.sp,
            )
        }

        Spacer(Modifier.height(16.dp))

        SpPrimaryButton(
            text = "创建账号并开始",
            enabled = canSubmit,
            loading = isLoading,
            onClick = {
                viewModel.register(
                    phone = phone,
                    code = phoneVM.code.value,
                    name = name.trim(),
                    password = password.ifEmpty { null },
                )
            },
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Field(label: String, content: @Composable () -> Unit) {
    Column {
        Eyebrow(label)
        Spacer(Modifier.height(8.dp))
        content()
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
    }
}

private fun formatPhoneReg(raw: String): String {
    if (raw.isEmpty()) return "138 0013 8000"
    if (raw.length != 11) return raw
    return "${raw.substring(0, 3)} ${raw.substring(3, 7)} ${raw.substring(7)}"
}
