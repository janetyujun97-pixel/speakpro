package com.speakpro.designsystem.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleMedium
import com.speakpro.designsystem.theme.SpWarning

/**
 * 环形分数展示组件
 *
 * 用于显示 0~100 分的评分，中心显示数字 + "分"，外圈为动画进度环。
 *
 * @param score     分数 (0~100)
 * @param color     进度环颜色
 * @param lineWidth 进度环线宽
 * @param size      组件整体尺寸
 * @param modifier  Modifier
 */
@Composable
fun ScoreRing(
    score: Int,
    modifier: Modifier = Modifier,
    color: Color = SpAccent,
    lineWidth: Dp = 10.dp,
    size: Dp = 100.dp,
) {
    // 动画目标值
    var targetProgress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 1000),
        label = "score_ring_progress",
    )

    LaunchedEffect(score) {
        targetProgress = score.coerceIn(0, 100) / 100f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size),
    ) {
        // Canvas 绘制背景环 + 进度环
        Canvas(modifier = Modifier.size(size)) {
            val strokeWidth = lineWidth.toPx()
            val radius = (this.size.minDimension - strokeWidth) / 2f

            // 背景环
            drawCircle(
                color = color.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(width = strokeWidth),
            )

            // 进度环（从12点方向顺时针绘制）
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        // 中心分数文本
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${score.coerceIn(0, 100)}",
                style = SpTitleMedium,
                color = SpTextPrimary,
            )
            Text(
                text = "分",
                style = SpBodySmall,
                color = SpTextSecondary,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreRingPreview() {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp),
        modifier = Modifier.then(
            Modifier
        ),
    ) {
        ScoreRing(score = 85, color = SpSuccess)
        ScoreRing(score = 62, color = SpWarning, lineWidth = 8.dp, size = 80.dp)
        ScoreRing(score = 30, color = SpError, lineWidth = 6.dp, size = 60.dp)
    }
}
