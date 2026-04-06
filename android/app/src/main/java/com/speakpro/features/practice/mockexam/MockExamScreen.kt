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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speakpro.designsystem.components.RecordButton
import com.speakpro.designsystem.components.WaveformView
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBlack
import com.speakpro.designsystem.theme.SpBodyLarge
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleMedium
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWhite

/**
 * 模拟考试页面
 *
 * 阶段：loading → ready → inProgress → evaluating → showingResult → sectionTransition → finished
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockExamScreen(
    onBack: () -> Unit,
    viewModel: MockExamViewModel = viewModel(),
) {
    val phase by viewModel.phase.collectAsState()
    val currentPart by viewModel.currentPart.collectAsState()
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val currentQuestion by viewModel.currentQuestion.collectAsState()
    val subQuestions by viewModel.subQuestions.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isPaused by viewModel.isPaused.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val evaluationProgress by viewModel.evaluationProgress.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val examScores by viewModel.examScores.collectAsState()
    val overallScore by viewModel.overallScore.collectAsState()
    val transitionFromPart by viewModel.transitionFromPart.collectAsState()
    val transitionToPart by viewModel.transitionToPart.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        // 顶部栏
        TopAppBar(
            title = { Text("模拟考试", style = SpTitleSmall, color = SpTextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            actions = {
                if (phase == ExamPhase.IN_PROGRESS || phase == ExamPhase.READY) {
                    TextButton(onClick = { viewModel.endExam() }) {
                        Text("结束考试", style = SpCaption, color = SpAccent)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpWhite),
        )

        when (phase) {
            ExamPhase.LOADING -> LoadingContent()

            ExamPhase.SECTION_TRANSITION -> SectionTransitionContent(
                fromPart = transitionFromPart,
                toPart = transitionToPart,
                onContinue = { viewModel.continueAfterTransition() },
            )

            ExamPhase.EVALUATING -> EvaluatingContent(evaluationProgress)

            ExamPhase.SHOWING_RESULT -> MockExamResultScreen(
                question = currentQuestion,
                part = currentPart,
                result = currentResult,
                questionIndex = currentQuestionIndex + 1,
                totalQuestions = viewModel.totalQuestions,
                onNext = { viewModel.dismissResult() },
                onRedo = { viewModel.redoCurrentQuestion() },
            )

            ExamPhase.FINISHED -> MockExamSummaryScreen(
                overallScore = overallScore,
                partAverages = viewModel.partAverages,
                scores = examScores,
                onRetry = { viewModel.loadExamQuestions() },
                onDismiss = onBack,
            )

            else -> ExamContent(
                currentPart = currentPart,
                currentQuestionIndex = currentQuestionIndex,
                totalQuestions = viewModel.totalQuestions,
                currentQuestion = currentQuestion,
                subQuestions = subQuestions,
                isRecording = isRecording,
                isPaused = isPaused,
                remainingTime = remainingTime,
                formattedTime = viewModel.formattedTime,
                progress = viewModel.progress,
                errorMessage = errorMessage,
                waveformData = waveformData,
                onTogglePause = { viewModel.togglePause() },
                onStartRecording = { viewModel.startRecording() },
                onStopRecording = { viewModel.stopRecording() },
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("正在加载题目...", style = SpBodyMedium, color = SpTextSecondary)
    }
}

@Composable
private fun ExamContent(
    currentPart: Int,
    currentQuestionIndex: Int,
    totalQuestions: Int,
    currentQuestion: String,
    subQuestions: List<String>,
    isRecording: Boolean,
    isPaused: Boolean,
    remainingTime: Int,
    formattedTime: String,
    progress: Float,
    errorMessage: String?,
    waveformData: List<Float>,
    onTogglePause: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 进度 + 暂停 + 计时
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "题目 ${currentQuestionIndex + 1}/$totalQuestions",
                    style = SpCaption,
                    color = SpTextSecondary,
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = onTogglePause, modifier = Modifier.size(24.dp)) {
                    Icon(
                        if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        null,
                        tint = SpTextSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Timer, null,
                    tint = if (remainingTime < 30) SpError else SpTextSecondary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    formattedTime,
                    style = SpBodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = if (remainingTime < 30) SpError else SpTextSecondary,
                )
            }

            // 进度条
            Box(modifier = Modifier.fillMaxWidth().height(3.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(SpBackground),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(SpAccent),
                )
            }

            // 题目内容
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
            ) {
                Text(
                    "Part $currentPart",
                    style = SpCaption.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp),
                    color = SpAccent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SpAccent.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    currentQuestion,
                    style = SpBodyLarge,
                    color = SpTextPrimary,
                    lineHeight = SpBodyLarge.lineHeight,
                )
                if (subQuestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    subQuestions.forEach { sub ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text("\u2022", style = SpBodyMedium, color = SpAccent)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(sub, style = SpBodyMedium, color = SpTextSecondary)
                        }
                    }
                }
            }

            // 错误提示
            errorMessage?.let {
                Text(it, style = SpCaption, color = SpError, modifier = Modifier.padding(horizontal = 20.dp))
            }

            // 波形
            if (isRecording) {
                WaveformView(
                    data = waveformData,
                    barColor = SpAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(30.dp)
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 8.dp),
                )
            }

            // 录音按钮
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
            ) {
                RecordButton(
                    isRecording = isRecording,
                    onClick = {
                        if (isRecording) onStopRecording() else onStartRecording()
                    },
                )
            }
        }

        // 暂停蒙层
        if (isPaused) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpBlack.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Pause,
                        null,
                        tint = SpWhite,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("考试已暂停", style = SpTitleMedium, color = SpWhite)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onTogglePause,
                        colors = ButtonDefaults.buttonColors(containerColor = SpWhite),
                    ) {
                        Text(
                            "继续",
                            style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SpAccent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EvaluatingContent(progress: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(20.dp))
        Text(progress, style = SpBodyMedium, color = SpTextSecondary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("请稍候，AI 正在全面分析您的回答...", style = SpCaption, color = SpTextSecondary.copy(alpha = 0.7f))
    }
}

@Composable
private fun SectionTransitionContent(
    fromPart: Int,
    toPart: Int,
    onContinue: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Filled.ArrowForward,
            null,
            tint = SpAccent,
            modifier = Modifier.size(56.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Part $fromPart 完成", style = SpTitleMedium, color = SpTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("即将进入 Part $toPart", style = SpBodyMedium, color = SpTextSecondary)
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onContinue,
            colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
        ) {
            Text(
                "继续",
                style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = SpWhite,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
    }
}
