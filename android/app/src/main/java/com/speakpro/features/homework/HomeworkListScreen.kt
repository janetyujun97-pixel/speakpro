package com.speakpro.features.homework

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.speakpro.data.models.HomeworkAssignment
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleLarge
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite

/**
 * 作业列表页 — 对应 iOS HomeworkListView
 */
@Composable
fun HomeworkListScreen(
    onNavigateToDetail: (String) -> Unit,
    viewModel: HomeworkListViewModel = hiltViewModel(),
) {
    val assignments by viewModel.assignments.collectAsState()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabTitles = listOf("待完成", "已完成")

    LaunchedEffect(Unit) {
        viewModel.fetchAssignments()
    }

    val filteredAssignments = assignments.filter { hw ->
        if (selectedTab == 0) !hw.isCompleted else hw.isCompleted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "作业",
            style = SpTitleLarge,
            color = SpTextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // ── Tab 切换 ──
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SpBackground,
            contentColor = SpAccent,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = SpAccent,
                )
            },
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTab == index) SpAccent else SpTextSecondary,
                        )
                    },
                )
            }
        }

        // ── 列表 ──
        if (filteredAssignments.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
            ) {
                Text(
                    text = if (selectedTab == 0) "暂无待完成作业" else "暂无已完成作业",
                    style = SpBodyMedium,
                    color = SpTextSecondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filteredAssignments, key = { it.id }) { hw ->
                    HomeworkCard(
                        assignment = hw,
                        onClick = { onNavigateToDetail(hw.id) },
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HomeworkCard(
    assignment: HomeworkAssignment,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SpWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp),
        ) {
            Icon(
                imageVector = if (assignment.isCompleted) Icons.Default.CheckCircle else Icons.Default.Description,
                contentDescription = null,
                tint = if (assignment.isCompleted) SpSuccess else SpWarning,
                modifier = Modifier.size(24.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = assignment.title,
                    style = SpBodyMedium,
                    color = SpTextPrimary,
                )
                if (assignment.dueDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "截止：${assignment.dueDate}",
                        style = SpBodySmall,
                        color = SpTextSecondary,
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = SpTextSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
