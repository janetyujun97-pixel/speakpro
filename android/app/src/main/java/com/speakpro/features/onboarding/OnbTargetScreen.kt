@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.speakpro.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.data.models.OnbExamType
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import com.speakpro.features.auth.SpPrimaryButton
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun OnbTargetScreen(vm: OnboardingViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val examType by vm.examType.collectAsState()
    val target by vm.targetScore.collectAsState()

    val isToefl = examType == OnbExamType.TOEFL
    val min = if (isToefl) 0.0 else 4.0
    val max = if (isToefl) 30.0 else 9.0

    Column(modifier = Modifier.fillMaxSize().background(SpBackground)) {
        StepBar(step = 2, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(12.dp))
            Eyebrow("YOUR TARGET · 02")
            Spacer(Modifier.height(16.dp))
            Text("Aim for", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 34.sp)

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    target?.let { if (isToefl) "${it.toInt()}" else "%.1f".format(it) } ?: "—",
                    color = SpAccent,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 68.sp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (isToefl) "SPEAKING" else "BAND",
                    color = SpMuted,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                if (isToefl) "目标越具体，计划越可落地。口语 26 对应多数研究生项目最低要求。"
                else "目标越具体，计划越可落地。Band 7.0 对应多数研究生申请的最低要求。",
                color = SpMuted,
                fontSize = 13.sp,
            )
        }

        // scale
        ScaleBar(
            min = min, max = max, toefl = isToefl,
            value = target,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 44.dp),
        ) { newValue ->
            vm.setTargetScore(newValue)
        }

        // quick chips
        Chips(isToefl = isToefl, target = target) { v ->
            vm.setTargetScore(v)
        }

        Spacer(Modifier.weight(1f))

        SpPrimaryButton(
            text = "继续 · 设置考期",
            enabled = target != null && vm.isTargetValid(target!!),
            modifier = Modifier.padding(24.dp),
        ) {
            vm.patchCurrent()
            onContinue()
        }
    }
}

@Composable
private fun ScaleBar(
    min: Double,
    max: Double,
    toefl: Boolean,
    value: Double?,
    modifier: Modifier = Modifier,
    onChange: (Double) -> Unit,
) {
    val ticks = if (toefl) {
        (0..30 step 2).map { it.toDouble() }
    } else {
        (0..10).map { min + it * 0.5 }
    }

    Box(modifier = modifier
        .fillMaxWidth()
        .height(56.dp)
        .pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val raw = min + (max - min) * (offset.x / size.width)
                    onChange(snap(raw, toefl))
                },
            ) { change, _ ->
                val raw = min + (max - min) * (change.position.x / size.width)
                onChange(snap(raw, toefl))
            }
        }
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                val raw = min + (max - min) * (offset.x / size.width)
                onChange(snap(raw, toefl))
            }
        },
    ) {
        ticks.forEachIndexed { i, v ->
            val major = if (toefl) v.toInt() % 5 == 0 else v % 1.0 == 0.0
            val isTarget = value != null && abs(value - v) < 1e-3
            val xFrac = (v - min) / (max - min)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                TickAndLabel(
                    majorTick = major,
                    isTarget = isTarget,
                    label = if (major) {
                        if (toefl) v.toInt().toString()
                        else if (v % 1.0 == 0.0) "%.0f".format(v) else "%.1f".format(v)
                    } else null,
                    xFraction = xFrac.toFloat(),
                )
            }
        }
    }
}

@Composable
private fun TickAndLabel(
    majorTick: Boolean,
    isTarget: Boolean,
    label: String?,
    xFraction: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(xFraction.coerceIn(0f, 1f))
                .height(1.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(if (isTarget) 3.dp else 1.dp)
                    .height(if (majorTick) (if (isTarget) 32.dp else 20.dp) else 12.dp)
                    .background(if (isTarget) SpAccent else SpLine),
            )
        }
        if (label != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(xFraction.coerceIn(0f, 1f))
                    .height(56.dp),
            ) {
                Text(
                    label,
                    color = if (isTarget) SpAccent else SpMuted,
                    fontFamily = FraunceFamily,
                    fontSize = 11.sp,
                    fontWeight = if (isTarget) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 0.dp, bottom = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun Chips(isToefl: Boolean, target: Double?, onPick: (Double) -> Unit) {
    val items: List<Pair<String, Double>> = if (isToefl) {
        listOf("20" to 20.0, "23" to 23.0, "26" to 26.0, "28" to 28.0)
    } else {
        listOf("6.0" to 6.0, "6.5" to 6.5, "7.0" to 7.0, "7.5" to 7.5, "8.0+" to 8.0)
    }
    FlowRow(
        modifier = Modifier.padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (label, v) ->
            val isSel = target != null && abs(target - v) < 1e-3
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(CircleShape)
                    .background(if (isSel) SpPrimary else SpIvory)
                    .border(1.dp, if (isSel) SpPrimary else SpLine, CircleShape)
                    .clickable { onPick(v) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    label,
                    color = if (isSel) SpIvory else SpPrimary,
                    fontFamily = FraunceFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

private fun snap(raw: Double, toefl: Boolean): Double =
    if (toefl) raw.roundToInt().toDouble()
    else (raw * 2).roundToInt() / 2.0
