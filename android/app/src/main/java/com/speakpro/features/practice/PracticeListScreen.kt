package com.speakpro.features.practice

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWhite
import com.speakpro.navigation.Routes

// ── 练习模式定义 ────────────────────────────────

private data class PracticeModeItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
)

private val practiceModes = listOf(
    PracticeModeItem(
        title = "AI 对话练习",
        subtitle = "与 AI 考官进行模拟对话，实时评分反馈",
        icon = Icons.Default.Mic,
        route = Routes.PRACTICE_CONVERSATION,
    ),
    PracticeModeItem(
        title = "朗读练习",
        subtitle = "朗读给定文章，评测发音准确度和流利度",
        icon = Icons.Default.TextSnippet,
        route = Routes.PRACTICE_READALOUD,
    ),
    PracticeModeItem(
        title = "跟读练习",
        subtitle = "跟随标准发音逐句练习，对比纠正",
        icon = Icons.Default.RecordVoiceOver,
        route = Routes.PRACTICE_FOLLOWREAD,
    ),
    PracticeModeItem(
        title = "模考练习",
        subtitle = "完整模考流程，还原真实考试体验",
        icon = Icons.Default.Timer,
        route = Routes.PRACTICE_MOCKEXAM,
    ),
)

/**
 * 练习列表页 — 对应 iOS PracticeListView
 */
@Composable
fun PracticeListScreen(
    onNavigate: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column {
                Text(
                    text = "选择练习模式",
                    style = SpTitleLarge,
                    color = SpTextPrimary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "每天坚持练习，口语能力稳步提升",
                    style = SpBodyMedium,
                    color = SpTextSecondary,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        items(practiceModes) { mode ->
            PracticeModeCard(
                item = mode,
                onClick = { onNavigate(mode.route) },
            )
        }

        item { Spacer(modifier = Modifier.height(32.dp)) }
    }
}

@Composable
private fun PracticeModeCard(
    item: PracticeModeItem,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SpWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            // 图标
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpAccent.copy(alpha = 0.1f)),
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.title,
                    tint = SpAccent,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 文本区域
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = item.title,
                    style = SpTitleSmall,
                    color = SpTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.subtitle,
                    style = SpBodySmall,
                    color = SpTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 箭头
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SpTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
