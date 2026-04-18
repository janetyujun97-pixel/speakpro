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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow

@Composable
fun OnbBaselineIntroScreen(onBack: () -> Unit, onStart: () -> Unit) {
    val prompt = "Tell me about a place you recently visited and enjoyed."

    Column(modifier = Modifier.fillMaxSize().background(SpBackground)) {
        StepBar(step = 5, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.size(12.dp))
            Eyebrow("BASELINE · 05")
            Spacer(Modifier.size(16.dp))
            Text("Let's hear you", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 36.sp)
            Text(
                "speak for 30s.",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 36.sp,
            )
            Spacer(Modifier.size(14.dp))
            Text(
                "用 30 秒回答一个简单问题 —— 我们会快速分析你现在的 6 个维度，制定适合的计划。",
                color = SpMuted,
                fontSize = 14.sp,
            )
        }

        Spacer(Modifier.size(28.dp))

        Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpIvory)
                    .border(1.dp, SpLine, RoundedCornerShape(12.dp))
                    .padding(24.dp),
            ) {
                Column {
                    Text(
                        "YOUR PROMPT",
                        color = SpAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )
                    Spacer(Modifier.size(12.dp))
                    Text(
                        prompt,
                        color = SpPrimary,
                        fontFamily = FraunceFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 22.sp,
                    )
                    Spacer(Modifier.size(14.dp))
                    Text(
                        "— IELTS Part 1 style · 一道 common 问题",
                        color = SpMuted,
                        fontSize = 11.sp,
                    )
                }
            }

            Spacer(Modifier.size(28.dp))

            Eyebrow("WE'LL ANALYZE")
            Spacer(Modifier.size(10.dp))

            val rows = listOf(
                "I" to "Fluency · 流畅度",
                "II" to "Pronunciation · 发音",
                "III" to "Grammar · 语法",
                "IV" to "Vocabulary · 词汇",
            )
            rows.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    pair.forEach { (n, t) ->
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                "$n.",
                                color = SpAccent,
                                fontFamily = FraunceFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = 13.sp,
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(t, color = SpPrimary, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(SpAccent)
                    .clickable(onClick = onStart),
            ) {
                Icon(
                    Icons.Filled.Mic,
                    contentDescription = null,
                    tint = SpIvory,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    "开始录音 · 30 秒",
                    color = SpIvory,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.size(10.dp))
            Text(
                "不紧张 · 说错也没关系",
                color = SpMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
