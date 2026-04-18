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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.data.models.OnbExamType
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentWarm
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import com.speakpro.features.auth.SpPrimaryButton

private data class ExamOpt(
    val t: OnbExamType,
    val display: String,
    val en: String,
    val sub: String,
    val badge: String?,
)

@Composable
fun OnbExamTypeScreen(vm: OnboardingViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val selected by vm.examType.collectAsState()

    val options = listOf(
        ExamOpt(OnbExamType.IELTS, "IELTS",
            "International English Language Testing", "雅思口语 · Part 1 / 2 / 3", "最常选"),
        ExamOpt(OnbExamType.TOEFL, "TOEFL",
            "Test of English as a Foreign Language", "托福口语 · 4 个独立 / 综合任务", null),
        ExamOpt(OnbExamType.GENERAL, "GENERAL",
            "Daily conversation · business", "日常英语 · 面试 · 商务", null),
    )

    Column(modifier = Modifier.fillMaxSize().background(SpBackground)) {
        StepBar(step = 1, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.size(12.dp))
            Eyebrow("SELECT YOUR EXAM · 01")
            Spacer(Modifier.size(16.dp))
            Text("What are you", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 34.sp)
            Text(
                "training for?",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 34.sp,
            )
            Spacer(Modifier.size(10.dp))
            Text(
                "选择你要准备的考试类型 —— 我们会按这个做题目、评分和计划。",
                color = SpMuted,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.size(28.dp))

        Column(
            modifier = Modifier.padding(horizontal = 24.dp).weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            options.forEach { opt ->
                ExamCard(opt, selected == opt.t) { vm.setExamType(opt.t) }
            }
        }

        SpPrimaryButton(
            text = "继续",
            enabled = selected != null,
            modifier = Modifier.padding(24.dp),
        ) {
            vm.patchCurrent()
            onContinue()
        }
    }
}

@Composable
private fun ExamCard(opt: ExamOpt, selected: Boolean, onClick: () -> Unit) {
    val bg: Color = if (selected) SpPrimary else SpIvory
    val fg: Color = if (selected) SpIvory else SpPrimary
    val subFg: Color = if (selected) SpIvory.copy(alpha = 0.8f) else SpMuted
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, if (selected) SpPrimary else SpLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(opt.display, color = fg, fontFamily = FraunceFamily, fontSize = 22.sp)
                if (opt.badge != null) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        opt.badge,
                        color = SpIvory,
                        fontSize = 9.sp,
                        modifier = Modifier
                            .background(SpAccentWarm)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.size(4.dp))
            Text(
                opt.en,
                color = if (selected) SpIvory.copy(alpha = 0.55f) else SpMuted,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
            )
            Spacer(Modifier.size(10.dp))
            Text(opt.sub, color = subFg, fontSize = 12.sp)
        }
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) SpIvory else Color.Transparent)
                .border(1.5.dp, if (selected) SpIvory else SpLine, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = SpPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
