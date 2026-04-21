package com.speakpro.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
 * 6 格验证码输入 —— 隐藏 BasicTextField 承接键盘，6 个 cell 仅用于展示。
 */
@Composable
fun OTPScreen(
    phoneVM: PhoneAuthViewModel,
    primaryLabel: String = "验证并继续",
    onBack: () -> Unit,
    onResend: () -> Unit,
    onVerified: () -> Unit,
) {
    val phone by phoneVM.phone.collectAsState()
    val code by phoneVM.code.collectAsState()
    val cooldownSec by phoneVM.cooldownSec.collectAsState()
    val isVerifying by phoneVM.isVerifying.collectAsState()
    val errorMessage by phoneVM.errorMessage.collectAsState()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 28.dp),
    ) {
        // header with back
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
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack),
            )
        }

        Spacer(Modifier.height(8.dp))
        Eyebrow("STEP · 02 / 02")
        Spacer(Modifier.height(14.dp))
        Text("Enter the", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 32.sp)
        Text(
            "six digits.",
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 32.sp,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "验证码已发送至 +86 ${formatPhone(phone)}",
            color = SpMuted,
            fontSize = 13.sp,
        )

        Spacer(Modifier.height(36.dp))

        // Hidden TextField + cells
        Box(modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = code,
                onValueChange = phoneVM::onCodeChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.Transparent),
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(focusRequester)
                    .alpha(0f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (i in 0..5) {
                    CodeCell(
                        digit = code.getOrNull(i)?.toString() ?: "",
                        active = i == code.length,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("没收到？", color = SpMuted, fontSize = 12.sp)
            Spacer(Modifier.weight(1f))
            if (cooldownSec > 0) {
                Text(
                    "重新发送 · ${cooldownSec}s",
                    color = SpMuted,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 12.sp,
                )
            } else {
                Text(
                    "重新发送",
                    color = SpPrimary,
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onResend),
                )
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMessage ?: "", color = SpError, fontSize = 12.sp)
        }

        Spacer(Modifier.weight(1f))

        SpPrimaryButton(
            text = primaryLabel,
            enabled = phoneVM.isCodeValid,
            loading = isVerifying,
            onClick = { phoneVM.verifyOtp(onVerified) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CodeCell(digit: String, active: Boolean, modifier: Modifier = Modifier) {
    val filled = digit.isNotEmpty()
    val bg = if (filled) SpPrimary else SpIvory
    val border = when {
        active  -> SpAccent
        filled  -> SpPrimary
        else    -> SpLine
    }
    val borderWidth = if (active) 2.dp else 1.dp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(1f / 1.15f)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(borderWidth, border, RoundedCornerShape(10.dp)),
    ) {
        Text(
            digit,
            color = if (filled) SpIvory else SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 28.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatPhone(raw: String): String {
    if (raw.length != 11) return raw
    return "${raw.substring(0, 3)} ${raw.substring(3, 7)} ${raw.substring(7)}"
}
