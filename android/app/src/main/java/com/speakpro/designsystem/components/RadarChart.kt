package com.speakpro.designsystem.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpTextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Canvas 雷达图 / 蛛网图组件
 *
 * 绘制同心多边形网格，并在其上叠加数据多边形（30% 透明度填充 + 描边）。
 * 周围标注维度标签。
 *
 * @param scores      维度名 → 分数 (0~maxValue)
 * @param maxValue    最大分值，默认 100
 * @param accentColor 数据多边形颜色
 * @param size        组件尺寸
 * @param modifier    Modifier
 */
@Composable
fun RadarChart(
    scores: Map<String, Double>,
    modifier: Modifier = Modifier,
    maxValue: Double = 100.0,
    accentColor: Color = SpAccent,
    size: Dp = 200.dp,
) {
    val labels = scores.keys.toList()
    val values = scores.values.toList()
    val n = labels.size
    if (n < 3) return // 至少需要 3 个维度

    Canvas(modifier = modifier.size(size)) {
        val centerX = this.size.width / 2f
        val centerY = this.size.height / 2f
        val radius = (this.size.minDimension / 2f) * 0.72f // 留出标签空间
        val angleStep = (2 * PI / n).toFloat()
        val startAngle = (-PI / 2).toFloat() // 从顶部开始

        // ── 同心多边形网格 (4 层) ──
        val gridLevels = 4
        for (level in 1..gridLevels) {
            val r = radius * level / gridLevels
            val gridPath = Path()
            for (i in 0 until n) {
                val angle = startAngle + i * angleStep
                val x = centerX + r * cos(angle)
                val y = centerY + r * sin(angle)
                if (i == 0) gridPath.moveTo(x, y)
                else gridPath.lineTo(x, y)
            }
            gridPath.close()

            drawPath(
                path = gridPath,
                color = SpTextSecondary.copy(alpha = 0.15f),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = if (level < gridLevels) PathEffect.dashPathEffect(
                        floatArrayOf(4.dp.toPx(), 4.dp.toPx()),
                    ) else null,
                ),
            )
        }

        // ── 从中心到顶点的辐射线 ──
        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            drawLine(
                color = SpTextSecondary.copy(alpha = 0.1f),
                start = Offset(centerX, centerY),
                end = Offset(x, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        // ── 数据多边形 ──
        val dataPath = Path()
        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val value = (values[i] / maxValue).toFloat().coerceIn(0f, 1f)
            val r = radius * value
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            if (i == 0) dataPath.moveTo(x, y)
            else dataPath.lineTo(x, y)
        }
        dataPath.close()

        // 填充
        drawPath(
            path = dataPath,
            color = accentColor.copy(alpha = 0.3f),
        )

        // 描边
        drawPath(
            path = dataPath,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
        )

        // ── 数据点 ──
        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val value = (values[i] / maxValue).toFloat().coerceIn(0f, 1f)
            val r = radius * value
            val x = centerX + r * cos(angle)
            val y = centerY + r * sin(angle)
            drawCircle(
                color = accentColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(x, y),
            )
        }

        // ── 标签 ──
        val labelRadius = radius + 18.dp.toPx()
        for (i in 0 until n) {
            val angle = startAngle + i * angleStep
            val x = centerX + labelRadius * cos(angle)
            val y = centerY + labelRadius * sin(angle)

            drawContext.canvas.nativeCanvas.drawText(
                labels[i],
                x,
                y + 4.dp.toPx(), // 微调垂直居中
                android.graphics.Paint().apply {
                    textSize = 11.dp.toPx()
                    color = android.graphics.Color.parseColor("#6B7280")
                    textAlign = when {
                        cos(angle) < -0.1 -> android.graphics.Paint.Align.RIGHT
                        cos(angle) > 0.1 -> android.graphics.Paint.Align.LEFT
                        else -> android.graphics.Paint.Align.CENTER
                    }
                    isAntiAlias = true
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun RadarChartPreview() {
    Column(modifier = Modifier.padding(24.dp)) {
        RadarChart(
            scores = mapOf(
                "发音" to 85.0,
                "流利度" to 72.0,
                "语法" to 68.0,
                "内容" to 78.0,
                "词汇" to 80.0,
            ),
            modifier = Modifier.size(200.dp),
        )
    }
}
