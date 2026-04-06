package com.speakpro.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleSmall

/**
 * 富文本反馈展示组件
 *
 * 解析 AI 返回的 markdown 风格文本：
 * - **heading** → 粗体标题
 * - - bullet → 彩色圆点 + 文本
 * - "quote" → 左边框 + 斜体
 * - 普通文本 → 正常段落
 * - 移除所有 ** 标记
 *
 * @param text   AI 反馈文本
 * @param modifier Modifier
 */
@Composable
fun RichFeedbackView(
    text: String,
    modifier: Modifier = Modifier,
) {
    val blocks = remember(text) { parseFeedbackText(text) }

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        blocks.forEachIndexed { index, block ->
            when (block) {
                is FeedbackBlock.Heading -> {
                    if (index > 0) Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = block.text,
                        style = SpTitleSmall.copy(fontWeight = FontWeight.Bold),
                        color = SpTextPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                is FeedbackBlock.Bullet -> {
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        // 彩色圆点
                        val dotColor = when (index % 3) {
                            0 -> SpAccent
                            1 -> SpSuccess
                            else -> SpPrimary
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .padding(top = 6.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(dotColor),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = block.text,
                            style = SpBodySmall,
                            color = SpTextSecondary,
                        )
                    }
                }

                is FeedbackBlock.Quote -> {
                    Row(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth(),
                    ) {
                        // 左边框
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(20.dp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(SpAccent.copy(alpha = 0.4f)),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = block.text,
                            style = SpBodySmall.copy(fontStyle = FontStyle.Italic),
                            color = SpTextSecondary,
                        )
                    }
                }

                is FeedbackBlock.Paragraph -> {
                    if (index > 0) Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = block.text,
                        style = SpBodySmall,
                        color = SpTextPrimary,
                    )
                }
            }
        }
    }
}

// ── 解析逻辑 ──

private sealed class FeedbackBlock {
    data class Heading(val text: String) : FeedbackBlock()
    data class Bullet(val text: String) : FeedbackBlock()
    data class Quote(val text: String) : FeedbackBlock()
    data class Paragraph(val text: String) : FeedbackBlock()
}

/**
 * 将 markdown 风格文本解析为结构化块
 */
private fun parseFeedbackText(raw: String): List<FeedbackBlock> {
    val blocks = mutableListOf<FeedbackBlock>()
    val lines = raw.split("\n")

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        when {
            // **heading** → 整行被 ** 包裹
            trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length > 4 -> {
                val heading = trimmed.removePrefix("**").removeSuffix("**").trim()
                blocks.add(FeedbackBlock.Heading(heading))
            }

            // - bullet
            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                val bullet = cleanMarkers(trimmed.substring(2).trim())
                blocks.add(FeedbackBlock.Bullet(bullet))
            }

            // "quote"
            trimmed.startsWith("\"") && trimmed.endsWith("\"") -> {
                val quote = trimmed.removeSurrounding("\"").trim()
                blocks.add(FeedbackBlock.Quote(quote))
            }

            // 普通段落
            else -> {
                val cleaned = cleanMarkers(trimmed)
                blocks.add(FeedbackBlock.Paragraph(cleaned))
            }
        }
    }

    return blocks
}

/** 移除所有 ** 标记 */
private fun cleanMarkers(text: String): String {
    return text.replace("**", "")
}

@Preview(showBackground = true)
@Composable
private fun RichFeedbackPreview() {
    RichFeedbackView(
        text = """
            **Pronunciation**
            - Your vowel sounds need more practice, especially the /ae/ sound.
            - Try to distinguish between /l/ and /r/ more clearly.

            **Fluency**
            - Good overall pace, but there were some unnecessary pauses.
            "Practice reading aloud daily to improve your rhythm."

            Keep up the good work!
        """.trimIndent(),
        modifier = Modifier.padding(16.dp),
    )
}
