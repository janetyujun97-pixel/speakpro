package com.speakpro.features.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
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

        // 占位提示
        Text(
            text = "更多进度详情将在后续版本中推出",
            style = SpBodyMedium,
            color = SpTextSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
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
