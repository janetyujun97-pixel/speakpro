package com.speakpro.features.home

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
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentSoft
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 首页 —— editorial 杂志风格。
 * 结构对应 speakpro/components/HomeEditorial.jsx：masthead → 大标题 → 数据 strip →
 * 模考 hero 卡 → 其他练习模块列表 → 今日推荐 → 待完成作业 → 引语页脚。
 */
@Composable
fun HomeScreen(
    onNavigateToPractice: (String) -> Unit,
    onNavigateToHomework: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val streakDays by viewModel.streakDays.collectAsState()
    val todayProgress by viewModel.todayProgress.collectAsState()
    val pendingHomework by viewModel.pendingHomework.collectAsState()

    // 风格偏好（dev 彩蛋切换）—— 连点 No.XXX 3 下开 sheet
    val style = HomeStylePreference.current.value
    var showStyleSheet by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    // Minimal / Dashboard 的占位：UI 未实现时退回 Editorial Full 渲染
    val effectiveStyle = if (style.available) style else HomeStyle.EDITORIAL_FULL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
    ) {
        Masthead(
            streakDays = streakDays,
            onEditionTripleTap = { showStyleSheet = true },
        )
        Spacer(Modifier.height(8.dp))

        // 顶部提示：如果选的是未实现的风格，提示一下回退
        if (!style.available) {
            UnavailableHint(style)
        }

        HeroHeadline()
        StatsStrip(
            streakDays = streakDays,
            todayPercent = (todayProgress * 100).toInt(),
            targetScore = 7.0,
        )
        Spacer(Modifier.height(32.dp))
        MockExamFeature(
            variant = effectiveStyle,
            onClick = { onNavigateToPractice("practice/mockexam") },
        )
        Spacer(Modifier.height(32.dp))
        PracticeList(onNavigate = onNavigateToPractice)
        Spacer(Modifier.height(36.dp))
        TodaysRecommendation(
            onClick = { onNavigateToPractice("practice/conversation") },
        )
        Spacer(Modifier.height(32.dp))
        AssignmentsSection(
            pending = pendingHomework,
            onClick = onNavigateToHomework,
        )
        Spacer(Modifier.height(40.dp))
        QuoteFooter()
    }

    if (showStyleSheet) {
        HomeStyleSheet(
            current = style,
            onPick = {
                HomeStylePreference.set(it)
                showStyleSheet = false
            },
            onDismiss = { showStyleSheet = false },
        )
    }
}

// ============================================================================
// Masthead —— SPEAKPRO · No. XXX  +  flame 图标
// ============================================================================

@Composable
private fun Masthead(streakDays: Int, onEditionTripleTap: () -> Unit) {
    // 连点计数：2 秒内连续点 3 下 No.XXX 触发切换 sheet
    var tapCount by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableIntStateOf(0)
    }
    var lastTap by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableLongStateOf(0L)
    }
    val tripleTap = {
        val now = System.currentTimeMillis()
        if (now - lastTap > 1500) tapCount = 0
        tapCount += 1
        lastTap = now
        if (tapCount >= 3) {
            tapCount = 0
            onEditionTripleTap()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 14.dp),
    ) {
        Text(
            "SPEAKPRO · NO. ${editionNumber()}",
            color = SpMuted,
            fontFamily = InterFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
            modifier = Modifier.clickable(
                interactionSource = androidx.compose.runtime.remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                },
                indication = null,
                onClick = tripleTap,
            ),
        )
        Spacer(Modifier.weight(1f))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(SpAccentSoft)
                .border(1.dp, SpAccent.copy(alpha = 0.13f), CircleShape),
        ) {
            Icon(
                Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = SpAccent,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

private fun editionNumber(): String {
    // 简单映射：用当年天数作刊号（稳定且随日期变化）
    val day = LocalDate.now().dayOfYear
    return day.toString().padStart(3, '0')
}

// ============================================================================
// Hero headline —— "你好，同学。今日宜 开口。" + 日期副标题
// ============================================================================

@Composable
private fun HeroHeadline() {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 16.dp)) {
        val headline = buildAnnotatedString {
            append("你好，同学。\n")
            withStyle(SpanStyle(color = SpMuted, fontStyle = FontStyle.Italic)) {
                append("今日宜")
            }
            withStyle(SpanStyle(color = SpAccent, fontStyle = FontStyle.Italic)) {
                append(" 开口。")
            }
        }
        Text(
            headline,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            subtitleLine(),
            color = SpMuted,
            fontSize = 13.sp,
            letterSpacing = 0.3.sp,
        )
    }
}

private fun subtitleLine(): String {
    val now = LocalDate.now()
    val enDate = now.format(DateTimeFormatter.ofPattern("EEEE · MMMM d", Locale.ENGLISH))
    // 简化：考期放 1 个月后（真实数据可从 onboarding profile 读）
    val exam = now.plusMonths(1)
    val daysLeft = ChronoUnit.DAYS.between(now, exam).coerceAtLeast(0)
    return "$enDate  ·  距考试 ${daysLeft} 天"
}

// ============================================================================
// Stats strip —— 连续 / 今日 / 目标分
// ============================================================================

@Composable
private fun StatsStrip(streakDays: Int, todayPercent: Int, targetScore: Double) {
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 26.dp)
            .border(
                width = 0.dp,
                color = Color.Transparent,
                shape = RoundedCornerShape(0.dp),
            ),
    ) {
        StatColumn("连续", streakDays.toString(), "天", SpPrimary)
        StatColumn("今日", todayPercent.toString(), "%", SpPrimary)
        StatColumn("目标分", "%.1f".format(targetScore), null, SpAccent)
    }
    // 上下分隔线
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 18.dp)
            .height(1.dp)
            .background(SpLine),
    )
}

@Composable
private fun StatColumn(label: String, value: String, unit: String?, color: Color) {
    Column {
        Eyebrow(label)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                color = color,
                fontFamily = FraunceFamily,
                fontSize = 44.sp,
                lineHeight = 44.sp,
                letterSpacing = (-1).sp,
            )
            if (unit != null) {
                Spacer(Modifier.width(6.dp))
                Text(unit, color = SpMuted, fontSize = 13.sp)
            }
        }
    }
}

// ============================================================================
// Mock Exam 大卡 —— 深墨底 + accent italic 副标题
// ============================================================================

@Composable
private fun MockExamFeature(variant: HomeStyle, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 32.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
        ) {
            Eyebrow("专题 · FEATURED")
            Spacer(Modifier.weight(1f))
            Text(
                when (variant) {
                    HomeStyle.EDITORIAL_TICKET -> "TICKET"
                    HomeStyle.EDITORIAL_DIAGRAM -> "FIG. 01"
                    else -> "01 / 04"
                },
                color = SpMuted, fontSize = 11.sp, letterSpacing = 1.sp,
            )
        }

        when (variant) {
            HomeStyle.EDITORIAL_TICKET -> MockTicket(onClick = onClick)
            HomeStyle.EDITORIAL_DIAGRAM -> MockDiagram(onClick = onClick)
            else -> MockFull(onClick = onClick)
        }
    }
}

@Composable
private fun MockFull(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(SpPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 22.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Eyebrow("模考 · MOCK EXAM", SpIvory.copy(alpha = 0.55f))
            Spacer(Modifier.weight(1f))
            Text(
                "NEW",
                color = SpIvory.copy(alpha = 0.55f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
        }

        Spacer(Modifier.height(18.dp))
        val title = buildAnnotatedString {
            append("完整模考\n")
            withStyle(SpanStyle(color = SpAccent, fontStyle = FontStyle.Italic)) {
                append("多维诊断报告")
            }
        }
        Text(
            title,
            color = SpIvory,
            fontFamily = FraunceFamily,
            fontSize = 30.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            "Part 1 · 2 · 3 全真流程 · 11 分钟\n完成后自动生成 6 维度评分与改进建议",
            color = SpIvory.copy(alpha = 0.65f),
            fontSize = 13.sp,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(20.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpIvory.copy(alpha = 0.15f)))
        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            DarkStat("已完成", "8", false)
            Spacer(Modifier.width(20.dp))
            DarkStat("最高分", "6.5", true)
            Spacer(Modifier.width(20.dp))
            DarkStat("平均", "5.8", false)
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SpIvory)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Text(
                    "开始模考",
                    color = SpPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SpPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** 票根样式：左侧 sienna 色块（大号 `11 minutes` numerals）+ 右侧正文 */
@Composable
private fun MockTicket(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, SpLine, RoundedCornerShape(6.dp))
            .background(SpIvory)
            .clickable(onClick = onClick),
    ) {
        // 左票根
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .width(96.dp)
                .background(SpAccent)
                .padding(horizontal = 16.dp, vertical = 20.dp),
        ) {
            Eyebrow("MOCK", SpIvory.copy(alpha = 0.7f))
            Column {
                Text(
                    "11",
                    color = SpIvory,
                    fontFamily = FraunceFamily,
                    fontSize = 52.sp,
                    lineHeight = 48.sp,
                    letterSpacing = (-1).sp,
                )
                Spacer(Modifier.height(2.dp))
                Text("minutes", color = SpIvory.copy(alpha = 0.8f), fontSize = 11.sp)
            }
        }
        // 右正文
        Column(
            modifier = Modifier.weight(1f).padding(start = 22.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
        ) {
            Eyebrow("完整模考 · MOCK EXAM")
            Spacer(Modifier.height(8.dp))
            Text(
                "IELTS Speaking\nFull Test",
                color = SpPrimary,
                fontFamily = FraunceFamily,
                fontSize = 22.sp,
                lineHeight = 25.sp,
                letterSpacing = (-0.3).sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Part 1 · 2 · 3 / 6 维度评分报告",
                color = SpMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "NEXT AVAILABLE · NOW",
                    color = SpMuted,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "入场",
                    color = SpAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SpAccent,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
    }
}

/** 示意图样式：P1 / P2 / P3 三档 schematic + 底部 Begin 按钮 */
@Composable
private fun MockDiagram(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, SpLine, RoundedCornerShape(6.dp))
            .background(SpIvory)
            .padding(22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Eyebrow("模考 · MOCK EXAM")
            Spacer(Modifier.weight(1f))
            Text("FIG. 01", color = SpMuted, fontSize = 10.sp, letterSpacing = 2.sp)
        }
        Spacer(Modifier.height(14.dp))
        val title = buildAnnotatedString {
            append("11-minute full mock\n")
            withStyle(SpanStyle(color = SpAccent, fontStyle = FontStyle.Italic)) {
                append("with 6-axis report.")
            }
        }
        Text(
            title,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 26.sp,
            lineHeight = 30.sp,
        )

        Spacer(Modifier.height(22.dp))
        // P1 / P2 / P3 schematic
        Row(verticalAlignment = Alignment.CenterVertically) {
            DiagramStage("P1", "4–5 min", weight = 1f, highlighted = false)
            Box(Modifier.width(8.dp).height(1.dp).background(SpLine))
            DiagramStage("P2", "3–4 min", weight = 1.6f, highlighted = true)
            Box(Modifier.width(8.dp).height(1.dp).background(SpLine))
            DiagramStage("P3", "4–5 min", weight = 1f, highlighted = false)
        }

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                buildAnnotatedString {
                    append("上次 ")
                    withStyle(SpanStyle(color = SpPrimary, fontWeight = FontWeight.SemiBold)) {
                        append("6.0")
                    }
                    append(" · 提升空间 1.0")
                },
                color = SpMuted,
                fontSize = 12.sp,
            )
            Spacer(Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SpPrimary)
                    .clickable(onClick = onClick)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            ) {
                Text(
                    "Begin",
                    color = SpIvory,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SpIvory,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.DiagramStage(
    label: String, duration: String, weight: Float, highlighted: Boolean,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(weight)
            .clip(RoundedCornerShape(3.dp))
            .background(if (highlighted) SpAccentSoft else Color.Transparent)
            .border(1.dp, SpLine, RoundedCornerShape(3.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
    ) {
        Text(
            label,
            color = if (highlighted) SpAccent else SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(duration, color = SpMuted, fontSize = 10.sp)
    }
}

@Composable
private fun DarkStat(label: String, value: String, accent: Boolean) {
    Column {
        Text(
            label,
            color = SpIvory.copy(alpha = 0.55f),
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = if (accent) SpAccent else SpIvory,
            fontFamily = FraunceFamily,
            fontSize = 20.sp,
        )
    }
}

// ============================================================================
// Practice list —— 02 / 03 / 04 带编号的列表行
// ============================================================================

@Composable
private fun PracticeList(onNavigate: (String) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 32.dp)) {
        Eyebrow("其他练习模块")
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))

        PracticeRow("02", "AI 对话", "Dialogue with AI Examiner",
            "模拟真实口试，实时评分与追问", Icons.Filled.Mic) {
            onNavigate("practice/conversation")
        }
        PracticeRow("03", "朗读", "Reading Aloud",
            "评测发音、流利度与语调", Icons.AutoMirrored.Filled.MenuBook) {
            onNavigate("practice/readaloud")
        }
        PracticeRow("04", "跟读", "Shadowing",
            "对照标准发音逐句纠正", Icons.Filled.GraphicEq) {
            onNavigate("practice/followread")
        }
    }
}

@Composable
private fun PracticeRow(
    num: String,
    title: String,
    en: String,
    desc: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
    ) {
        Text(
            num,
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontSize = 22.sp,
            modifier = Modifier.width(32.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    title,
                    color = SpPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    en,
                    color = SpMuted,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(desc, color = SpMuted, fontSize = 12.sp)
        }
        Icon(
            icon,
            contentDescription = null,
            tint = SpPrimary.copy(alpha = 0.7f),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SpMuted,
            modifier = Modifier.size(16.dp),
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
}

// ============================================================================
// Today's recommendation —— ivory 卡片 + tag + 题干 + 圆形进入按钮
// ============================================================================

@Composable
private fun TodaysRecommendation(onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 36.dp)) {
        Eyebrow("今日推荐")
        Spacer(Modifier.height(14.dp))
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp))
                .background(SpIvory)
                .border(1.dp, SpLine, RoundedCornerShape(4.dp))
                .padding(20.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .border(1.dp, SpMoss, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            "IELTS · PART 2",
                            color = SpMoss,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("约 4 分钟", color = SpMuted, fontSize = 11.sp)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Describe a skill you\nwould like to learn.",
                    color = SpPrimary,
                    fontFamily = FraunceFamily,
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "高频话题 · 上次你在 \"structure\" 上扣分较多",
                    color = SpMuted,
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(SpPrimary)
                    .clickable(onClick = onClick),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = SpIvory,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

// ============================================================================
// Assignments —— 待完成作业列表
// ============================================================================

@Composable
private fun AssignmentsSection(pending: List<String>, onClick: () -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 32.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Eyebrow("待完成作业")
            Spacer(Modifier.weight(1f))
            Text(
                if (pending.isEmpty()) "暂无" else "共 ${pending.size} 项",
                color = SpMuted,
                fontSize = 11.sp,
            )
        }
        Spacer(Modifier.height(10.dp))

        if (pending.isEmpty()) {
            Text(
                "暂无待完成作业",
                color = SpMuted,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        } else {
            pending.forEachIndexed { idx, title ->
                AssignmentRow(
                    date = monthDayLabel(idx),
                    teacher = "老师",
                    title = title,
                    due = if (idx == 0) "明天 22:00" else "本周日",
                    onClick = onClick,
                )
            }
        }
    }
}

private fun monthDayLabel(offsetDays: Int): String {
    val d = LocalDate.now().plusDays(offsetDays.toLong() + 1)
    return "%02d·%02d".format(d.monthValue, d.dayOfMonth)
}

@Composable
private fun AssignmentRow(
    date: String, teacher: String, title: String, due: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Text(
            date,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontSize = 16.sp,
            letterSpacing = (-0.3).sp,
            modifier = Modifier.width(52.dp),
        )
        Box(Modifier.width(1.dp).height(28.dp).background(SpLine))
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = SpPrimary, fontSize = 14.sp, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text("$teacher · 截止 $due", color = SpMuted, fontSize = 11.sp)
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
}

// ============================================================================
// Footer quote
// ============================================================================

@Composable
private fun QuoteFooter() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 40.dp, bottom = 8.dp),
    ) {
        Text(
            "\"The only way to learn a language is to fall\nin love with it.\"",
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "— № ${editionNumber()} —",
            color = SpMuted,
            fontSize = 10.sp,
            letterSpacing = 2.sp,
        )
    }
}

// ============================================================================
// Eyebrow shared helper
// ============================================================================

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

// ============================================================================
// 风格切换占位提示 —— 选了未实现的 Minimal / Dashboard 时顶部软提示
// ============================================================================

@Composable
private fun UnavailableHint(style: HomeStyle) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SpAccentSoft)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            "「${style.label}」UI 待开发，暂用编辑风 · 完整卡 占位",
            color = SpAccent,
            fontSize = 11.sp,
        )
    }
}

// ============================================================================
// 风格选择 bottom sheet —— 5 选项列表，点击立即切换
// ============================================================================

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun HomeStyleSheet(
    current: HomeStyle,
    onPick: (HomeStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState()
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpBackground,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 24.dp),
        ) {
            Eyebrow("首页风格 · HOME STYLE")
            Spacer(Modifier.height(14.dp))
            Text(
                "dev · 连点三下顶部 No.XXX 即可再次打开",
                color = SpMuted,
                fontSize = 11.sp,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
            )
            Spacer(Modifier.height(16.dp))

            HomeStyle.entries.forEach { style ->
                StyleRow(
                    style = style,
                    selected = style == current,
                    onClick = { onPick(style) },
                )
            }
        }
    }
}

@Composable
private fun StyleRow(style: HomeStyle, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    style.label,
                    color = SpPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (!style.available) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "占位",
                        color = SpMuted,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .border(1.dp, SpLine, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(style.subtitle, color = SpMuted, fontSize = 12.sp)
        }
        if (selected) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(SpAccent),
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = SpIvory,
                    modifier = Modifier.size(14.dp),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .border(1.dp, SpLine, CircleShape),
            )
        }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
}
