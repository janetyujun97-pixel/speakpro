package com.speakpro.designsystem.components.states

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.speakpro.core.errors.SpErrorCode
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * Error 态 —— 大号数字 error code + 红色 -8° 斜划线 + 可选"录音已本地保存"chip。
 */
@Composable
fun ErrorState(
    error: SpErrorCode,
    modifier: Modifier = Modifier,
    localBackupSize: String? = null,
    onRetry: (() -> Unit)? = null,
    onFeedback: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpBackground)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))

        NumberGlyph(error.displayNumber)

        Spacer(Modifier.height(10.dp))
        Text(
            error.eyebrow,
            color = SpAccent,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.2.sp,
            fontFamily = InterFamily,
        )

        Spacer(Modifier.height(20.dp))
        Text(error.headline, color = SpPrimary, fontFamily = FraunceFamily, fontSize = 26.sp)
        Text(
            error.headlineItalic,
            color = SpAccent,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 26.sp,
        )

        Spacer(Modifier.height(16.dp))
        Text(
            error.body,
            color = SpMuted,
            fontSize = 13.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 260.dp),
        )

        if (localBackupSize != null) {
            Spacer(Modifier.height(18.dp))
            BackupChip(size = localBackupSize)
        }

        if (onRetry != null) {
            Spacer(Modifier.height(28.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(SpPrimary)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 28.dp, vertical = 14.dp),
            ) {
                Text(
                    "再试一次",
                    color = SpIvory,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = SpIvory)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onFeedback != null) {
                Text(
                    "反馈问题",
                    color = SpAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable(onClick = onFeedback),
                )
                Text(" · ", color = SpLine, fontSize = 11.sp)
            }
            Text("错误码 ${error.code}", color = SpMuted, fontSize = 11.sp)
        }

        Spacer(Modifier.weight(1f))

        FooterRule(footer = "ERROR STATE", number = "N° ${error.displayNumber}")
    }
}

@Composable
private fun NumberGlyph(number: String) {
    Box(contentAlignment = Alignment.Center) {
        Text(
            number,
            color = SpPrimary,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 110.sp,
            lineHeight = 110.sp,
        )
        Box(
            modifier = Modifier
                .rotate(-8f)
                .height(2.dp)
                .fillMaxWidth(0.85f)
                .background(SpAccent),
        )
    }
}

@Composable
private fun BackupChip(size: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(CircleShape)
            .background(SpIvory)
            .border(1.dp, SpLine, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(SpMoss))
        Spacer(Modifier.size(8.dp))
        Text(
            "录音已本地保存 · $size",
            color = SpMoss,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
        )
    }
}
