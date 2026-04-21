package com.speakpro.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.core.audio.AudioRecorder
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import kotlinx.coroutines.delay

@Composable
fun OnbBaselineRecordingScreen(
    vm: OnboardingViewModel,
    onBack: () -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val recorder = remember { AudioRecorder(context) }
    val waveform by recorder.waveformData.collectAsState()
    val isRecording by recorder.isRecording.collectAsState()

    var elapsed by remember { mutableFloatStateOf(0f) }
    val duration = 30f
    var isUploading by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }

    // 权限 launcher —— 未授权时拉系统弹窗
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            try {
                recorder.startRecording()
            } catch (e: Exception) {
                android.util.Log.e("OnbBaseline", "startRecording failed: ${e.message}")
                permissionDenied = true
            }
        } else {
            permissionDenied = true
        }
    }

    // 进屏：检查权限 → 未授权拉弹窗；授权了直接录
    LaunchedEffect(Unit) {
        if (recorder.hasRecordPermission()) {
            try {
                recorder.startRecording()
            } catch (e: Exception) {
                android.util.Log.e("OnbBaseline", "startRecording failed: ${e.message}")
                permissionDenied = true
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    // Timer
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (elapsed < duration && isRecording) {
                delay(100)
                elapsed += 0.1f
            }
            if (elapsed >= duration) finishEarly(recorder, vm, onFinished) { isUploading = it }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (recorder.isRecording.value) recorder.stopRecording()
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(SpBackground)) {
        StepBar(step = 6, onBack = onBack)

        // header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(SpAccent))
                Spacer(Modifier.size(8.dp))
                Text(
                    "RECORDING · 录音中",
                    color = SpAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.2.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                String.format("0:%02d", elapsed.toInt()),
                color = SpPrimary,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 18.sp,
            )
        }

        Text(
            "\"Tell me about a place you\nrecently visited…\"",
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 22.sp,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        // waveform
        Waveform(
            samples = waveform,
            progress = elapsed / duration,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        // Progress bar
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("0:00", color = SpMuted, fontSize = 10.sp)
                Text(
                    "0:${String.format("%02d", elapsed.toInt())} / 0:30",
                    color = SpAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text("0:30", color = SpMuted, fontSize = 10.sp)
            }
            Spacer(Modifier.size(6.dp))
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(SpLine)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = (elapsed / duration).coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(SpAccent),
                )
            }
        }

        // hint card
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SpIvory)
                .border(1.dp, SpLine, RoundedCornerShape(10.dp))
                .padding(14.dp),
        ) {
            Eyebrow("LIVE · 实时反馈", SpMoss)
            Spacer(Modifier.size(8.dp))
            Text(
                when {
                    isUploading -> "正在上传并分析基线录音…"
                    isRecording -> "你正在录音中 · 说出你最近去过的一个地方"
                    permissionDenied -> "未获得麦克风权限，请在系统设置里开启"
                    else -> "录音已结束"
                },
                color = if (permissionDenied) SpAccent else SpPrimary,
                fontSize = 13.sp,
            )
        }

        Spacer(Modifier.size(20.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp).fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(SpIvory)
                    .border(1.dp, SpLine, CircleShape)
                    .clickable {
                        finishEarly(recorder, vm, onFinished) { isUploading = it }
                    },
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, tint = SpPrimary, modifier = Modifier.size(16.dp))
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(SpPrimary)
                    .clickable {
                        finishEarly(recorder, vm, onFinished) { isUploading = it }
                    },
            ) {
                Text(
                    "提前结束 · 分析结果",
                    color = SpIvory,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun Waveform(samples: List<Float>, progress: Float, modifier: Modifier = Modifier) {
    val barCount = 40
    val reduced = if (samples.isEmpty()) List(barCount) { 0.3f } else reduce(samples, barCount)
    val active = (progress.coerceIn(0f, 1f) * barCount).toInt()
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().height(70.dp),
    ) {
        reduced.forEachIndexed { i, sample ->
            val h = (sample.coerceIn(0f, 1f) * 52f + 8f).dp
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(h)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i < active) SpAccent else SpLine),
            )
        }
    }
}

private fun reduce(samples: List<Float>, target: Int): List<Float> {
    if (samples.isEmpty()) return List(target) { 0f }
    if (samples.size <= target) return samples
    val chunk = samples.size / target
    return (0 until target).map { i ->
        val start = i * chunk
        val end = (start + chunk).coerceAtMost(samples.size)
        samples.subList(start, end).average().toFloat()
    }
}

private fun finishEarly(
    recorder: AudioRecorder,
    vm: OnboardingViewModel,
    onFinished: () -> Unit,
    setUploading: (Boolean) -> Unit,
) {
    if (!recorder.isRecording.value) return
    val file = recorder.stopRecording()
    setUploading(true)

    // 一期：仅把文件名当占位 URL 传给服务端；后续 PR 接通 Go /practice/audio 上传拿真 sessionId
    val audioUrl = file?.name
    vm.submitBaseline(
        sessionId = null,
        audioUrl = audioUrl,
        transcript = null,
        localAudioFile = file,
    )

    // finalize 会生成计划；错开一个 tick 让 submitBaseline 的协程先起步
    vm.finalize()

    onFinished()
}
