package com.speakpro.features.onboarding

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccentWarm
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpPrimary

@Composable
fun OnbWelcomeScreen(onContinue: () -> Unit, onGoLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpPrimary)
            .padding(horizontal = 32.dp, vertical = 40.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "SpeakPro",
                color = SpIvory,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "EN / 中",
                color = SpIvory,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .border(1.dp, SpIvory.copy(alpha = 0.3f), CircleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        Text(
            "SPEAKPRO · WELCOME",
            color = SpIvory.copy(alpha = 0.55f),
            fontFamily = InterFamily,
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(24.dp))
        Text("Speak, in", color = SpIvory, fontFamily = FraunceFamily, fontSize = 52.sp)
        Text(
            "rhythm.",
            color = SpAccentWarm,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 52.sp,
            modifier = Modifier.offset(y = (-8).dp),
        )

        Spacer(Modifier.height(28.dp))
        Text(
            "AI 考官陪你练习 11 分钟的 IELTS Speaking —— 和真考一样的节奏、评分、追问。",
            color = SpIvory.copy(alpha = 0.75f),
            fontSize = 14.sp,
            modifier = Modifier.fillMaxWidth(fraction = 0.8f),
        )

        Spacer(Modifier.height(40.dp))

        ProofRow("I", "真实考官口音 · 英美可选")
        Spacer(Modifier.height(14.dp))
        ProofRow("II", "11 分钟完整模考 · 6 维度评分")
        Spacer(Modifier.height(14.dp))
        ProofRow("III", "每日 15 分钟 · 逐句纠音")

        Spacer(Modifier.weight(1f))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(CircleShape)
                .background(SpIvory)
                .clickable(onClick = onContinue),
        ) {
            Text(
                "开始 · Get started",
                color = SpPrimary,
                fontFamily = InterFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = SpPrimary)
        }

        Spacer(Modifier.height(14.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "已有账号？ 登录",
                color = SpIvory.copy(alpha = 0.55f),
                fontSize = 11.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.clickable(onClick = onGoLogin),
            )
        }
    }
}

@Composable
private fun ProofRow(numeral: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            "$numeral.",
            color = SpAccentWarm,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 16.sp,
            modifier = Modifier.width(24.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(text, color = SpIvory, fontSize = 13.sp)
    }
}
