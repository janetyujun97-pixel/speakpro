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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
 * Login —— 手机号优先（主路径）+ OR 分割 + Apple / WeChat / Password 三个社交入口。
 */
@Composable
fun LoginScreen(
    phoneVM: PhoneAuthViewModel,
    onRequestOtp: () -> Unit,
    onGoRegister: () -> Unit,
    onGoForgot: () -> Unit,
    onAppleSignIn: () -> Unit,
    onWechatSignIn: () -> Unit,
    onEmailLogin: () -> Unit,
) {
    val phone by phoneVM.phone.collectAsState()
    val isSending by phoneVM.isSending.collectAsState()
    val errorMessage by phoneVM.errorMessage.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 20.dp),
    ) {
        // masthead
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
        ) {
            Eyebrow("SPEAKPRO · LOG IN")
            Spacer(Modifier.weight(1f))
            Text(
                "EN",
                color = SpAccent,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(SpLine),
        )

        Spacer(Modifier.height(32.dp))

        Text("Welcome", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 36.sp)
        Text(
            "back.",
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 36.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Sign in to pick up your rehearsal\nwhere you left off.",
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 14.sp,
        )

        Spacer(Modifier.height(36.dp))

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

        Spacer(Modifier.height(24.dp))
        SpPrimaryButton(
            text = "获取验证码",
            enabled = phoneVM.isPhoneValid,
            loading = isSending,
            onClick = onRequestOtp,
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

        Spacer(Modifier.height(28.dp))
        OrDivider()

        Spacer(Modifier.height(18.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            SocialCell("Apple", Icons.Filled.Lock, modifier = Modifier.weight(1f)) {
                onAppleSignIn()
            }
            SocialCell("WeChat", Icons.Filled.Chat, modifier = Modifier.weight(1f)) {
                onWechatSignIn()
            }
            SocialCell("Password", Icons.Filled.Lock, modifier = Modifier.weight(1f)) {
                onEmailLogin()
            }
        }

        Spacer(Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "注册账号",
                color = SpPrimary,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onGoRegister),
            )
            Spacer(Modifier.width(12.dp))
            Text("·", color = SpMuted)
            Spacer(Modifier.width(12.dp))
            Text(
                "忘记密码",
                color = SpPrimary,
                fontFamily = InterFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onGoForgot),
            )
        }

        Spacer(Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "首次登录即注册 · 代表同意",
                color = SpMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
            Row {
                Text(
                    "用户协议",
                    color = SpPrimary,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                )
                Text(" · ", color = SpMuted, fontSize = 11.sp)
                Text(
                    "隐私政策",
                    color = SpPrimary,
                    fontSize = 11.sp,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }

        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun SocialCell(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SpIvory)
            .border(1.dp, SpLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Icon(icon, contentDescription = null, tint = SpPrimary, modifier = Modifier.size(18.dp))
        Text(label, color = SpMuted, fontFamily = InterFamily, fontSize = 10.sp)
    }
}
