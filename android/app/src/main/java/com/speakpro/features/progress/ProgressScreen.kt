package com.speakpro.features.progress

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentSoft
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpWhite

/**
 * 进度页面 — 占位实现，Sprint 2 增加图表和详细维度分析
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
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "学习进度",
            style = SpTitleLarge,
            color = SpTextPrimary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 统计卡片行
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            StatCard(
                label = "练习次数",
                value = "$totalSessions",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "平均分",
                value = if (averageScore > 0) "%.1f".format(averageScore) else "--",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "连续天数",
                value = "$streakDays",
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Review 入口（PR3c）
        ReviewEntry(
            title = "历史回听",
            subtitle = "按天查看练习记录并回听",
            icon = Icons.Filled.GraphicEq,
            onClick = onOpenHistory,
        )
        Spacer(Modifier.height(12.dp))
        ReviewEntry(
            title = "错题本 / 生词本",
            subtitle = "低分单词 + 间隔复习",
            icon = Icons.Filled.MenuBook,
            onClick = onOpenNotebook,
        )
        Spacer(Modifier.height(12.dp))
        ReviewEntry(
            title = "通知中心",
            subtitle = "作业 / 批改 / 提醒",
            icon = Icons.Filled.Notifications,
            onClick = onOpenNotifications,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "更多进度详情将在后续版本中推出",
            style = SpBodyMedium,
            color = SpTextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun ReviewEntry(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(SpAccentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = SpAccent)
            }
            Spacer(Modifier.size(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = SpTextPrimary, fontSize = 14.sp)
                Text(subtitle, color = SpMuted, fontSize = 11.sp)
            }
            Icon(
                Icons.Filled.ChevronRight,
                null,
                tint = SpMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = SpAccent,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = SpBodyMedium,
                color = SpTextSecondary,
            )
        }
    }
}
