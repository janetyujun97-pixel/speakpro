package com.speakpro.features.review

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.data.models.NotificationItem
import com.speakpro.data.models.NotificationKind
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentWarm
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenPrefs: () -> Unit,
    viewModel: NotificationsViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = SpBackground,
        topBar = {
            TopAppBar(
                title = { Text("通知", color = SpPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpPrimary)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, null, tint = SpPrimary)
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("全部已读") },
                                onClick = { viewModel.markAllRead(); menuExpanded = false },
                            )
                            DropdownMenuItem(
                                text = { Text("免打扰设置") },
                                onClick = { onOpenPrefs(); menuExpanded = false },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpBackground),
            )
        },
    ) { padding ->
        Box(
            Modifier.fillMaxSize().padding(padding).background(SpBackground),
        ) {
            when {
                isLoading && items.isEmpty() ->
                    com.speakpro.designsystem.components.states.SkeletonState(
                        headerTitle = "NOTIFICATIONS · 加载中",
                        cardCount = 4,
                    )
                items.isEmpty() ->
                    com.speakpro.designsystem.components.states.EmptyState(
                        eyebrow = "NO NOTIFICATIONS · 通知空空",
                        headline = "Quiet afternoon,",
                        headlineItalic = "nothing to report.",
                        message = "老师还没有新动作，也没有系统提醒。\n先去练一轮吧？",
                        footer = "EMPTY STATE",
                        footerNumber = "N° NOTIF",
                    )
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        vertical = 8.dp,
                    ),
                ) {
                    items(items, key = { it.id }) { n ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.markRead(n.id) }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                        ) {
                            KindIcon(NotificationKind.fromValue(n.kind))
                            Spacer(Modifier.size(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row {
                                    Text(
                                        n.title,
                                        color = SpPrimary,
                                        fontSize = 14.sp,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (!n.isRead) {
                                        Box(
                                            Modifier.size(6.dp).clip(CircleShape).background(SpAccent),
                                        )
                                    }
                                }
                                Text(n.body, color = SpMuted, fontSize = 11.sp, maxLines = 3)
                                Text(relativeTime(n.createdAt), color = SpMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KindIcon(kind: NotificationKind) {
    val color = when (kind) {
        NotificationKind.HOMEWORK -> SpAccent
        NotificationKind.FEEDBACK -> SpMoss
        NotificationKind.STREAK   -> SpAccentWarm
        NotificationKind.REMINDER -> SpMuted
        else                      -> SpPrimary
    }
    val icon: ImageVector = when (kind) {
        NotificationKind.HOMEWORK -> Icons.Filled.TextSnippet
        NotificationKind.FEEDBACK -> Icons.Filled.Check
        NotificationKind.STREAK   -> Icons.Filled.LocalFireDepartment
        NotificationKind.REMINDER -> Icons.Filled.Schedule
        else                      -> Icons.Filled.Notifications
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color)
    }
}

private fun relativeTime(iso: String): String {
    return try {
        val ms = Instant.parse(iso).toEpochMilli()
        DateUtils.getRelativeTimeSpanString(
            ms, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    } catch (_: Exception) {
        iso
    }
}

// ============================================================================
// 偏好设置页
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationPrefsScreen(
    onBack: () -> Unit,
    viewModel: NotificationPrefsViewModel = hiltViewModel(),
) {
    val prefs by viewModel.prefs.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        containerColor = SpBackground,
        topBar = {
            TopAppBar(
                title = { Text("免打扰设置", color = SpPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpPrimary)
                    }
                },
                actions = {
                    IconButton(
                        enabled = !isSaving,
                        onClick = { viewModel.save(onDone = onBack) },
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "保存", tint = SpPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.size(16.dp))
            SectionHeader("免打扰时段")
            Spacer(Modifier.size(8.dp))
            PrefRow(
                label = "开始",
                value = prefs.quietStart.take(5),
                onChangeRaw = viewModel::updateQuietStart,
            )
            PrefRow(
                label = "结束",
                value = prefs.quietEnd.take(5),
                onChangeRaw = viewModel::updateQuietEnd,
            )

            Spacer(Modifier.size(24.dp))
            SectionHeader("推送通道")
            Spacer(Modifier.size(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) {
                Text("接收推送通知", color = SpPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Switch(checked = prefs.pushEnabled, onCheckedChange = viewModel::updatePushEnabled)
            }
            Text(
                "v1 仅在 App 内显示通知列表；凭证齐备后将接入系统推送。",
                color = SpMuted, fontSize = 11.sp,
            )

            if (errorMessage != null) {
                Spacer(Modifier.size(12.dp))
                Text(errorMessage!!, color = Color.Red, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(title.uppercase(), color = SpMuted, fontSize = 10.sp, letterSpacing = 2.2.sp)
}

@Composable
private fun PrefRow(label: String, value: String, onChangeRaw: (String) -> Unit) {
    // 简化的时间选择：点击进入 TimePickerDialog（系统样式）
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val parts = value.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 22
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 30
                android.app.TimePickerDialog(
                    context,
                    { _, hh, mm ->
                        onChangeRaw(String.format("%02d:%02d:00", hh, mm))
                    },
                    h, m, true,
                ).show()
            }
            .padding(vertical = 12.dp),
    ) {
        Text(label, color = SpPrimary, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(value, color = SpMuted, fontSize = 14.sp)
    }
}
