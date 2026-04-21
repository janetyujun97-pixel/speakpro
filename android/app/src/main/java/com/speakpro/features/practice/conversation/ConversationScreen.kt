package com.speakpro.features.practice.conversation

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.designsystem.components.RecordButton
import com.speakpro.designsystem.components.WaveformView
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWhite

/**
 * AI 对话练习页面
 *
 * - 顶部：考试类型 + 连接状态 + 倒计时
 * - 中间：LazyColumn 聊天气泡（考官左，学生右）
 * - 实时反馈面板（评分）
 * - 底部：波形可视化 + 录音按钮
 * - LaunchedEffect 自动连接 WebSocket
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val ctx = LocalContext.current
    val micLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> if (granted) viewModel.startRecording() }
    val tryStartRecording: () -> Unit = {
        val granted = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) viewModel.startRecording()
        else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    val messages by viewModel.messages.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val isConnecting by viewModel.isConnecting.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val scores by viewModel.scores.collectAsState()
    val remainingTime by viewModel.remainingTime.collectAsState()
    val processingStatus by viewModel.processingStatus.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val playingMessageId by viewModel.playingMessageId.collectAsState()
    val waveformData by viewModel.waveformData.collectAsState()
    val listState = rememberLazyListState()

    // 进入页面自动连接 WebSocket
    LaunchedEffect(Unit) {
        viewModel.startConversation()
    }

    // 新消息时自动滚动到底部
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        // ── 顶部栏 ──
        TopAppBar(
            title = {
                Text(
                    text = "雅思 Part 2",
                    style = SpTitleSmall,
                    color = SpPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.endConversation()
                    onBack()
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                    )
                }
            },
            actions = {
                if (isConnecting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("连接中...", style = SpCaption, color = SpTextSecondary)
                    }
                } else if (isConnected) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Timer,
                            contentDescription = "计时",
                            modifier = Modifier.size(14.dp),
                            tint = if (remainingTime < 60) SpError else SpTextSecondary,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = viewModel.formattedRemainingTime,
                            style = SpBodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = if (remainingTime < 60) SpError else SpTextSecondary,
                        )
                    }
                } else {
                    TextButton(onClick = { viewModel.startConversation() }) {
                        Text("重新连接", style = SpCaption, color = SpAccent)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpWhite),
        )

        // ── 聊天区域 ──
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                if (message.isVoiceMessage) {
                    VoiceMessageBubble(
                        message = message,
                        isPlaying = playingMessageId == message.id,
                        onPlay = { viewModel.playAudio(message) },
                        onConvertToText = { viewModel.convertToText(message.id) },
                        onDelete = { viewModel.deleteMessage(message.id) },
                    )
                } else {
                    ChatBubbleView(
                        isExaminer = message.isExaminer,
                        text = message.text,
                    )
                }
            }
        }

        // ── 实时反馈面板 ──
        if (scores.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SpWhite.copy(alpha = 0.9f))
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                scores.forEach { (key, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${value.toInt()}",
                            style = SpTitleSmall,
                            color = SpAccent,
                        )
                        Text(text = key, style = SpCaption, color = SpTextSecondary)
                    }
                }
            }
        }

        // ── 状态提示 ──
        if (processingStatus.isNotEmpty()) {
            Text(
                text = processingStatus,
                style = SpCaption,
                color = SpTextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .padding(horizontal = 20.dp),
            )
        }

        // ── 错误提示 ──
        errorMessage?.let {
            Text(
                text = it,
                style = SpCaption,
                color = SpError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }

        // ── 底部录音区域 ──
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .background(SpWhite)
                .padding(top = 8.dp),
        ) {
            WaveformView(
                data = if (isRecording) waveformData else emptyList(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(horizontal = 20.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            RecordButton(
                isRecording = isRecording,
                onClick = {
                    if (isRecording) viewModel.stopAndSendAudio()
                    else tryStartRecording()
                },
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

// ── 语音消息气泡（点击播放 + 长按菜单） ──

@Composable
private fun VoiceMessageBubble(
    message: ChatMessage,
    isPlaying: Boolean,
    onPlay: () -> Unit,
    onConvertToText: () -> Unit,
    onDelete: () -> Unit,
) {
    val isExaminer = message.isExaminer
    val bubbleColor = if (isExaminer) SpPrimary else SpAccent
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isExaminer) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top,
    ) {
        if (isExaminer) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SpPrimary.copy(alpha = 0.1f)),
            ) {
                Icon(Icons.Filled.Shield, "考官", tint = SpPrimary, modifier = Modifier.size(14.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isExaminer) Alignment.Start else Alignment.End,
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .widthIn(min = 100.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bubbleColor)
                        .clickable(onClick = onPlay)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    // 播放图标
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "停止" else "播放",
                        tint = SpWhite,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    // 时长
                    message.duration?.let { dur ->
                        if (dur > 0) {
                            Text(
                                text = "${dur.toInt()}\"",
                                style = SpBodySmall,
                                color = SpWhite.copy(alpha = 0.9f),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                    }
                    // 简化波形条
                    WaveformBars()
                }

                // 长按菜单
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    if (message.transcribedText == null) {
                        DropdownMenuItem(
                            text = { Text("转文字") },
                            leadingIcon = { Icon(Icons.Filled.TextFields, null) },
                            onClick = {
                                onConvertToText()
                                showMenu = false
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Filled.Delete, null, tint = SpError) },
                        onClick = {
                            onDelete()
                            showMenu = false
                        },
                    )
                }
            }

            // 转写文本
            message.transcribedText?.let { transcript ->
                Text(
                    text = transcript,
                    style = SpCaption,
                    color = SpTextSecondary,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SpWhite)
                        .padding(4.dp),
                )
            }
        }

        if (!isExaminer) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SpAccent.copy(alpha = 0.1f)),
            ) {
                Icon(Icons.Filled.Person, "学生", tint = SpAccent, modifier = Modifier.size(14.dp))
            }
        }
    }
}

/** 简化的波形条（静态） */
@Composable
private fun WaveformBars() {
    val heights = listOf(8f, 14f, 10f, 16f, 12f, 18f, 9f, 13f)
    Row(horizontalArrangement = Arrangement.spacedBy(1.5.dp)) {
        heights.forEach { h ->
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(SpWhite.copy(alpha = 0.6f)),
            )
        }
    }
}
