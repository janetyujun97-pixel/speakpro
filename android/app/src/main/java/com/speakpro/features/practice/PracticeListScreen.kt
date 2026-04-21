package com.speakpro.features.practice

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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * 练习 Tab —— editorial 风格。
 * 对应 speakpro/components/MoreScreens.jsx · PracticeList。
 */
@Composable
fun PracticeListScreen(
    onNavigate: (String) -> Unit,
) {
    var tab by remember { mutableStateOf("scene") }

    val quickTiles = listOf(
        QuickTile("朗读", "Reading", "4h ago", streak = 3, route = "practice/readaloud"),
        QuickTile("跟读", "Shadow", "Yesterday", streak = null, route = "practice/followread"),
        // 听写 / 复述 暂无对应页面 —— 点击降级到跟读/朗读，后续补实现
        QuickTile("听写", "Dictation", "—", streak = null, route = "practice/followread"),
        QuickTile("复述", "Retell", "2d ago", streak = null, route = "practice/readaloud"),
    )

    val scenes = listOf(
        Scene("IELTS · Part 1", "日常话题问答", "Daily interview", 24, "基础", SpAccent),
        Scene("IELTS · Part 2", "Cue Card 独白", "Long turn · 2 min", 18, "中级", SpPrimary),
        Scene("IELTS · Part 3", "深度讨论", "Two-way discussion", 16, "进阶", SpMoss),
        Scene("面试 · Interview", "英文面试", "Job interview", 12, "中级", Color(0xFF8A5A2B)),
        Scene("商务 · Business", "会议与演讲", "Meetings & pitches", 9, "进阶", Color(0xFF5A4A8A)),
    )

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
                .padding(top = 12.dp, bottom = 16.dp),
        ) {
            Eyebrow("PRACTICE · 练习")
            Spacer(Modifier.weight(1f))
            Icon(Icons.Filled.Search, null, tint = SpMuted, modifier = Modifier.size(20.dp))
        }

        // hero headline
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Text("Pick your", color = SpPrimary, fontFamily = FraunceFamily, fontSize = 32.sp)
            Text(
                "rhythm today.",
                color = SpAccent,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 32.sp,
            )
        }

        Spacer(Modifier.height(24.dp))

        // quick tiles 2×2
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            Eyebrow("QUICK · 快练 15 分钟")
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                quickTiles.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        pair.forEach { q ->
                            QuickTileCard(
                                q,
                                onClick = { onNavigate(q.route) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // category tabs
        Row(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            listOf("scene" to "场景分类", "level" to "按难度", "topic" to "话题").forEach { (k, l) ->
                val on = tab == k
                Column(modifier = Modifier.clickable { tab = k }) {
                    Text(
                        l,
                        color = if (on) SpPrimary else SpMuted,
                        fontSize = 13.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 10.dp),
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (on) SpPrimary else Color.Transparent),
                    )
                }
            }
        }
        Box(Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(1.dp).background(SpLine))

        // scene rows
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            scenes.forEachIndexed { i, s ->
                SceneRow(i + 1, s) {
                    onNavigate("practice/conversation")
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // dark recommend card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SpPrimary)
                .clickable { onNavigate("practice/conversation") }
                .padding(16.dp),
        ) {
            Text(
                "✦",
                color = Color(0xFFD9734A),
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 34.sp,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow("RECOMMENDED · 推荐", SpIvory.copy(alpha = 0.55f))
                Spacer(Modifier.height(4.dp))
                Text(
                    "今天建议练 Part 3 深度讨论",
                    color = SpIvory,
                    fontFamily = FraunceFamily,
                    fontSize = 16.sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "· 基于昨天的评分，Coherence 有提升空间",
                    color = SpIvory.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                null,
                tint = SpIvory,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

// ============================================================================

private data class QuickTile(
    val title: String,
    val en: String,
    val last: String,
    val streak: Int?,
    val route: String,
)

private data class Scene(
    val tag: String, val title: String, val en: String,
    val count: Int, val lvl: String, val color: Color,
)

@Composable
private fun QuickTileCard(q: QuickTile, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SpIvory)
            .border(1.dp, SpLine, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(q.title, color = SpPrimary, fontFamily = FraunceFamily, fontSize = 20.sp)
            Spacer(Modifier.weight(1f))
            if (q.streak != null) {
                Text(
                    "${q.streak}D",
                    color = SpIvory,
                    fontSize = 9.sp,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .background(SpAccent, RoundedCornerShape(2.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            q.en,
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(16.dp))
        Text("上次 · ${q.last}", color = SpMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
    }
}

@Composable
private fun SceneRow(num: Int, s: Scene, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
    ) {
        Text(
            "%02d".format(num),
            color = SpMuted,
            fontFamily = FraunceFamily,
            fontStyle = FontStyle.Italic,
            fontSize = 26.sp,
            modifier = Modifier.width(32.dp),
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow(s.tag, s.color)
            Spacer(Modifier.height(6.dp))
            Text(s.title, color = SpPrimary, fontFamily = FraunceFamily, fontSize = 18.sp)
            Spacer(Modifier.height(3.dp))
            Text(
                s.en,
                color = SpMuted,
                fontFamily = FraunceFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 11.sp,
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("● ${s.count} 题", color = SpMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
                Text("● ${s.lvl}", color = SpMuted, fontSize = 10.sp, letterSpacing = 0.5.sp)
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
