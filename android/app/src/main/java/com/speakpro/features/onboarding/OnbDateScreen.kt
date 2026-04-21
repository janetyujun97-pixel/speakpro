package com.speakpro.features.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import com.speakpro.features.auth.SpPrimaryButton
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun OnbDateScreen(vm: OnboardingViewModel, onBack: () -> Unit, onContinue: () -> Unit) {
    val selected by vm.examDate.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }

    Column(modifier = Modifier.fillMaxSize().background(SpBackground)) {
        StepBar(step = 3, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Spacer(Modifier.size(12.dp))
            Eyebrow("EXAM DATE · 03")
            Spacer(Modifier.size(16.dp))
            Text("When is the", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 34.sp)
            Text(
                "real test?",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 34.sp,
            )
        }

        // Hero card
        HeroCard(selected)

        Column(modifier = Modifier.padding(horizontal = 24.dp).weight(1f)) {
            // month nav
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = SpMuted,
                    modifier = Modifier.clickable { month = month.minusMonths(1) },
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)} ${month.year}",
                    color = SpPrimary,
                    fontFamily = FraunceFamily,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = SpMuted,
                    modifier = Modifier.clickable { month = month.plusMonths(1) },
                )
            }
            Spacer(Modifier.size(12.dp))

            // weekday header
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { w ->
                    Text(
                        w,
                        color = SpMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.size(4.dp))

            // days
            val cells = monthCells(month)
            LazyVerticalGrid(columns = GridCells.Fixed(7)) {
                items(cells) { day ->
                    if (day == null) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    } else {
                        val sel = selected == day
                        val past = day.isBefore(LocalDate.now())
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (sel) SpAccent else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable(enabled = !past) { vm.setExamDate(day) }
                                .alpha(if (past) 0.4f else 1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "${day.dayOfMonth}",
                                color = if (sel) SpIvory else (if (past) SpMuted else SpPrimary),
                                fontFamily = FraunceFamily,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }

        SpPrimaryButton(
            text = "继续 · 水平自评",
            enabled = selected != null,
            modifier = Modifier.padding(24.dp),
        ) {
            vm.patchCurrent()
            onContinue()
        }
    }
}

@Composable
private fun HeroCard(selected: LocalDate?) {
    val today = LocalDate.now()
    val days = selected?.let { java.time.temporal.ChronoUnit.DAYS.between(today, it) }?.coerceAtLeast(0L)
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 18.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SpPrimary)
            .padding(18.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "SELECTED",
                color = SpIvory.copy(alpha = 0.55f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
            Spacer(Modifier.size(6.dp))
            Text(
                selected?.let { "${it.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${it.dayOfMonth}, ${it.year}" }
                    ?: "请选择考试日",
                color = SpIvory,
                fontFamily = FraunceFamily,
                fontSize = 26.sp,
            )
            Spacer(Modifier.size(2.dp))
            Text(
                selected?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.ENGLISH)?.uppercase() ?: "TAP A DATE BELOW",
                color = SpIvory.copy(alpha = 0.55f),
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                days?.toString() ?: "—",
                color = SpIvory,
                fontFamily = FraunceFamily,
                fontSize = 40.sp,
            )
            Text(
                "DAYS LEFT",
                color = SpIvory.copy(alpha = 0.55f),
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.5.sp,
            )
        }
    }
}

private fun monthCells(month: YearMonth): List<LocalDate?> {
    val firstDay = month.atDay(1)
    // Sunday = 7 in Java time. We want Sunday first.
    val leading = firstDay.dayOfWeek.value % 7
    val days = month.lengthOfMonth()
    val cells = MutableList<LocalDate?>(leading) { null }
    for (d in 1..days) cells.add(month.atDay(d))
    while (cells.size < 42) cells.add(null)
    return cells
}

