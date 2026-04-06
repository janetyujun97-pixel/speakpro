package com.speakpro.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite
import com.speakpro.navigation.Routes

/**
 * 首页 — 对应 iOS HomeView
 */
@Composable
fun HomeScreen(
    onNavigateToPractice: (String) -> Unit,
    onNavigateToHomework: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val streakDays by viewModel.streakDays.collectAsState()
    val todayProgress by viewModel.todayProgress.collectAsState()
    val recommendedItems by viewModel.recommendedItems.collectAsState()
    val pendingHomework by viewModel.pendingHomework.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // ── 问候 + 连续打卡 ──
        GreetingSection(streakDays = streakDays)

        Spacer(modifier = Modifier.height(24.dp))

        // ── 今日进度 ──
        TodayProgressSection(progress = todayProgress)

        Spacer(modifier = Modifier.height(24.dp))

        // ── 快速入口 ──
        QuickEntrySection(onNavigate = onNavigateToPractice)

        Spacer(modifier = Modifier.height(24.dp))

        // ── 推荐练习 ──
        RecommendedSection(
            items = recommendedItems,
            onNavigate = onNavigateToPractice,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 待完成作业 ──
        HomeworkReminderSection(
            items = pendingHomework,
            onNavigate = onNavigateToHomework,
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ── 问候区域 ────────────────────────────────────

@Composable
private fun GreetingSection(streakDays: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "你好，同学！",
                style = SpTitleLarge,
                color = SpTextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "已连续练习 $streakDays 天",
                style = SpBodyMedium,
                color = SpTextSecondary,
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(SpAccent.copy(alpha = 0.1f)),
        ) {
            Icon(
                imageVector = Icons.Default.LocalFireDepartment,
                contentDescription = "连续打卡",
                tint = SpAccent,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

// ── 今日进度 ────────────────────────────────────

@Composable
private fun TodayProgressSection(progress: Float) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日进度", style = SpTitleSmall, color = SpTextPrimary)
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = SpBodyMedium,
                    color = SpAccent,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = SpAccent,
                trackColor = SpAccent.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}

// ── 快速入口（2x2 网格） ────────────────────────

private data class QuickEntry(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val route: String,
)

private val quickEntries = listOf(
    QuickEntry("AI 对话", Icons.Default.Mic, SpAccent, Routes.PRACTICE_CONVERSATION),
    QuickEntry("跟读", Icons.Default.RecordVoiceOver, SpSuccess, Routes.PRACTICE_FOLLOWREAD),
    QuickEntry("朗读", Icons.Default.TextSnippet, SpPrimary, Routes.PRACTICE_READALOUD),
    QuickEntry("模考", Icons.Default.Timer, SpWarning, Routes.PRACTICE_MOCKEXAM),
)

@Composable
private fun QuickEntrySection(onNavigate: (String) -> Unit) {
    Column {
        Text("快速开始", style = SpTitleSmall, color = SpTextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(180.dp), // 固定高度避免嵌套滚动冲突
            userScrollEnabled = false,
        ) {
            items(quickEntries) { entry ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.clickable { onNavigate(entry.route) },
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    ) {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = entry.title,
                            tint = entry.color,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = entry.title,
                            style = SpBodySmall,
                            color = SpTextPrimary,
                        )
                    }
                }
            }
        }
    }
}

// ── 推荐练习（横向滚动） ────────────────────────

@Composable
private fun RecommendedSection(
    items: List<RecommendedItem>,
    onNavigate: (String) -> Unit,
) {
    if (items.isEmpty()) return

    Column {
        Text("推荐练习", style = SpTitleSmall, color = SpTextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 8.dp),
        ) {
            items(items) { item ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SpWhite),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .width(160.dp)
                        .clickable { onNavigate(item.route) },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = SpAccent,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = item.title,
                            style = SpBodyMedium,
                            color = SpTextPrimary,
                            maxLines = 2,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "点击开始练习",
                            style = SpCaption,
                            color = SpTextSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ── 待完成作业 ──────────────────────────────────

@Composable
private fun HomeworkReminderSection(
    items: List<String>,
    onNavigate: () -> Unit,
) {
    Column {
        Text("待完成作业", style = SpTitleSmall, color = SpTextPrimary)
        Spacer(modifier = Modifier.height(12.dp))

        if (items.isEmpty()) {
            Text(
                text = "暂无待完成作业",
                style = SpBodyMedium,
                color = SpTextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
            )
        } else {
            items.forEach { title ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = SpWhite),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clickable { onNavigate() },
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = SpWarning,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = title,
                            style = SpBodyMedium,
                            color = SpTextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = SpTextSecondary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
