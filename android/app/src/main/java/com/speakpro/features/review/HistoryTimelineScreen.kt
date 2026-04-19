package com.speakpro.features.review

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.speakpro.data.models.PracticeSessionListItem
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentSoft
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryTimelineScreen(
    onBack: () -> Unit,
    viewModel: HistoryTimelineViewModel = hiltViewModel(),
) {
    val groups by viewModel.groups.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    var currentPlayingId by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) currentPlayingId = null
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(Unit) { viewModel.load() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = SpBackground,
        topBar = {
            TopAppBar(
                title = { Text("历史回听", color = SpPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpBackground),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).background(SpBackground)) {
            when {
                isLoading && groups.isEmpty() ->
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                groups.isEmpty() ->
                    Text(
                        "还没有练习记录",
                        color = SpMuted,
                        modifier = Modifier.align(Alignment.Center),
                    )
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                        vertical = 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    items(groups) { group ->
                        Column {
                            Eyebrow(group.dateLabel)
                            Spacer(Modifier.height(8.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SpIvory)
                                    .border(1.dp, SpLine, RoundedCornerShape(12.dp)),
                            ) {
                                group.items.forEachIndexed { idx, item ->
                                    HistoryRow(
                                        item = item,
                                        isPlayingThisItem = currentPlayingId == item.id && isPlaying,
                                        onPlayPause = {
                                            if (currentPlayingId == item.id) {
                                                if (isPlaying) player.pause() else player.play()
                                            } else {
                                                scope.launch {
                                                    val url = viewModel.resolveAudioUrl(item)
                                                    if (!url.isNullOrEmpty()) {
                                                        player.setMediaItem(MediaItem.fromUri(url))
                                                        player.prepare()
                                                        player.play()
                                                        currentPlayingId = item.id
                                                    }
                                                }
                                            }
                                        },
                                    )
                                    if (idx < group.items.size - 1) {
                                        Box(
                                            Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .padding(start = 16.dp)
                                                .background(SpLine),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(
    item: PracticeSessionListItem,
    isPlayingThisItem: Boolean,
    onPlayPause: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(14.dp),
    ) {
        // play button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isPlayingThisItem) SpAccent else SpAccentSoft)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlayingThisItem) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = if (isPlayingThisItem) SpIvory else SpAccent,
                modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                item.question?.promptText ?: modeLabel(item.mode),
                color = SpPrimary,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Row {
                Text(
                    modeLabel(item.mode),
                    color = SpMuted,
                    fontSize = 11.sp,
                )
                if (item.durationSec != null && item.durationSec > 0) {
                    Text(
                        " · ${formatDuration(item.durationSec)}",
                        color = SpMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }
        item.overallScore?.let {
            Text(
                "%.1f".format(it),
                color = scoreColor(it),
                fontFamily = FraunceFamily,
                fontSize = 18.sp,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

private fun scoreColor(s: Double): Color = when {
    s >= 80 -> SpMoss
    s >= 60 -> SpPrimary
    else -> SpAccent
}

private fun modeLabel(m: String): String = when (m) {
    "conversation" -> "AI 对话"
    "read_aloud"   -> "朗读"
    "follow_read"  -> "跟读"
    "mock_exam"    -> "模考"
    "baseline"     -> "基线测试"
    else           -> m
}

private fun formatDuration(sec: Int): String {
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "${m} 分 %02d 秒".format(s) else "${s} 秒"
}
