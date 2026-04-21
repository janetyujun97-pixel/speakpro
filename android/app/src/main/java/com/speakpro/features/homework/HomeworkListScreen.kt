package com.speakpro.features.homework

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.data.models.HomeworkAssignment
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 作业 Tab —— editorial hero + 待完成/已完成 tabs + 行列表。
 * 对应 speakpro/components/Tabs.jsx · Homework。
 */
@Composable
fun HomeworkListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeworkListViewModel = hiltViewModel(),
) {
    val assignments by viewModel.assignments.collectAsState()
    var tab by remember { mutableStateOf("todo") }

    val todoList = assignments.filter { !it.isCompleted }
    val doneList = assignments.filter { it.isCompleted }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // hero
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Eyebrow("HOMEWORK · 作业")
                Spacer(Modifier.weight(1f))
                Text("WEEK ${currentWeekOfYear()}", color = SpMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(14.dp))
            val headline = buildAnnotatedString {
                withStyle(SpanStyle(color = SpPrimary)) { append("${todoList.size} ") }
                withStyle(SpanStyle(color = SpMuted, fontStyle = FontStyle.Italic)) { append("due,") }
            }
            Text(
                headline,
                fontFamily = FraunceFamily,
                fontSize = 36.sp,
                lineHeight = 40.sp,
            )
            val urgent = todoList.count { isUrgent(it) }
            Text(
                if (urgent > 0) "$urgent overdue soon." else "all on track.",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 36.sp,
                lineHeight = 40.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "共 ${todoList.sumOf { estimatePoints(it) }} 分 · 已完成 ${doneList.size}/${assignments.size}",
                color = SpMuted,
                fontSize = 13.sp,
            )
        }

        // tabs
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            listOf("todo" to "待完成 · ${todoList.size}", "done" to "已完成 · ${doneList.size}").forEach { (k, l) ->
                val on = tab == k
                Column(modifier = Modifier.clickable { tab = k }) {
                    Text(
                        l,
                        color = if (on) SpPrimary else SpMuted,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (on) SpPrimary else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(SpLine))

        // list
        val list = if (tab == "todo") todoList else doneList
        if (list.isEmpty()) {
            Text(
                if (tab == "todo") "太棒了，没有待完成作业" else "还没有已完成的作业",
                color = SpMuted,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 30.dp),
            )
        } else {
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                list.forEach { hw ->
                    if (tab == "todo") TodoRow(hw) { onNavigateToDetail(hw.id) }
                    else DoneRow(hw) { onNavigateToDetail(hw.id) }
                }
            }
        }
    }
}

// ============================================================================

@Composable
private fun TodoRow(hw: HomeworkAssignment, onClick: () -> Unit) {
    val typeLabel = guessType(hw)
    val urgent = isUrgent(hw)
    val accent = typeLabel == "模考"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                typeLabel,
                color = if (accent) SpAccent else SpPrimary,
                fontSize = 10.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .border(
                        1.dp,
                        if (accent) SpAccent else SpPrimary,
                        RoundedCornerShape(2.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
            if (urgent) {
                Spacer(Modifier.width(8.dp))
                Text(
                    "● URGENT",
                    color = SpAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "+${estimatePoints(hw)}",
                    color = SpMuted,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 24.sp,
                )
                Text("POINTS", color = SpMuted, fontSize = 9.sp, letterSpacing = 1.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            hw.title,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            buildAnnotatedString {
                append(hw.teacher?.name ?: "老师")
                append(" · 截止 ")
                withStyle(
                    SpanStyle(
                        color = if (urgent) SpAccent else SpPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                ) {
                    append(formatDue(hw.dueDate))
                }
                append(" · ")
                append(estimateDuration(hw))
            },
            color = SpMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(CircleShape)
                .background(SpPrimary)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
        ) {
            Text(
                "开始 $typeLabel",
                color = SpIvory,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = SpIvory,
                modifier = Modifier.size(12.dp),
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
}

@Composable
private fun DoneRow(hw: HomeworkAssignment, onClick: () -> Unit) {
    val score = hw.submissions?.firstOrNull { it.status == "graded" }?.score ?: 0.0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Text(
            formatShortDate(hw.dueDate),
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontSize = 15.sp,
            modifier = Modifier.width(48.dp),
        )
        Spacer(Modifier.width(14.dp))
        Box(Modifier.width(1.dp).height(32.dp).background(SpLine))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                guessType(hw).uppercase(),
                color = SpMuted,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                hw.title,
                color = SpPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
        Text(
            if (score > 0) "%.1f".format(score) else "—",
            color = if (score >= 6.5) SpMoss else SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 20.sp,
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
}

// ============================================================================
// 助手：模型里没有明确 type / points / duration，按规则推断

private fun guessType(hw: HomeworkAssignment): String {
    val t = hw.title.lowercase()
    return when {
        "模考" in hw.title || "mock" in t -> "模考"
        "朗读" in hw.title || "reading" in t -> "朗读"
        "跟读" in hw.title || "shadow" in t -> "跟读"
        else -> "AI 对话"
    }
}

private fun estimatePoints(hw: HomeworkAssignment): Int {
    val n = hw.questionIds?.size ?: 3
    return n * 10
}

private fun estimateDuration(hw: HomeworkAssignment): String {
    val t = guessType(hw)
    return when (t) {
        "模考" -> "11 min"
        "朗读" -> "8 min"
        "跟读" -> "10 min"
        else -> "15 min"
    }
}

private fun formatDue(iso: String?): String {
    if (iso.isNullOrEmpty()) return "—"
    return try {
        val due = Instant.parse(iso).atZone(ZoneId.systemDefault())
        val now = LocalDateTime.now().atZone(ZoneId.systemDefault())
        val hours = ChronoUnit.HOURS.between(now, due)
        when {
            hours < 0 -> "已过期"
            hours < 24 -> "明天 %02d:%02d".format(due.hour, due.minute)
            hours < 24 * 3 -> "本周日"
            else -> "%02d·%02d".format(due.monthValue, due.dayOfMonth)
        }
    } catch (_: Exception) {
        iso.take(10)
    }
}

private fun formatShortDate(iso: String?): String {
    if (iso.isNullOrEmpty()) return "—"
    return try {
        val d = Instant.parse(iso).atZone(ZoneId.systemDefault())
        "%02d·%02d".format(d.monthValue, d.dayOfMonth)
    } catch (_: Exception) {
        "—"
    }
}

private fun isUrgent(hw: HomeworkAssignment): Boolean {
    val iso = hw.dueDate ?: return false
    return try {
        val due = Instant.parse(iso)
        val hours = ChronoUnit.HOURS.between(Instant.now(), due)
        hours in 0..48
    } catch (_: Exception) {
        false
    }
}

private fun currentWeekOfYear(): Int {
    val cal = java.util.Calendar.getInstance()
    return cal.get(java.util.Calendar.WEEK_OF_YEAR)
}

@Composable
private fun Eyebrow(text: String, color: Color = SpMuted) {
    Text(
        text.uppercase(),
        color = color,
        fontFamily = InterFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.2.sp,
    )
}

