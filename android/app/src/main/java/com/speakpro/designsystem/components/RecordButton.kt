package com.speakpro.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpWhite

/**
 * 录音按钮组件 — 带脉冲光圈动画
 *
 * 录音中显示红色并带有脉冲光圈效果；空闲时显示强调色。
 *
 * @param isRecording  是否正在录音
 * @param onClick      点击回调
 * @param size         按钮直径
 * @param modifier     Modifier
 */
@Composable
fun RecordButton(
    isRecording: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 72.dp,
) {
    val buttonColor by animateColorAsState(
        targetValue = if (isRecording) SpError else SpAccent,
        animationSpec = tween(durationMillis = 300),
        label = "record_button_color",
    )

    // 脉冲动画（无限循环）
    val infiniteTransition: InfiniteTransition = rememberInfiniteTransition(
        label = "record_pulse",
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_alpha",
    )

    // 外层容器（包含脉冲光圈空间）
    val outerSize = size * 1.6f

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(outerSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        val sizePx = size

        // 脉冲光圈（仅录音时显示）
        if (isRecording) {
            Canvas(modifier = Modifier.size(outerSize)) {
                val haloRadius = (sizePx.toPx() / 2f) * pulseScale
                drawCircle(
                    color = buttonColor.copy(alpha = pulseAlpha),
                    radius = haloRadius,
                )
            }
        }

        // 主按钮圆形
        Canvas(modifier = Modifier.size(size)) {
            // 阴影
            drawCircle(
                color = buttonColor.copy(alpha = 0.4f),
                radius = this.size.minDimension / 2f + 4.dp.toPx(),
            )
            // 主体
            drawCircle(
                color = buttonColor,
                radius = this.size.minDimension / 2f,
            )
        }

        // 图标
        Icon(
            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
            contentDescription = if (isRecording) "停止录音" else "开始录音",
            tint = SpWhite,
            modifier = Modifier.size(size * 0.35f),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecordButtonPreview() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RecordButton(isRecording = false, onClick = {})
        Spacer(modifier = Modifier.height(24.dp))
        RecordButton(isRecording = true, onClick = {})
    }
}
