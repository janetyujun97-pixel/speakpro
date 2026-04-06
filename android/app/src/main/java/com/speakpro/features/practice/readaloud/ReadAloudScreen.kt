package com.speakpro.features.practice.readaloud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speakpro.designsystem.components.RecordButton
import com.speakpro.designsystem.components.RichFeedbackView
import com.speakpro.designsystem.components.ScoreRing
import com.speakpro.designsystem.components.WaveformView
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyLarge
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpTitleMedium
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite

/**
 * 朗读练习页面
 *
 * 阶段：reading → recording → evaluating → result
 * 支持多篇文章循环练习
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadAloudScreen(
    onBack: () -> Unit,
    viewModel: ReadAloudViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsState()
    val articleTitle by viewModel.articleTitle.collectAsState()
    val articleText by viewModel.articleText.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val totalArticles by viewModel.totalArticles.collectAsState()
    val isPlayingDemo by viewModel.isPlayingDemo.collectAsState()
    val isPlayingStudent by viewModel.isPlayingStudent.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val overallScore by viewModel.overallScore.collectAsState()
    val pronunciationScore by viewModel.pronunciationScore.collectAsState()
    val fluencyScore by viewModel.fluencyScore.collectAsState()
    val completenessScore by viewModel.completenessScore.collectAsState()
    val aiFeedback by viewModel.aiFeedback.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        TopAppBar(
            title = { Text("朗读练习", style = SpTitleSmall, color = SpTextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpWhite),
        )

        when (phase) {
            ReadAloudPhase.READING -> ReadingPhase(
                articleTitle = articleTitle,
                articleText = articleText,
                currentIndex = currentIndex,
                totalArticles = totalArticles,
                isPlayingDemo = isPlayingDemo,
                playbackSpeed = playbackSpeed,
                errorMessage = errorMessage,
                onPlayDemo = { viewModel.playDemo() },
                onSetSpeed = { viewModel.setPlaybackSpeed(it) },
                onStartRecording = { viewModel.startRecording() },
            )

            ReadAloudPhase.RECORDING -> RecordingPhase(
                articleTitle = articleTitle,
                articleText = articleText,
                waveformData = waveformData,
                onStop = { viewModel.stopRecording() },
            )

            ReadAloudPhase.EVALUATING -> EvaluatingPhase()

            ReadAloudPhase.RESULT -> ResultPhase(
                overallScore = overallScore,
                pronunciationScore = pronunciationScore,
                fluencyScore = fluencyScore,
                completenessScore = completenessScore,
                aiFeedback = aiFeedback,
                errorMessage = errorMessage,
                waveformData = waveformData,
                isPlayingStudent = isPlayingStudent,
                isPlayingDemo = isPlayingDemo,
                isLastArticle = viewModel.isLastArticle,
                onPlayStudent = { viewModel.playStudentRecording() },
                onPlayDemo = { viewModel.playDemo() },
                onNextArticle = { viewModel.nextArticle() },
                onRetry = { viewModel.retryRecording() },
                onDismiss = onBack,
            )
        }
    }
}

@Composable
private fun ReadingPhase(
    articleTitle: String,
    articleText: String,
    currentIndex: Int,
    totalArticles: Int,
    isPlayingDemo: Boolean,
    playbackSpeed: Float,
    errorMessage: String?,
    onPlayDemo: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onStartRecording: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 进度
        if (totalArticles > 1) {
            Text(
                "第 ${currentIndex + 1} / $totalArticles 篇",
                style = SpCaption,
                color = SpTextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // 文章内容
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(SpWhite)
                .padding(20.dp),
        ) {
            Text(articleTitle, style = SpTitleMedium, color = SpTextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(articleText, style = SpBodyLarge, color = SpTextPrimary, lineHeight = SpBodyLarge.lineHeight)
        }

        Divider()

        // 示范播放 + 语速
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onPlayDemo) {
                Icon(
                    if (isPlayingDemo) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = SpPrimary,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isPlayingDemo) "暂停示范" else "听示范朗读",
                    style = SpBodyMedium,
                    color = SpPrimary,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text("语速", style = SpCaption, color = SpTextSecondary)
            Spacer(modifier = Modifier.width(4.dp))
            listOf(0.5f, 1.0f, 1.5f).forEach { speed ->
                FilterChip(
                    selected = playbackSpeed == speed,
                    onClick = { onSetSpeed(speed) },
                    label = { Text("${speed}x", style = SpCaption) },
                    modifier = Modifier.padding(horizontal = 2.dp),
                )
            }
        }

        errorMessage?.let {
            Text(it, style = SpCaption, color = SpError, modifier = Modifier.padding(horizontal = 20.dp))
        }

        Spacer(modifier = Modifier.weight(0.01f))

        Button(
            onClick = onStartRecording,
            colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 32.dp),
        ) {
            Icon(Icons.Filled.Mic, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("开始朗读", style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SpWhite)
        }
    }
}

@Composable
private fun RecordingPhase(
    articleTitle: String,
    articleText: String,
    waveformData: List<Float>,
    onStop: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .background(SpWhite)
                .padding(20.dp),
        ) {
            Text(articleTitle, style = SpTitleMedium, color = SpTextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(articleText, style = SpBodyLarge, color = SpTextSecondary, lineHeight = SpBodyLarge.lineHeight)
        }

        Divider()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
        ) {
            Text("正在录音...", style = SpBodyMedium, color = SpAccent)
            Spacer(modifier = Modifier.height(8.dp))
            WaveformView(
                data = waveformData,
                barColor = SpAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 20.dp),
            )
        }

        Spacer(modifier = Modifier.weight(0.01f))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {
            RecordButton(isRecording = true, onClick = onStop)
            Spacer(modifier = Modifier.height(8.dp))
            Text("点击停止录音", style = SpCaption, color = SpTextSecondary)
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun EvaluatingPhase() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = SpAccent, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text("正在评测你的朗读...", style = SpBodyMedium, color = SpTextSecondary)
    }
}

@Composable
private fun ResultPhase(
    overallScore: Double,
    pronunciationScore: Double,
    fluencyScore: Double,
    completenessScore: Double,
    aiFeedback: String?,
    errorMessage: String?,
    waveformData: List<Float>,
    isPlayingStudent: Boolean,
    isPlayingDemo: Boolean,
    isLastArticle: Boolean,
    onPlayStudent: () -> Unit,
    onPlayDemo: () -> Unit,
    onNextArticle: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            // 总分
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text("朗读评测结果", style = SpTitleLarge, color = SpTextPrimary)
                Spacer(modifier = Modifier.height(16.dp))
                ScoreRing(
                    score = overallScore.toInt(),
                    color = scoreColor(overallScore),
                    lineWidth = 12.dp,
                    size = 120.dp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 三维度评分
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DimensionCard("发音", pronunciationScore, SpSuccess, Modifier.weight(1f))
                DimensionCard("流利度", fluencyScore, SpAccent, Modifier.weight(1f))
                DimensionCard("完整度", completenessScore, SpPrimary, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 录音回放
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpWhite)
                    .padding(16.dp),
            ) {
                Text("你的录音", style = SpTitleSmall, color = SpTextPrimary)
                Spacer(modifier = Modifier.height(12.dp))
                Row {
                    Button(
                        onClick = onPlayStudent,
                        colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                    ) {
                        Icon(
                            if (isPlayingStudent) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isPlayingStudent) "停止" else "播放录音",
                            style = SpBodyMedium,
                            color = SpWhite,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onPlayDemo,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpPrimary),
                    ) {
                        Icon(
                            if (isPlayingDemo) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isPlayingDemo) "停止" else "听示范", style = SpBodyMedium)
                    }
                }
                if (waveformData.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    WaveformView(
                        data = waveformData,
                        barColor = SpAccent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                    )
                }
            }

            // AI 反馈
            if (!aiFeedback.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
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
                    RichFeedbackView(text = aiFeedback)
                }
            }

            // 自动建议
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpWhite)
                    .padding(16.dp),
            ) {
                Text("练习提示", style = SpTitleSmall, color = SpTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                generateTips(pronunciationScore, fluencyScore, completenessScore).forEach { tip ->
                    Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(Icons.Filled.Lightbulb, null, tint = SpWarning, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(tip, style = SpBodySmall, color = SpTextSecondary)
                    }
                }
            }

            errorMessage?.let {
                Text(it, style = SpCaption, color = SpError, modifier = Modifier.padding(16.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 底部操作
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpWhite)
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            if (!isLastArticle) {
                Button(
                    onClick = onNextArticle,
                    colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Filled.Forward, null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("下一篇文章", style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold), color = SpWhite)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpAccent),
                ) {
                    Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("重新朗读", style = SpBodySmall)
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SpTextSecondary),
                ) {
                    Text("结束练习", style = SpBodySmall)
                }
            }
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

private fun scoreColor(score: Double): Color {
    return when {
        score >= 80 -> SpSuccess
        score >= 60 -> SpWarning
        else -> SpError
    }
}

private fun generateTips(pron: Double, flu: Double, comp: Double): List<String> {
    val tips = mutableListOf<String>()
    if (pron < 70) tips.add("发音准确度需要提升，建议先听示范朗读，注意每个单词的重音和元音发音。")
    if (flu < 70) tips.add("朗读流畅度不够，尝试减少停顿，注意句子之间的自然衔接和连读。")
    if (comp < 70) tips.add("朗读完整度不足，确保每个单词都清晰读出，不要跳过或吞掉词语。")
    if (tips.isEmpty()) tips.add("表现不错！可以尝试提高语速或挑战更长的段落来进一步提升。")
    return tips
}
