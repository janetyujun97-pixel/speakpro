package com.speakpro.features.progress

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import kotlin.math.abs
import kotlin.math.sin

/**
 * 进度 Tab —— editorial 风格。
 * 对应 speakpro/components/Tabs.jsx · Progress。
 * Review 入口（历史回听/错题本/通知中心）保留在底部。
 */
@Composable
fun ProgressScreen(
    viewModel: ProgressViewModel = hiltViewModel(),
    onOpenHistory: () -> Unit = {},
    onOpenNotebook: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
) {
    val totalSessions by viewModel.totalSessions.collectAsState()
    val averageScore by viewModel.averageScore.collectAsState()
    val streakDays by viewModel.streakDays.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        Hero(startScore = 5.0, endScore = averageScore.coerceAtLeast(0.0))
        Spacer(Modifier.height(4.dp))

        // 3 个顶部 stat 卡
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
        ) {
            StatCard("练习次数", totalSessions.toString(), "THIS MONTH", Modifier.weight(1f))
            StatCard(
                "平均分",
                if (averageScore > 0) "%.1f".format(averageScore) else "—",
                "↑ +0.6",
                Modifier.weight(1f),
            )
            StatCard("连续天数", streakDays.toString(), "DAYS", Modifier.weight(1f))
        }

        Spacer(Modifier.height(22.dp))
        TrendChartSection()

        Spacer(Modifier.height(22.dp))
        SixDimensionSection()

        Spacer(Modifier.height(22.dp))
        ActivityLedger()

        Spacer(Modifier.height(22.dp))
        NextMilestone()

        // ================= Review 入口（PR3c）=================
        Spacer(Modifier.height(26.dp))
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Eyebrow("REVIEW · 回顾")
            Spacer(Modifier.height(10.dp))
            ReviewRow("历史回听", "按天查看练习记录并回听", onOpenHistory)
            ReviewRow("错题本 / 生词本", "低分单词 + 间隔复习", onOpenNotebook)
            ReviewRow("通知中心", "作业 / 批改 / 提醒", onOpenNotifications)
        }
    }
}

// ============================================================================

@Composable
private fun Hero(startScore: Double, endScore: Double) {
    val from = "%.1f".format(startScore)
    val to = if (endScore > 0) "%.1f".format(endScore) else "7.0"

    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 22.dp)) {
        Eyebrow("PROGRESS · 学习进度")
        Spacer(Modifier.height(14.dp))
        val headline = buildAnnotatedString {
            withStyle(SpanStyle(color = SpMuted, fontStyle = FontStyle.Italic)) {
                append("From $from to\n")
            }
            withStyle(SpanStyle(color = SpAccent)) {
                append("$to — in 3 weeks.")
            }
        }
        Text(
            headline,
            fontFamily = FraunceFamily,
            fontSize = 36.sp,
            lineHeight = 40.sp,
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, sub: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SpIvory)
            .border(1.dp, SpLine, RoundedCornerShape(10.dp))
            .padding(14.dp),
    ) {
        Text(
            label.uppercase(),
            color = SpMuted,
            fontSize = 10.sp,
            letterSpacing = 1.5.sp,
            fontFamily = InterFamily,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 26.sp,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            sub,
            color = SpMoss,
            fontSize = 10.sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
private fun TrendChartSection() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Eyebrow("14-DAY BAND TREND")
            Spacer(Modifier.weight(1f))
            Text("FIG. 02", color = SpMuted, fontSize = 11.sp)
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SpIvory)
                .border(1.dp, SpLine, RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp),
        ) {
            TrendChart()
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Apr 5", color = SpMuted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text("Apr 12", color = SpMuted, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text("Today", color = SpMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun TrendChart() {
    val series = listOf(5.2, 5.5, 5.5, 6.0, 5.8, 6.0, 6.2, 6.0, 6.5, 6.0, 6.5, 6.8, 6.5, 7.0)
    val yMin = 4.0
    val yMax = 9.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
    ) {
        val w = size.width
        val h = size.height - 8f  // 留点 padding 给最顶上的点
        // grid lines
        for (f in listOf(0f, 0.33f, 0.66f, 1f)) {
            drawLine(
                color = SpLine,
                start = Offset(0f, h * f + 4f),
                end = Offset(w, h * f + 4f),
                strokeWidth = 1f,
            )
        }
        val points = series.mapIndexed { i, v ->
            val x = i.toFloat() / (series.size - 1) * w
            val y = h - ((v - yMin) / (yMax - yMin) * h).toFloat() + 4f
            Offset(x, y)
        }
        // filled area
        val area = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
            lineTo(w, h + 4f)
            lineTo(0f, h + 4f)
            close()
        }
        drawPath(area, SpAccent.copy(alpha = 0.1f))

        // line
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        drawPath(
            line,
            SpAccent,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // today dot with ring
        val last = points.last()
        drawCircle(SpAccent.copy(alpha = 0.2f), radius = 10.dp.toPx(), center = last)
        drawCircle(SpAccent, radius = 5.dp.toPx(), center = last)
    }
}

@Composable
private fun SixDimensionSection() {
    val dims = listOf(
        "Fluency" to 7.0, "Lexical" to 6.5, "Grammar" to 6.0,
        "Pronunciation" to 7.0, "Coherence" to 5.5, "Response" to 6.5,
    )
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Eyebrow("6 维度当前水平")
        Spacer(Modifier.height(12.dp))
        dims.forEachIndexed { i, (name, v) ->
            Column(modifier = Modifier.padding(vertical = 12.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.weight(1f)) {
                        Text(
                            "%02d".format(i + 1),
                            color = SpMuted,
                            fontFamily = FraunceFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            modifier = Modifier.width(28.dp),
                        )
                        Text(name, color = SpPrimary, fontSize = 13.sp)
                    }
                    Text(
                        "%.1f".format(v),
                        color = SpPrimary,
                        fontFamily = FraunceFamily,
                        fontSize = 16.sp,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).background(SpLine)) {
                    Box(
                        Modifier
                            .fillMaxWidth(fraction = (v / 9.0).toFloat().coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(
                                when {
                                    v < 6 -> SpAccent
                                    v >= 6.5 -> SpMoss
                                    else -> SpPrimary
                                },
                            ),
                    )
                }
            }
            if (i < dims.size - 1) Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
        }
    }
}

@Composable
private fun ActivityLedger() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Eyebrow("本月活动")
        Spacer(Modifier.height(10.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SpIvory)
                .border(1.dp, SpLine, RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            // 4 行 × 7 列 heatmap
            for (row in 0 until 4) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (col in 0 until 7) {
                        val i = row * 7 + col
                        val intensity = listOf(0f, 0.15f, 0.4f, 0.7f, 1f, 0.85f, 0.55f)[
                            (abs(sin(i * 1.3)) * 7).toInt().coerceAtMost(6),
                        ]
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(2.dp))
                                .background(
                                    if (intensity == 0f) SpLine
                                    else SpAccent.copy(alpha = intensity),
                                ),
                        )
                    }
                }
                if (row < 3) Spacer(Modifier.height(4.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row {
                Text("APR 1", color = SpMuted, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Text("TODAY", color = SpMuted, fontSize = 10.sp, letterSpacing = 1.sp)
            }
        }
    }
}

@Composable
private fun NextMilestone() {
    Column(modifier = Modifier.padding(horizontal = 24.dp)) {
        Eyebrow("下个里程碑")
        Spacer(Modifier.height(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SpPrimary)
                .padding(16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "BAND 7.0",
                    color = SpIvory.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "距目标还有 0.5",
                    color = SpIvory,
                    fontFamily = FraunceFamily,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "按当前节奏 · 约 18 天后达成",
                    color = SpIvory.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SpAccent),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    tint = SpIvory,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SpPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = SpMuted, fontSize = 11.sp)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            null,
            tint = SpMuted,
            modifier = Modifier.size(14.dp),
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
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
