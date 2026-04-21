package com.speakpro.features.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpAccentWarm
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * 麦克风权限前置页 —— 展示权限理由 + trust 卡，用户点击"同意"再触发真正请求。
 */
@Composable
fun PermissionPrimeScreen(onComplete: (granted: Boolean) -> Unit) {

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted -> onComplete(granted) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 28.dp),
    ) {
        // masthead
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Eyebrow("PERMISSION · 1 / 2")
            Spacer(Modifier.weight(1f))
            Text(
                "跳过",
                color = SpMuted,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(8.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        // illustrative mark
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // outer pulse rings
            Box(
                modifier = Modifier
                    .size(174.dp)
                    .clip(CircleShape)
                    .border(1.dp, SpAccentWarm.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(CircleShape)
                        .border(1.dp, SpAccentWarm.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(SpPrimary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = null,
                            tint = SpAccentWarm,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(36.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("We need to", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 28.sp)
            Text(
                "hear you.",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 28.sp,
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            "SpeakPro 只在你主动按下录音时才会采集麦克风音频 ——\n所有分析都在云端完成，不用于训练第三方 AI 模型。",
            color = SpMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))

        TrustCard()

        Spacer(Modifier.weight(1f))

        SpPrimaryButton(
            text = "同意并继续",
            onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) },
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TrustCard() {
    val points = listOf(
        "你可以随时在系统设置里撤销",
        "录音文件默认保留 30 天后自动删除",
        "端到端加密传输 · AES-256",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SpIvory)
            .border(1.dp, SpLine, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        points.forEachIndexed { idx, text ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    String.format("%02d", idx + 1),
                    color = SpAccent,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 11.sp,
                    modifier = Modifier.size(width = 24.dp, height = 16.dp),
                )
                Spacer(Modifier.size(10.dp))
                Text(text, color = SpPrimary, fontSize = 12.sp)
            }
            if (idx < points.size - 1) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
