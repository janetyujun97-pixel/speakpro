package com.speakpro.features.review

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.data.models.NotebookFilter
import com.speakpro.data.models.NotebookPhrase
import com.speakpro.data.models.NotebookWord
import com.speakpro.designsystem.theme.FraunceFamily
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpIvory
import com.speakpro.designsystem.theme.SpLine
import com.speakpro.designsystem.theme.SpMoss
import com.speakpro.designsystem.theme.SpMuted
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.features.auth.Eyebrow
import com.speakpro.features.auth.SpPrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    onBack: () -> Unit,
    viewModel: NotebookViewModel = hiltViewModel(),
) {
    val words by viewModel.words.collectAsState()
    val phrases by viewModel.phrases.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val reviewingWord by viewModel.reviewingWord.collectAsState()

    var tab by remember { mutableStateOf(Tab.WORDS) }
    LaunchedEffect(Unit) { viewModel.loadAll() }

    Scaffold(
        containerColor = SpBackground,
        topBar = {
            TopAppBar(
                title = { Text("错题本", color = SpPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = SpPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SpBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SpBackground),
        ) {
            TabRow(tab, onTabChange = { tab = it })

            if (tab == Tab.WORDS) {
                FilterBar(
                    filter = filter,
                    dueCount = viewModel.dueCount,
                    onFilter = viewModel::setFilter,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    isLoading && words.isEmpty() && phrases.isEmpty() ->
                        CircularProgressIndicator()
                    tab == Tab.WORDS -> WordList(
                        items = words,
                        onReview = viewModel::startReview,
                        onDelete = viewModel::deleteWord,
                    )
                    else -> PhraseList(items = phrases)
                }
            }
        }

        if (reviewingWord != null) {
            ReviewSheet(
                word = reviewingWord!!,
                onDismiss = viewModel::dismissReview,
                onAnswer = { quality ->
                    viewModel.submitReview(reviewingWord!!, quality)
                },
            )
        }
    }
}

private enum class Tab(val label: String) { WORDS("生词"), PHRASES("短语") }

@Composable
private fun TabRow(selected: Tab, onTabChange: (Tab) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Tab.entries.forEach { t ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabChange(t) }
                    .padding(vertical = 8.dp),
            ) {
                Text(
                    t.label,
                    color = if (selected == t) SpPrimary else SpMuted,
                    fontSize = 14.sp,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .height(2.dp)
                        .background(if (selected == t) SpAccent else Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun FilterBar(
    filter: NotebookFilter,
    dueCount: Int,
    onFilter: (NotebookFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Chip("待复习 ($dueCount)", filter == NotebookFilter.DUE) { onFilter(NotebookFilter.DUE) }
        Chip("全部", filter == NotebookFilter.ALL) { onFilter(NotebookFilter.ALL) }
        Chip("已掌握", filter == NotebookFilter.MASTERED) { onFilter(NotebookFilter.MASTERED) }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) SpPrimary else SpIvory)
            .border(1.dp, if (selected) SpPrimary else SpLine, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            label,
            color = if (selected) SpIvory else SpPrimary,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun WordList(
    items: List<NotebookWord>,
    onReview: (NotebookWord) -> Unit,
    onDelete: (NotebookWord) -> Unit,
) {
    if (items.isEmpty()) {
        Text("暂无生词", color = SpMuted)
        return
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
        items(items, key = { it.id }) { w ->
            WordRow(w, onReview, onDelete)
        }
    }
}

@Composable
private fun WordRow(
    w: NotebookWord,
    onReview: (NotebookWord) -> Unit,
    onDelete: (NotebookWord) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                w.word,
                color = SpPrimary,
                fontFamily = FraunceFamily,
                fontSize = 18.sp,
            )
            Row {
                if (!w.ipa.isNullOrEmpty()) {
                    Text(w.ipa, color = SpMuted, fontSize = 11.sp)
                    Spacer(Modifier.width(8.dp))
                }
                if (w.masteredAt != null) {
                    Text("已掌握", color = SpMoss, fontSize = 11.sp)
                }
            }
        }
        Text("×${w.missCount}", color = SpMuted, fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(SpPrimary)
                .clickable { onReview(w) }
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text("复习", color = SpIvory, fontSize = 11.sp)
        }
        IconButton(onClick = { onDelete(w) }) {
            Icon(Icons.Filled.Delete, null, tint = SpMuted)
        }
    }
}

@Composable
private fun PhraseList(items: List<NotebookPhrase>) {
    if (items.isEmpty()) {
        Text("暂无短语", color = SpMuted)
        return
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
        items(items, key = { it.id }) { p ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            ) {
                Text(p.phrase, color = SpPrimary, fontSize = 14.sp)
                if (!p.note.isNullOrEmpty()) {
                    Text(p.note, color = SpMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReviewSheet(
    word: NotebookWord,
    onDismiss: () -> Unit,
    onAnswer: (Int) -> Unit,
) {
    val sheet = rememberModalBottomSheetState()
    var revealed by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
        containerColor = SpBackground,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Eyebrow("REVIEW · 复习")
            Spacer(Modifier.height(24.dp))
            Text(
                word.word,
                color = SpPrimary,
                fontFamily = FraunceFamily,
                fontSize = 40.sp,
                fontWeight = FontWeight.Normal,
            )
            if (revealed && !word.ipa.isNullOrEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text(word.ipa, color = SpMuted, fontSize = 14.sp)
            }
            Spacer(Modifier.height(40.dp))

            if (!revealed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(CircleShape)
                        .border(1.dp, SpPrimary, CircleShape)
                        .clickable { revealed = true },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("显示答案", color = SpPrimary, fontSize = 14.sp)
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SheetButton("没想起来", SpAccent, Modifier.weight(1f)) { onAnswer(2) }
                    SheetButton("想起来了", SpMoss, Modifier.weight(1f)) { onAnswer(4) }
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SheetButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = SpIvory, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
