package com.speakpro.features.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccentWarm
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpPrimary
import kotlinx.coroutines.delay

/**
 * Splash —— Editorial 墨底，"Speak" + italic accent "Pro."
 * 1.2s 后触发 [onFinished]。
 */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var progress by remember { mutableFloatStateOf(0f) }
    val animated by animateFloatAsState(progress, animationSpec = tween(200), label = "loading")

    LaunchedEffect(Unit) {
        for (i in 1..12) {
            delay(100)
            progress = i / 12f
        }
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpPrimary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // editorial marks
            Row(
                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    "A DAILY EDITION",
                    color = SpIvory.copy(alpha = 0.35f),
                    fontFamily = InterFamily,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "N° 001",
                    color = SpIvory.copy(alpha = 0.35f),
                    fontFamily = InterFamily,
                    fontSize = 10.sp,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "EST. 2026 · SPEAKPRO",
                color = SpIvory.copy(alpha = 0.55f),
                fontFamily = InterFamily,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            Text(
                "Speak",
                color = SpIvory,
                fontFamily = FraunceFamily,
                fontSize = 72.sp,
            )
            Text(
                "Pro.",
                color = SpAccentWarm,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 72.sp,
                modifier = Modifier.offset(y = (-12).dp),
            )
            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(1.dp)
                    .background(SpIvory.copy(alpha = 0.35f)),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "The language of speaking,\nrehearsed in good taste.",
                color = SpIvory.copy(alpha = 0.65f),
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 15.sp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(1.dp)
                    .background(SpIvory.copy(alpha = 0.2f)),
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp * animated)
                        .height(1.dp)
                        .background(SpAccentWarm),
                )
            }
            Spacer(Modifier.height(14.dp))
            val pctText = buildAnnotatedString {
                append("LOADING · ")
                withStyle(SpanStyle(color = SpIvory.copy(alpha = 0.6f))) {
                    append("${(animated * 100).toInt()}%")
                }
            }
            Text(
                pctText,
                color = SpIvory.copy(alpha = 0.4f),
                fontFamily = InterFamily,
                fontSize = 10.sp,
            )
        }
    }
}
