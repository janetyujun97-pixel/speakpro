package com.speakpro.designsystem.components.states

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.core.network.NetworkMonitor
import com.speakpro.designsystem.theme.SpAccentWarm
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpPrimary

/**
 * 顶部深色 offline sticky bar —— 由 MainActivity 把 NetworkMonitor
 * 传进来；非 connected 时滑入。
 */
@Composable
fun OfflineBanner(monitor: NetworkMonitor, modifier: Modifier = Modifier) {
    val connected by monitor.isConnected.collectAsState()
    AnimatedVisibility(
        visible = !connected,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(SpPrimary)
                .padding(vertical = 8.dp, horizontal = 16.dp),
        ) {
            Icon(
                Icons.Filled.WifiOff,
                contentDescription = null,
                tint = SpAccentWarm,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.size(8.dp))
            Text(
                "OFFLINE · 无网络连接",
                color = SpIvory,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
            )
        }
    }
}
