package com.speakpro.features.practice.followread

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speakpro.designsystem.components.RecordButton
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
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite

/**
 * 跟读练习页面
 *
 * 阶段：ready → listening → recording → evaluating → result
 * 每句依次：听参考音 → 录音 → 评测 → 查看结果 → 下一句
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowReadScreen(
    onBack: () -> Unit,
    viewModel: FollowReadViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsState()
    val currentSentence by viewModel.currentSentence.collectAsState()
    val currentIndex by viewModel.currentSentenceIndex.collectAsState()
    val totalSentences by viewModel.totalSentences.collectAsState()
    val pronunciationScore by viewModel.pronunciationScore.collectAsState()
    val intonationScore by viewModel.intonationScore.collectAsState()
    val fluencyScore by viewModel.fluencyScore.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPlayingReference by viewModel.isPlayingReference.collectAsState()
    val isPlayingStudent by viewModel.isPlayingStudent.collectAsState()
    val isCompleted by viewModel.isCompleted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val referenceWaveform by viewModel.referenceWaveform.collectAsState()
    val studentWaveform by viewModel.studentWaveform.collectAsState()
    var showReport by remember { mutableStateOf(false) }

    if (showReport) {
        val scores by viewModel.scoreHistory.collectAsState()
        FollowReadReportScreen(
            scores = scores,
            onDismiss = {
                showReport = false
                onBack()
            },
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        // ── 顶部栏 ──
        TopAppBar(
            title = { Text("跟读练习", style = SpTitleSmall, color = SpTextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpWhite),
        )

        // ── 进度 ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            Text(
                text = "第 ${currentIndex + 1} / $totalSentences 句",
                style = SpCaption,
                color = SpTextSecondary,
            )
        }

        // ── 参考文本 ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SpWhite)
                .padding(20.dp),
        ) {
            Text("参考句子", style = SpCaption, color = SpTextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = currentSentence,
                style = SpBodyLarge,
                color = SpTextPrimary,
                lineHeight = SpBodyLarge.lineHeight,
            )
        }

        // ── 阶段内容 ──
        Box(modifier = Modifier.weight(1f)) {
            when (phase) {
                FollowReadPhase.READY -> ReadyPhaseContent(
                    onPlayReference = { viewModel.playReferenceAndTransition() },
                )
                FollowReadPhase.LISTENING -> ListeningPhaseContent(
                    isPlaying = isPlayingReference,
                    waveform = referenceWaveform,
                )
                FollowReadPhase.RECORDING -> RecordingPhaseContent(
                    isRecording = isRecording,
                    waveform = studentWaveform,
                    onStart = { viewModel.startRecording() },
                    onStop = { viewModel.stopRecording() },
                )
                FollowReadPhase.EVALUATING -> EvaluatingPhaseContent()
                FollowReadPhase.RESULT -> ResultPhaseContent(
                    pronunciationScore = pronunciationScore,
                    intonationScore = intonationScore,
                    fluencyScore = fluencyScore,
                    referenceWaveform = referenceWaveform,
                    studentWaveform = studentWaveform,
                    isPlayingReference = isPlayingReference,
                    isPlayingStudent = isPlayingStudent,
                    onPlayReference = { viewModel.playReference() },
                    onPlayStudent = { viewModel.playStudentRecording() },
                )
            }
        }

        // ── 错误提示 ──
        errorMessage?.let {
            Text(
                text = it,
                style = SpCaption,
                color = SpError,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // ── 底部操作 ──
        if (phase == FollowReadPhase.RESULT) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpWhite)
                    .padding(20.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedButton(
                        onClick = { viewModel.playReference() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpPrimary),
                    ) {
                        Icon(Icons.Filled.VolumeUp, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("再听一次", style = SpBodyMedium)
                    }

                    OutlinedButton(
                        onClick = { viewModel.retryRecording() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpAccent),
                    ) {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("重录", style = SpBodyMedium)
                    }

                    if (!isCompleted) {
                        Button(
                            onClick = { viewModel.nextSentence() },
                            colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                        ) {
                            Icon(Icons.Filled.Forward, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("下一句", style = SpBodyMedium, color = SpWhite)
                        }
                    }
                }

                if (isCompleted) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showReport = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                    ) {
                        Text("查看完整报告", style = SpBodyMedium, color = SpWhite)
                    }
                    TextButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("结束练习", style = SpBodyMedium, color = SpTextSecondary)
                    }
                }
            }
        }
    }
}

// ── 阶段子组件 ──

@Composable
private fun ReadyPhaseContent(onPlayReference: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Headphones,
            contentDescription = null,
            tint = SpSuccess,
            modifier = Modifier.size(48.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "点击下方按钮\n先听一遍标准发音",
            style = SpBodyMedium,
            color = SpTextSecondary,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onPlayReference,
            colors = ButtonDefaults.buttonColors(containerColor = SpSuccess),
            modifier = Modifier.padding(horizontal = 40.dp),
        ) {
            Icon(Icons.Filled.VolumeUp, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("播放参考音", style = SpBodyMedium, color = SpWhite)
        }
    }
}

@Composable
private fun ListeningPhaseContent(
    isPlaying: Boolean,
    waveform: List<Float>,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
        ) {
            Text("参考音频", style = SpCaption, color = SpSuccess)
            Spacer(modifier = Modifier.height(4.dp))
            WaveformView(
                data = waveform,
                barColor = SpSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        // 播放动画条
        val infiniteTransition = rememberInfiniteTransition(label = "listen_anim")
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(16f, 24f, 32f, 24f, 16f).forEachIndexed { i, baseH ->
                val animScale by infiniteTransition.animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        tween(500, easing = LinearEasing, delayMillis = i * 100),
                        RepeatMode.Reverse,
                    ),
                    label = "bar_$i",
                )
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height((baseH * animScale).dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SpSuccess),
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("正在播放参考音...", style = SpBodyMedium, color = SpTextSecondary)
    }
}

@Composable
private fun RecordingPhaseContent(
    isRecording: Boolean,
    waveform: List<Float>,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isRecording) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Text("你的录音", style = SpCaption, color = SpAccent)
                Spacer(modifier = Modifier.height(4.dp))
                WaveformView(
                    data = waveform,
                    barColor = SpAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            RecordButton(isRecording = true, onClick = onStop)
            Spacer(modifier = Modifier.height(8.dp))
            Text("点击停止录音", style = SpCaption, color = SpTextSecondary)
        } else {
            Icon(
                Icons.Filled.Mic,
                contentDescription = null,
                tint = SpAccent,
                modifier = Modifier.size(48.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "参考音播放完毕\n点击下方按钮开始录音",
                style = SpBodyMedium,
                color = SpTextSecondary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onStart,
                colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                modifier = Modifier.padding(horizontal = 40.dp),
            ) {
                Icon(Icons.Filled.Mic, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("开始录音", style = SpBodyMedium, color = SpWhite)
            }
        }
    }
}

@Composable
private fun EvaluatingPhaseContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = SpAccent, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text("正在评测你的发音...", style = SpBodyMedium, color = SpTextSecondary)
    }
}

@Composable
private fun ResultPhaseContent(
    pronunciationScore: Double,
    intonationScore: Double,
    fluencyScore: Double,
    referenceWaveform: List<Float>,
    studentWaveform: List<Float>,
    isPlayingReference: Boolean,
    isPlayingStudent: Boolean,
    onPlayReference: () -> Unit,
    onPlayStudent: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // 参考音播放
        WaveformCard(
            label = "参考",
            labelColor = SpSuccess,
            waveform = referenceWaveform,
            barColor = SpSuccess,
            isPlaying = isPlayingReference,
            onTogglePlay = onPlayReference,
        )

        // 我的录音播放
        WaveformCard(
            label = "你的录音",
            labelColor = SpAccent,
            waveform = studentWaveform,
            barColor = SpAccent,
            isPlaying = isPlayingStudent,
            onTogglePlay = onPlayStudent,
        )

        // 评分卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScoreCard("发音", pronunciationScore, SpSuccess, Modifier.weight(1f))
            ScoreCard("语调", intonationScore, SpAccent, Modifier.weight(1f))
            ScoreCard("流利度", fluencyScore, SpPrimary, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WaveformCard(
    label: String,
    labelColor: androidx.compose.ui.graphics.Color,
    waveform: List<Float>,
    barColor: androidx.compose.ui.graphics.Color,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpWhite)
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, style = SpCaption, color = labelColor)
            TextButton(onClick = onTogglePlay) {
                Icon(
                    if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    null,
                    modifier = Modifier.size(14.dp),
                    tint = labelColor,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    if (isPlaying) "停止" else "播放",
                    style = SpCaption,
                    color = labelColor,
                )
            }
        }
        WaveformView(
            data = waveform,
            barColor = barColor,
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
        )
    }
}

@Composable
private fun ScoreCard(
    title: String,
    score: Double,
    color: androidx.compose.ui.graphics.Color,
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
