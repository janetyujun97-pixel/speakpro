package com.speakpro.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpTextSecondary
import kotlin.math.max

/**
 * 音频波形可视化组件
 *
 * 将音频电平数据渲染为垂直条形图。数据会自动归一化到组件高度的 80%。
 * 当 [data] 为空时显示虚线占位效果。
 *
 * @param data       音频电平数据
 * @param barColor   条形颜色
 * @param barWidth   单根条形宽度
 * @param spacing    条形之间的间距
 * @param modifier   Modifier
 */
@Composable
fun WaveformView(
    data: List<Float>,
    modifier: Modifier = Modifier,
    barColor: Color = SpAccent,
    barWidth: Dp = 3.dp,
    spacing: Dp = 2.dp,
) {
    Canvas(modifier = modifier) {
        val barWidthPx = barWidth.toPx()
        val spacingPx = spacing.toPx()
        val totalBarWidth = barWidthPx + spacingPx
        val maxBars = (size.width / totalBarWidth).toInt()
        val minBarHeight = 2.dp.toPx()
        val cornerRadiusPx = barWidthPx / 2f

        if (data.isEmpty()) {
            drawEmptyState(
                maxBars = maxBars.coerceAtMost(40),
                barWidthPx = barWidthPx,
                spacingPx = spacingPx,
                minBarHeight = minBarHeight,
                cornerRadiusPx = cornerRadiusPx,
                color = barColor.copy(alpha = 0.3f),
            )
            return@Canvas
        }

        // 取最近 maxBars 个采样
        val samples = if (data.size <= maxBars) {
            data
        } else {
            data.takeLast(maxBars)
        }

        // 归一化：最大值映射到高度的 80%
        val maxVal = samples.maxOrNull() ?: 0.001f
        val scale = if (maxVal > 0f) (size.height * 0.8f) / maxVal else 1f

        // 居中绘制
        val totalWidth = samples.size * totalBarWidth - spacingPx
        val startX = (size.width - totalWidth) / 2f

        samples.forEachIndexed { index, sample ->
            val barHeight = max(minBarHeight, sample * scale)
            val x = startX + index * totalBarWidth
            val y = (size.height - barHeight) / 2f

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
            )
        }
    }
}

/**
 * 空数据状态：绘制虚线占位模式（交替高低条）
 */
private fun DrawScope.drawEmptyState(
    maxBars: Int,
    barWidthPx: Float,
    spacingPx: Float,
    minBarHeight: Float,
    cornerRadiusPx: Float,
    color: Color,
) {
    val totalBarWidth = barWidthPx + spacingPx
    val totalWidth = maxBars * totalBarWidth - spacingPx
    val startX = (size.width - totalWidth) / 2f
    val centerY = size.height / 2f

    for (i in 0 until maxBars) {
        val barHeight = if (i % 2 == 0) minBarHeight else minBarHeight * 0.5f
        val x = startX + i * totalBarWidth
        val y = centerY - barHeight / 2f

        drawRoundRect(
            color = color,
            topLeft = Offset(x, y),
            size = Size(barWidthPx, barHeight),
            cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F7F4)
@Composable
private fun WaveformViewPreview() {
    Column(
        modifier = Modifier.padding(16.dp),
    ) {
        // 空数据
        WaveformView(
            data = emptyList(),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 小幅值
        WaveformView(
            data = (0 until 50).map { (Math.random() * 0.05f).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 正常波形
        WaveformView(
            data = (0 until 50).map { (Math.random() * 0.9f + 0.1f).toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
        )
    }
}
