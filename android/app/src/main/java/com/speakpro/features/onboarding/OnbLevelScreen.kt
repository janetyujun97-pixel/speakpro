@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.speakpro.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentSoft
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBgSoft
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import com.speakpro.features.auth.SpPrimaryButton

private data class LevelOpt(
    val letter: String,
    val level: Int,
    val band: String,
    val title: String,
    val desc: String,
    val tags: List<String>,
)

@Composable
fun OnbLevelScreen(vm: OnboardingViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val selected by vm.selfLevel.collectAsState()
    val options = listOf(
        LevelOpt("A", 1, "4.0 – 5.0", "Beginner", "能说简单句 · 想说但常卡壳", listOf("语法错多", "词汇有限")),
        LevelOpt("B", 2, "5.0 – 6.0", "Intermediate", "基本能交流 · 但流畅度不足", listOf("重复多", "发音不稳")),
        LevelOpt("C", 3, "6.0 – 7.0", "Upper", "能表达观点 · 复杂话题偶尔卡", listOf("想更地道")),
        LevelOpt("D", 4, "7.0+", "Advanced", "流畅 · 想冲 7.5 或 8.0", listOf("冲刺高分")),
    )

    Column(modifier = Modifier.fillMaxSize().background(SpBackground)) {
        StepBar(step = 4, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.size(12.dp))
            Eyebrow("SELF-ASSESSMENT · 04")
            Spacer(Modifier.size(16.dp))
            Text("Where are you", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 34.sp)
            Text(
                "right now?",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 34.sp,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "选一个最接近你当前水平的档位 —— 之后会用 30 秒基线测试校准。",
                color = SpMuted,
                fontSize = 13.sp,
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.size(22.dp))
            options.forEach { opt ->
                LevelCard(opt, selected == opt.level) { vm.setSelfLevel(opt.level) }
            }
            Spacer(Modifier.size(24.dp))
        }

        SpPrimaryButton(
            text = "继续 · 做基线测试",
            enabled = selected != null,
            modifier = Modifier.padding(24.dp),
        ) {
            vm.patchCurrent()
            onContinue()
        }
    }
}

@Composable
private fun LevelCard(opt: LevelOpt, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) SpIvory else Color.Transparent)
            .border(1.dp, if (selected) SpPrimary else SpLine, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Text(
            opt.letter,
            color = if (selected) SpAccent else SpMuted,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 28.sp,
            modifier = Modifier.width(30.dp),
        )
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(opt.title, color = SpPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    "Band ${opt.band}",
                    color = SpMuted,
                    fontFamily = FraunceFamily,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.size(4.dp))
            Text(opt.desc, color = SpMuted, fontSize = 12.sp)
            Spacer(Modifier.size(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                opt.tags.forEach { tag ->
                    Text(
                        tag,
                        color = if (selected) SpAccent else SpMuted,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) SpAccentSoft else SpBgSoft)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
        Spacer(Modifier.size(10.dp))
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (selected) SpAccent else Color.Transparent)
                .border(1.5.dp, if (selected) SpAccent else SpLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = SpIvory, modifier = Modifier.size(12.dp))
            }
        }
    }
}
