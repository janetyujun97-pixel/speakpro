package com.speakpro.designsystem.components.states

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import kotlinx.coroutines.delay

/**
 * Skeleton 加载态 —— 卡片占位（opacity 递减）+ Fraunces italic "patience" loader。
 */
@Composable
fun SkeletonState(
    modifier: Modifier = Modifier,
    headerTitle: String = "LOADING · 加载中",
    cardCount: Int = 3,
) {
    var pulse by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            pulse++
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        // masthead
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp),
        ) {
            Text(
                headerTitle,
                color = SpMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.2.sp,
                fontFamily = InterFamily,
            )
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))

        // hero placeholder
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp)
                .alpha(0.55f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(SpLine).width(220.dp).height(30.dp))
            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(SpLine).width(170.dp).height(30.dp))
        }

        // cards
        Column(
            modifier = Modifier.padding(horizontal = 24.dp).padding(top = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(cardCount) { i ->
                SkeletonCard(alpha = 1f - i * 0.15f)
            }
        }

        Spacer(Modifier.weight(1f))

        // typographic loader
        Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "patience",
                color = SpMuted,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
            )
            Spacer(Modifier.height(4.dp))
            val dotsFrames = listOf("●○○", "○●○", "○○●")
            Text(
                "FETCHING · ${dotsFrames[pulse % 3]}",
                color = SpMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                fontFamily = InterFamily,
            )
        }
    }
}

@Composable
private fun SkeletonCard(alpha: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(alpha)
            .clip(RoundedCornerShape(10.dp))
            .background(SpIvory)
            .border(1.dp, SpLine, RoundedCornerShape(10.dp))
            .padding(16.dp),
    ) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(SpLine))
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(SpLine).width(80.dp).height(10.dp))
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(SpLine).fillMaxWidth().height(14.dp))
            Box(Modifier.clip(RoundedCornerShape(3.dp)).background(SpLine).width(140.dp).height(8.dp))
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.clip(RoundedCornerShape(3.dp)).background(SpLine).width(30.dp).height(20.dp))
    }
}
