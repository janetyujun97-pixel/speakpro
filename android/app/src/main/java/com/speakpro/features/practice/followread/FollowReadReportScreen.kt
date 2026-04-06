package com.speakpro.features.practice.followread

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.components.ScoreRing
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite

/**
 * 跟读练习完整评分报告
 *
 * - 总分 ScoreRing
 * - 三维度平均分卡片
 * - 逐句评分行
 * - 改进建议
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowReadReportScreen(
    scores: List<SentenceScore>,
    onDismiss: () -> Unit,
) {
    val avgPronunciation = if (scores.isNotEmpty()) scores.map { it.pronunciation }.average() else 0.0
    val avgIntonation = if (scores.isNotEmpty()) scores.map { it.intonation }.average() else 0.0
    val avgFluency = if (scores.isNotEmpty()) scores.map { it.fluency }.average() else 0.0
    val overallScore = (avgPronunciation + avgIntonation + avgFluency) / 3.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        TopAppBar(
            title = { Text("练习报告", style = SpTitleSmall, color = SpTextPrimary) },
            actions = {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, "关闭", tint = SpAccent)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpWhite),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // ── 总分 ──
            Text("跟读练习报告", style = SpTitleLarge, color = SpTextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            ScoreRing(
                score = overallScore.toInt(),
                color = scoreColor(overallScore),
                lineWidth = 12.dp,
                size = 120.dp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${scores.size} 个句子 \u00b7 平均 ${overallScore.toInt()} 分",
                style = SpBodyMedium,
                color = SpTextSecondary,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 三维度平均分 ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DimensionCard("发音", avgPronunciation, SpSuccess, Modifier.weight(1f))
                DimensionCard("语调", avgIntonation, SpAccent, Modifier.weight(1f))
                DimensionCard("流利度", avgFluency, SpPrimary, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 逐句评分 ──
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("逐句评分", style = SpTitleSmall, color = SpTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                scores.forEachIndexed { index, score ->
                    SentenceScoreRow(index + 1, score)
                    if (index < scores.size - 1) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 改进建议 ──
            val suggestions = buildSuggestions(avgPronunciation, avgIntonation, avgFluency)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpWhite)
                    .padding(16.dp),
            ) {
                Text("改进建议", style = SpTitleSmall, color = SpTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                suggestions.forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            Icons.Filled.Lightbulb,
                            null,
                            tint = SpWarning,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tip, style = SpBodySmall, color = SpTextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── 完成按钮 ──
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text("完成", style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SpWhite)
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DimensionCard(
    title: String,
    score: Double,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SpWhite)
            .padding(vertical = 12.dp),
    ) {
        ScoreRing(score = score.toInt(), color = color, lineWidth = 6.dp, size = 60.dp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = SpCaption, color = SpTextSecondary)
    }
}

@Composable
private fun SentenceScoreRow(index: Int, score: SentenceScore) {
    val avg = (score.pronunciation + score.intonation + score.fluency) / 3.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpWhite)
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                "第 $index 句",
                style = SpCaption.copy(fontWeight = FontWeight.SemiBold),
                color = SpTextPrimary,
            )
            Text(
                "${avg.toInt()} 分",
                style = SpBodyMedium.copy(fontWeight = FontWeight.Bold),
                color = scoreColor(avg),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = score.sentence,
            style = SpBodySmall,
            color = SpTextSecondary,
            maxLines = 2,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ScoreTag("发音", score.pronunciation, SpSuccess)
            ScoreTag("语调", score.intonation, SpAccent)
            ScoreTag("流利度", score.fluency, SpPrimary)
        }
    }
}

@Composable
private fun ScoreTag(label: String, score: Double, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "$label ${score.toInt()}",
            style = SpCaption.copy(fontSize = 11.sp),
            color = SpTextSecondary,
        )
    }
}

private fun scoreColor(score: Double): Color {
    return when {
        score >= 80 -> SpSuccess
        score >= 60 -> SpWarning
        else -> SpError
    }
}

private fun buildSuggestions(pron: Double, inton: Double, flu: Double): List<String> {
    val tips = mutableListOf<String>()
    if (pron < 70) tips.add("发音准确度需要提升，建议放慢语速逐词模仿标准发音。")
    if (inton < 70) tips.add("语调变化不够自然，注意句子中的升降调和重读词。")
    if (flu < 70) tips.add("流利度有待提高，减少停顿和犹豫，多做连读练习。")
    if (tips.isEmpty()) tips.add("表现不错！继续保持每日跟读的习惯。")
    return tips
}
