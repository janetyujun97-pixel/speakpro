package com.speakpro.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.data.models.OnbExamType
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import com.speakpro.features.auth.SpPrimaryButton
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OnbPlanScreen(vm: OnboardingViewModel, onDone: () -> Unit) {
    val plan by vm.studyPlan.collectAsState()
    val target by vm.targetScore.collectAsState()
    val examType by vm.examType.collectAsState()
    val examDate by vm.examDate.collectAsState()
    val isFinalizing by vm.isFinalizing.collectAsState()

    val schedule = listOf(
        Triple("周一", "AI 对话", "15 min"),
        Triple("周二", "朗读 + 跟读", "12 min"),
        Triple("周三", "AI 对话", "15 min"),
        Triple("周四", "休息 · 轻听", "—"),
        Triple("周五", "朗读 + 跟读", "12 min"),
        Triple("周六", "完整模考", "15 min"),
        Triple("周日", "复盘 · 错题", "8 min"),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState()),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.size(24.dp))
            Eyebrow("YOUR PLAN · 07")
            Spacer(Modifier.size(12.dp))

            // 用 AnnotatedString 合成一段可正常换行的标题，替代 Row 里多 Text 并排的写法
            val headline = androidx.compose.ui.text.buildAnnotatedString {
                append("From ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = SpMuted, fontStyle = FontStyle.Italic))
                append("baseline")
                pop()
                append(" to ")
                pushStyle(androidx.compose.ui.text.SpanStyle(color = SpAccent, fontStyle = FontStyle.Italic))
                append(targetLabel(target, examType))
                pop()
                append(", ")
                append(weeksText(plan))
                append(".")
            }
            Text(
                text = headline,
                color = SpPrimary,
                fontFamily = FraunceFamily,
                fontSize = 30.sp,
                lineHeight = 36.sp,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                subtitleText(target, examType, examDate),
                color = SpMuted,
                fontSize = 12.sp,
            )
        }

        Spacer(Modifier.size(18.dp))
        BaselineSnapshot(modifier = Modifier.padding(horizontal = 24.dp))

        Spacer(Modifier.size(22.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Eyebrow("WEEKLY RHYTHM · 每周节奏")
            Spacer(Modifier.size(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpIvory)
                    .border(1.dp, SpLine, RoundedCornerShape(10.dp)),
            ) {
                schedule.forEachIndexed { idx, (day, act, dur) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            day,
                            color = SpMuted,
                            fontFamily = FraunceFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            modifier = Modifier.width(40.dp),
                        )
                        Spacer(Modifier.size(14.dp))
                        Box(Modifier.width(1.dp).height(20.dp).background(SpLine))
                        Spacer(Modifier.size(14.dp))
                        val isRest = act.contains("休息")
                        Text(
                            act,
                            color = if (isRest) SpMuted else SpPrimary,
                            fontSize = 13.sp,
                            fontStyle = if (isRest) FontStyle.Italic else FontStyle.Normal,
                            modifier = Modifier.weight(1f),
                        )
                        Text(dur, color = SpMuted, fontFamily = FraunceFamily, fontSize = 11.sp)
                    }
                    if (idx < schedule.size - 1) {
                        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
                    }
                }
            }
        }

        Spacer(Modifier.size(18.dp))
        ReminderCard(modifier = Modifier.padding(horizontal = 24.dp))

        Spacer(Modifier.size(24.dp))
        SpPrimaryButton(
            text = "开始练习 · Start now",
            loading = isFinalizing,
            modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
        ) {
            vm.markCompleted()
            onDone()
        }
    }
}

@Composable
private fun BaselineSnapshot(modifier: Modifier = Modifier) {
    val fields = listOf("FLUENCY", "PRON.", "GRAMMAR", "VOCAB")
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpIvory)
            .border(1.dp, SpLine, RoundedCornerShape(10.dp))
            .padding(14.dp),
    ) {
        fields.forEach { label ->
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    label,
                    color = SpMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.size(2.dp))
                Text("—", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 18.sp)
            }
        }
    }
}

@Composable
private fun ReminderCard(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, SpLine, RoundedCornerShape(10.dp))
            .padding(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow("每日提醒")
            Spacer(Modifier.size(4.dp))
            Text("19:00 · 工作日", color = SpPrimary, fontSize = 13.sp)
        }
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .clip(CircleShape)
                .background(SpAccent),
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(SpIvory)
                    .align(Alignment.CenterEnd),
            )
        }
    }
}

private fun targetLabel(target: Double?, exam: OnbExamType?): String {
    val t = target ?: return "目标"
    return if (exam == OnbExamType.TOEFL) "${t.toInt()}" else "%.1f".format(t)
}

private fun weeksText(plan: com.speakpro.data.models.StudyPlan?): String {
    return plan?.weeks?.let { "in $it weeks" } ?: "step by step"
}

private fun subtitleText(
    target: Double?,
    exam: OnbExamType?,
    examDate: java.time.LocalDate?,
): String {
    val parts = mutableListOf<String>()
    if (target != null) {
        parts += "目标：${targetLabel(target, exam)}"
    }
    if (examDate != null) {
        val f = DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA)
        parts += "考期：${examDate.format(f)}"
    }
    return parts.joinToString(" · ")
}
