package com.speakpro.designsystem.components.states

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * Empty 态 —— 编辑式大号 italic `0` + -15° 斜划线；标题两行（正体 +
 * italic 副句）；可选 primary/secondary CTA。
 */
data class EmptyStateCTA(val title: String, val onClick: () -> Unit)

@Composable
fun EmptyState(
    eyebrow: String,
    headline: String,
    headlineItalic: String,
    message: String,
    modifier: Modifier = Modifier,
    primaryCTA: EmptyStateCTA? = null,
    secondaryCTA: EmptyStateCTA? = null,
    footer: String = "EMPTY STATE",
    footerNumber: String = "N° 001",
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        ZeroGlyph()
        Spacer(Modifier.height(28.dp))

        Text(
            eyebrow,
            color = SpAccent,
            fontFamily = InterFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
        )

        Spacer(Modifier.height(14.dp))
        Text(headline, color = SpPrimary, fontFamily = FraunceFamily, fontSize = 28.sp)
        Text(
            headlineItalic,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 28.sp,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            message,
            color = SpMuted,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 240.dp),
        )

        if (primaryCTA != null) {
            Spacer(Modifier.height(30.dp))
            PrimaryPill(title = primaryCTA.title, onClick = primaryCTA.onClick)
        }
        if (secondaryCTA != null) {
            Spacer(Modifier.height(14.dp))
            Text(
                secondaryCTA.title,
                color = SpAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = secondaryCTA.onClick),
            )
        }

        Spacer(Modifier.weight(1f))

        FooterRule(footer = footer, number = footerNumber)
    }
}

@Composable
private fun ZeroGlyph() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.height(140.dp)) {
        Text(
            "0",
            color = SpLine,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 140.sp,
            lineHeight = 140.sp,
        )
        Box(
            modifier = Modifier
                .rotate(-15f)
                .height(1.dp)
                .fillMaxWidth(0.4f)
                .background(SpAccent),
        )
    }
}

@Composable
private fun PrimaryPill(title: String, onClick: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(SpPrimary)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp, vertical = 14.dp),
    ) {
        Text(
            title,
            color = SpIvory,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = InterFamily,
        )
        Spacer(Modifier.widthIn(min = 8.dp))
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = SpIvory,
        )
    }
}

@Composable
internal fun FooterRule(footer: String, number: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                footer,
                color = SpMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Text(
                number,
                color = SpMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
    }
}
