package com.speakpro.features.practice.mockexam

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.data.models.FullEvaluateResult
import com.speakpro.designsystem.components.ScoreRing
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpSurface
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleMedium
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite

/**
 * 模考单题评测结果页
 *
 * - 评分头部：总分 ScoreRing + 三维度
 * - 题目卡片（Part 标签 + 重做按钮）
 * - 我的答案：字数统计 + 音频播放 + 转写文本（错误高亮）
 * - 4-Tab 分析：已修订 / 思维导图 / 关键词 / 样例答案
 * - 底部按钮：重做 + 下一题
 */
@Composable
fun MockExamResultScreen(
    question: String,
    part: Int,
    result: FullEvaluateResult?,
    questionIndex: Int = 0,
    totalQuestions: Int = 0,
    onNext: (() -> Unit)? = null,
    onRedo: (() -> Unit)? = null,
) {
    val safeResult = result ?: FullEvaluateResult(sessionId = "")
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("已修订", "思维导图", "关键词", "样例答案")
    var isPlayingStudent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── 评分头部 ──
            ScoreHeader(safeResult)

            Spacer(modifier = Modifier.height(16.dp))

            // ── 题目卡片 ──
            QuestionCard(question, part, onRedo)

            Spacer(modifier = Modifier.height(16.dp))

            // ── 我的答案 ──
            MyAnswerSection(safeResult, isPlayingStudent) {
                isPlayingStudent = !isPlayingStudent
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Tab 分析 ──
            TabbedAnalysis(safeResult, tabs, selectedTab) { selectedTab = it }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── 底部按钮 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpWhite)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onRedo != null) {
                OutlinedButton(
                    onClick = onRedo,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpAccent),
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重做", style = SpBodySmall)
                }
            }
            if (onNext != null) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                ) {
                    Text("下一题", style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SpWhite)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Forward, null, modifier = Modifier.size(13.dp), tint = SpWhite)
                }
            }
        }
    }
}

@Composable
private fun ScoreHeader(result: FullEvaluateResult) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(SpAccent.copy(alpha = 0.08f), SpBackground),
                ),
            )
            .padding(vertical = 20.dp),
    ) {
        Text("AI 评估", style = SpTitleMedium, color = SpTextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        ScoreRing(
            score = result.overallScore.toInt(),
            color = scoreColor(result.overallScore),
            lineWidth = 10.dp,
            size = 100.dp,
        )
        Spacer(modifier = Modifier.height(16.dp))

        // 三维度
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SpWhite)
                .padding(vertical = 12.dp),
        ) {
            DimensionScore("发音", result.pronunciationScore?.overall ?: 0.0, SpSuccess, Modifier.weight(1f))
            DimensionScore("流利度", result.fluencyScore?.score ?: 0.0, SpPrimary, Modifier.weight(1f))
            DimensionScore("语法", result.grammarScore?.score ?: 0.0, SpAccent, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DimensionScore(title: String, score: Double, color: Color, modifier: Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Text(
            "${score.toInt()}",
            style = SpTitleMedium.copy(fontWeight = FontWeight.Bold),
            color = color,
        )
        Text(title, style = SpCaption.copy(fontSize = 11.sp), color = SpTextSecondary)
    }
}

@Composable
private fun QuestionCard(question: String, part: Int, onRedo: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SpWhite)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Part $part",
                style = SpCaption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                color = SpWhite,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SpAccent)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
            if (onRedo != null) {
                TextButton(onClick = onRedo) {
                    Icon(Icons.Filled.Edit, null, tint = SpAccent, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("重做", style = SpCaption, color = SpAccent)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(question, style = SpBodyMedium, color = SpTextPrimary, lineHeight = SpBodyMedium.lineHeight)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MyAnswerSection(
    result: FullEvaluateResult,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(SpWhite)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我的答案", style = SpTitleSmall, color = SpTextPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            // 统计信息可根据 result 展示
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 播放按钮
        Button(
            onClick = onTogglePlay,
            colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
        ) {
            Icon(
                if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPlaying) "停止" else "播放", style = SpBodySmall, color = SpWhite)
        }

        // 转写文本 + 错误高亮
        result.aiFeedback?.let { transcript ->
            if (transcript.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                val errorTexts = result.grammarScore?.errors
                    ?.mapNotNull { it.text?.lowercase() }
                    ?.toSet() ?: emptySet()

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    transcript.split(" ").forEach { word ->
                        val isError = errorTexts.any { it.contains(word.lowercase()) }
                        Text(
                            text = word,
                            style = SpBodySmall,
                            color = if (isError) SpError else SpTextPrimary,
                            textDecoration = if (isError) TextDecoration.Underline else TextDecoration.None,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabbedAnalysis(
    result: FullEvaluateResult,
    tabs: List<String>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Column {
        // Tab 栏
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = SpWhite,
            edgePadding = 16.dp,
            indicator = {},
            divider = { Divider() },
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { onTabSelected(index) },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                title,
                                style = SpBodySmall.copy(
                                    fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                                color = if (selectedTab == index) SpAccent else SpTextSecondary,
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .height(2.dp)
                                    .width(24.dp)
                                    .background(if (selectedTab == index) SpAccent else Color.Transparent),
                            )
                        }
                    },
                )
            }
        }

        // Tab 内容
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SpWhite)
                .padding(16.dp),
        ) {
            when (selectedTab) {
                0 -> RevisedTab(result)
                1 -> MindMapTab(result)
                2 -> KeywordsTab(result)
                3 -> SampleAnswersTab(result)
            }
        }
    }
}

@Composable
private fun RevisedTab(result: FullEvaluateResult) {
    Column {
        Text("修改后答案", style = SpTitleSmall, color = SpAccent)
        Spacer(modifier = Modifier.height(12.dp))
        // 注：full-evaluate 的 revisedAnswer 需要在 ApiModels 中定义
        // 这里使用 aiFeedback 作为替代内容
        val content = result.aiFeedback ?: "修订答案生成中..."
        Text(content, style = SpBodySmall, color = SpTextPrimary, lineHeight = SpBodySmall.lineHeight)
    }
}

@Composable
private fun MindMapTab(result: FullEvaluateResult) {
    Column {
        Text("思维导图", style = SpTitleSmall, color = SpAccent)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            result.suggestions?.joinToString("\n\n") ?: "思维导图生成中...",
            style = SpBodySmall,
            color = SpTextPrimary,
        )
    }
}

@Composable
private fun KeywordsTab(result: FullEvaluateResult) {
    Column {
        Text("关键词", style = SpTitleSmall, color = SpAccent)
        Spacer(modifier = Modifier.height(12.dp))
        if (result.suggestions.isNullOrEmpty()) {
            Text("关键词生成中...", style = SpBodySmall, color = SpTextSecondary)
        } else {
            result.suggestions?.forEach { keyword ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpSurface.copy(alpha = 0.5f))
                        .padding(12.dp),
                ) {
                    Text(
                        keyword,
                        style = SpBodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = SpTextPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun SampleAnswersTab(result: FullEvaluateResult) {
    Column {
        Text("样例答案", style = SpTitleSmall, color = SpAccent)
        Spacer(modifier = Modifier.height(12.dp))
        if (result.suggestions.isNullOrEmpty()) {
            Text("样例答案生成中...", style = SpBodySmall, color = SpTextSecondary)
        } else {
            result.suggestions?.forEachIndexed { i, sample ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, SpSurface, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                ) {
                    Text("${i + 1}.", style = SpBodySmall.copy(fontWeight = FontWeight.Bold), color = SpAccent)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(sample, style = SpBodySmall, color = SpTextPrimary, lineHeight = SpBodySmall.lineHeight)
                }
            }
        }
    }
}

private fun scoreColor(score: Double): Color {
    return when {
        score >= 80 -> SpSuccess
        score >= 60 -> SpWarning
        else -> SpError
    }
}
