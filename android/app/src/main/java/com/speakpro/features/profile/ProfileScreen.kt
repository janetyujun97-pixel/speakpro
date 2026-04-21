package com.speakpro.features.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.core.storage.TokenManager
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.InterFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary

/**
 * 我的 Tab —— editorial 风格。
 * 对应 speakpro/components/Tabs.jsx · Profile。
 */
@Composable
fun ProfileScreen(
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val userName = TokenManager.userName ?: "同学"
    val userEmail = TokenManager.userEmail ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp),
    ) {
        // masthead
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 20.dp),
        ) {
            Eyebrow("PROFILE · 我的")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Settings, null, tint = SpMuted, modifier = Modifier.size(20.dp))
        }

        // identity
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp).clip(CircleShape).background(SpPrimary),
            ) {
                Text(
                    userName.firstOrNull()?.toString() ?: "?",
                    color = SpIvory,
                    fontFamily = FraunceFamily,
                    fontStyle = FontStyle.Italic,
                    fontSize = 30.sp,
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(userName, color = SpPrimary, fontFamily = FraunceFamily, fontSize = 24.sp)
                Spacer(Modifier.height(2.dp))
                Text(userEmail, color = SpMuted, fontSize = 12.sp, maxLines = 1)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "IELTS 学员",
                        color = SpIvory,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(SpAccent, RoundedCornerShape(2.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "LV. 4 · Member since ${sinceDate()}",
                        color = SpMuted,
                        fontSize = 11.sp,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // goal block
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(SpIvory)
                .border(1.dp, SpLine, RoundedCornerShape(10.dp))
                .padding(18.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow("MY GOAL")
                Spacer(Modifier.height(6.dp))
                val goal = buildAnnotatedString {
                    append("IELTS ")
                    withStyle(SpanStyle(color = SpAccent, fontStyle = FontStyle.Italic)) {
                        append("7.0")
                    }
                }
                Text(goal, color = SpPrimary, fontFamily = FraunceFamily, fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                Text("考试日 · 2026 年 5 月 22 日", color = SpMuted, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    daysLeftToExam().toString(),
                    color = SpPrimary,
                    fontFamily = FraunceFamily,
                    fontSize = 36.sp,
                )
                Text("DAYS LEFT", color = SpMuted, fontSize = 9.sp, letterSpacing = 1.5.sp)
            }
        }

        // achievements
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 22.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Eyebrow("成就 · 3 / 12")
                Spacer(Modifier.weight(1f))
                Text("查看全部", color = SpMuted, fontSize = 11.sp)
            }
            Spacer(Modifier.height(10.dp))
            val achievements = listOf(
                Triple("连续 7 天", "🔥", true),
                Triple("首次模考", "◆", true),
                Triple("朗读 20 次", "❋", true),
                Triple("连续 30 天", "⬡", false),
                Triple("Band 7.0", "✦", false),
                Triple("模考 × 10", "⟡", false),
            )
            achievements.chunked(3).forEach { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                ) {
                    row.forEach { (t, icon, got) ->
                        AchievementTile(t, icon, got, modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        // settings
        Column(modifier = Modifier.padding(horizontal = 24.dp).padding(top = 22.dp)) {
            Eyebrow("设置 · SETTINGS")
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))

            val settings = listOf(
                SettingRow("01", "学习目标", "IELTS 7.0 · 5 月 22 日", null),
                SettingRow("02", "每日提醒", "19:00 · 开启", null),
                SettingRow("03", "考官口音", "British RP + American GA", null),
                SettingRow("04", "订阅计划", "Pro · 剩余 108 天", "PRO"),
                SettingRow("05", "数据与隐私", "音频自动 30 天后删除", null),
                SettingRow("06", "帮助与反馈", null, null),
            )
            settings.forEach { SettingsRowItem(it) }
        }

        // logout
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp)
                .fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .border(1.dp, SpAccent, CircleShape)
                    .clickable(onClick = onLogout)
                    .padding(vertical = 14.dp),
            ) {
                Text(
                    "退出登录",
                    color = SpAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "SPEAKPRO · V 2.4.1 · 2026",
                color = SpMuted,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ============================================================================

private data class SettingRow(
    val num: String,
    val title: String,
    val subtitle: String?,
    val badge: String?,
)

@Composable
private fun SettingsRowItem(row: SettingRow) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
    ) {
        Text(
            row.num,
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontSize = 15.sp,
            modifier = Modifier.width(28.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(row.title, color = SpPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (row.badge != null) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        row.badge,
                        color = SpIvory,
                        fontSize = 9.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .background(SpAccent, RoundedCornerShape(2.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            if (row.subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(row.subtitle, color = SpMuted, fontSize = 11.sp)
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            null,
            tint = SpMuted,
            modifier = Modifier.size(14.dp),
        )
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(SpLine))
}

@Composable
private fun AchievementTile(
    title: String, icon: String, got: Boolean, modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (got) SpIvory else Color.Transparent)
            .border(1.dp, SpLine, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 14.dp),
    ) {
        Text(
            icon,
            color = if (got) SpAccent else SpMuted,
            fontFamily = FraunceFamily,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            title,
            color = if (got) SpPrimary else SpMuted,
            fontSize = 11.sp,
        )
    }
}

private fun daysLeftToExam(): Long {
    // 简化：考试日固定 5/22（真实数据应来自 onboarding profile）
    val exam = java.time.LocalDate.of(java.time.Year.now().value, 5, 22)
    val today = java.time.LocalDate.now()
    val target = if (exam.isBefore(today)) exam.plusYears(1) else exam
    return java.time.temporal.ChronoUnit.DAYS.between(today, target).coerceAtLeast(0)
}

private fun sinceDate(): String {
    return "Jan ${java.time.Year.now().value}"
}

@Composable
private fun Eyebrow(text: String, color: Color = SpMuted) {
    Text(
        text.uppercase(),
        color = color,
        fontFamily = InterFamily,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 2.2.sp,
    )
}
