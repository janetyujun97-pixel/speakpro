package com.speakpro.features.practice.mockexam

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.components.ScoreRing
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpSurface
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite

/**
 * 模考总结页
 *
 * - 总分 ScoreRing
 * - 分 Part 得分进度条
 * - 各题明细
 * - 再测一次 + 返回
 */
@Composable
fun MockExamSummaryScreen(
    overallScore: Double,
    partAverages: List<Pair<Int, Double>>,
    scores: List<EnrichedQuestionScore>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // ── 总分 ──
        Text("考试报告", style = SpTitleLarge, color = SpTextPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        ScoreRing(
            score = overallScore.toInt(),
            color = scoreColor(overallScore),
            lineWidth = 14.dp,
            size = 140.dp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = when {
                overallScore >= 80 -> "优秀"
                overallScore >= 60 -> "良好"
                else -> "继续加油"
            },
            style = SpBodyMedium,
            color = SpTextSecondary,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ── 各部分得分 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpSurface)
                .padding(20.dp),
        ) {
            Text("各部分得分", style = SpTitleSmall, color = SpTextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            partAverages.forEach { (part, avg) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Part $part",
                        style = SpBodyMedium,
                        color = SpTextPrimary,
                        modifier = Modifier.width(60.dp),
                    )

                    // 进度条
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SpSurface),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((avg / 100.0).toFloat().coerceIn(0f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(scoreColor(avg)),
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "${avg.toInt()}",
                        style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = SpTextPrimary,
                        modifier = Modifier.width(40.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 各题明细 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SpSurface)
                .padding(20.dp),
        ) {
            Text("各题明细", style = SpTitleSmall, color = SpTextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            scores.forEach { qs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SpWhite)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Part ${qs.part}", style = SpCaption, color = SpAccent)
                        Text(
                            qs.question,
                            style = SpBodySmall,
                            color = SpTextSecondary,
                            maxLines = 2,
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${qs.score.toInt()}",
                        style = SpTitleSmall.copy(fontWeight = FontWeight.Bold),
                        color = scoreColor(qs.score),
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ── 按钮 ──
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {
            Text("再测一次", style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SpWhite)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("返回", style = SpBodyMedium, color = SpTextSecondary)
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private fun scoreColor(score: Double): Color {
    return when {
        score >= 80 -> SpSuccess
        score >= 60 -> SpWarning
        else -> SpError
    }
}
