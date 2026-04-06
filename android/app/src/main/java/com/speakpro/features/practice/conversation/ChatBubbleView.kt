package com.speakpro.features.practice.conversation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpWhite

/**
 * 聊天气泡组件
 *
 * - 考官（左对齐）：白色背景，深色文字，带盾牌头像
 * - 学生（右对齐）：强调色背景，白色文字，带人物头像
 *
 * @param isExaminer 是否为考官消息
 * @param text       消息文本
 * @param modifier   Modifier
 */
@Composable
fun ChatBubbleView(
    isExaminer: Boolean,
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isExaminer) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top,
    ) {
        if (isExaminer) {
            // 考官头像（左侧）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SpPrimary.copy(alpha = 0.1f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = "考官",
                    tint = SpPrimary,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        // 气泡
        Column(
            horizontalAlignment = if (isExaminer) Alignment.Start else Alignment.End,
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = if (isExaminer) 4.dp else 16.dp,
                            topEnd = if (isExaminer) 16.dp else 4.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp,
                        ),
                    )
                    .background(if (isExaminer) SpWhite else SpAccent)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = text,
                    style = SpBodySmall,
                    color = if (isExaminer) SpTextPrimary else SpWhite,
                )
            }
        }

        if (!isExaminer) {
            Spacer(modifier = Modifier.width(8.dp))
            // 学生头像（右侧）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SpAccent.copy(alpha = 0.1f)),
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "学生",
                    tint = SpAccent,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChatBubblePreview() {
    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ChatBubbleView(
            isExaminer = true,
            text = "Let's begin. Can you tell me about your hometown?",
        )
        ChatBubbleView(
            isExaminer = false,
            text = "Sure! My hometown is a small city in southern China.",
        )
    }
}
