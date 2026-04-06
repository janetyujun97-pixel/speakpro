package com.speakpro.features.homework

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GradeOutlined
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.speakpro.core.network.ApiClient.nestRetrofit
import com.speakpro.core.network.Endpoints
import com.speakpro.data.models.ApiResponse
import com.speakpro.data.models.HomeworkAssignment
import com.speakpro.data.models.QuestionItem
import com.speakpro.designsystem.theme.SpAccent
import com.speakpro.designsystem.theme.SpBackground
import com.speakpro.designsystem.theme.SpBodyMedium
import com.speakpro.designsystem.theme.SpBodySmall
import com.speakpro.designsystem.theme.SpCaption
import com.speakpro.designsystem.theme.SpError
import com.speakpro.designsystem.theme.SpPrimary
import com.speakpro.designsystem.theme.SpSuccess
import com.speakpro.designsystem.theme.SpTextPrimary
import com.speakpro.designsystem.theme.SpTextSecondary
import com.speakpro.designsystem.theme.SpTitleMedium
import com.speakpro.designsystem.theme.SpTitleSmall
import com.speakpro.designsystem.theme.SpWarning
import com.speakpro.designsystem.theme.SpWhite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * 作业详情页面
 *
 * - 作业标题、教师、题目数
 * - 截止日期（含颜色警告）
 * - 作业说明
 * - 状态标签（已提交/已批改/待完成）
 * - 可展开的题目列表
 * - 每题：Part 标签 + section + 题目文本 + 开始练习按钮
 * - 提交按钮 + 评分显示
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeworkDetailScreen(
    homeworkId: String,
    onBack: () -> Unit,
    onStartPractice: (String) -> Unit = {},
    viewModel: HomeworkDetailViewModel = viewModel(),
) {
    val assignment by viewModel.assignment.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    LaunchedEffect(homeworkId) {
        viewModel.loadAssignment(homeworkId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpBackground),
    ) {
        TopAppBar(
            title = { Text("作业详情", style = SpTitleSmall, color = SpTextPrimary) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SpWhite),
        )

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (assignment == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(errorMessage ?: "作业不存在", style = SpBodyMedium, color = SpTextSecondary)
            }
        } else {
            val hw = assignment!!
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── 作业头部 ──
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpWhite)
                        .padding(20.dp),
                ) {
                    // 状态标签
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(hw.title, style = SpTitleMedium, color = SpTextPrimary)
                        StatusBadge(hw)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 教师
                    hw.teacher?.let { teacher ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Person, null, tint = SpTextSecondary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("${teacher.name} 老师", style = SpBodySmall, color = SpTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 题目数
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Assignment, null, tint = SpTextSecondary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${hw.questions?.size ?: hw.questionIds?.size ?: 0} 道题目",
                            style = SpBodySmall,
                            color = SpTextSecondary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 截止日期 ──
                hw.dueDate?.let { dueDate ->
                    val isOverdue = false // 简化：实际项目中应比较日期
                    val isUrgent = false

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isOverdue -> SpError.copy(alpha = 0.1f)
                                    isUrgent -> SpWarning.copy(alpha = 0.1f)
                                    else -> SpWhite
                                },
                            )
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (isOverdue) Icons.Filled.Warning else Icons.Filled.AccessTime,
                            null,
                            tint = when {
                                isOverdue -> SpError
                                isUrgent -> SpWarning
                                else -> SpTextSecondary
                            },
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("截止日期", style = SpCaption, color = SpTextSecondary)
                            Text(dueDate, style = SpBodyMedium, color = SpTextPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── 作业说明 ──
                hw.description?.let { desc ->
                    if (desc.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpWhite)
                                .padding(16.dp),
                        ) {
                            Text("作业说明", style = SpTitleSmall, color = SpTextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(desc, style = SpBodySmall, color = SpTextSecondary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // ── 题目列表 ──
                val questions = hw.questions ?: emptyList()
                if (questions.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        Text("题目列表", style = SpTitleSmall, color = SpTextPrimary)
                        Spacer(modifier = Modifier.height(12.dp))

                        questions.forEachIndexed { index, question ->
                            QuestionRow(
                                index = index + 1,
                                question = question,
                                onStartPractice = { onStartPractice(question.id) },
                            )
                            if (index < questions.size - 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                // ── 评分显示 ──
                hw.submissions?.firstOrNull()?.let { submission ->
                    submission.score?.let { score ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(SpWhite)
                                .padding(20.dp),
                        ) {
                            Text("评分", style = SpTitleSmall, color = SpTextPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${score.toInt()} 分",
                                style = SpTitleMedium.copy(fontWeight = FontWeight.Bold),
                                color = SpAccent,
                            )
                            submission.feedback?.let { feedback ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(feedback, style = SpBodySmall, color = SpTextSecondary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── 提交按钮 ──
            if (!hw.isCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SpWhite)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    Button(
                        onClick = { viewModel.submitAssignment(homeworkId) },
                        colors = ButtonDefaults.buttonColors(containerColor = SpAccent),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            "提交作业",
                            style = SpBodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SpWhite,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(hw: HomeworkAssignment) {
    val (text, color) = when {
        hw.submissions?.any { it.status == "graded" } == true -> "已批改" to SpSuccess
        hw.submissions?.any { it.status == "submitted" } == true -> "已提交" to SpPrimary
        else -> "待完成" to SpWarning
    }

    Text(
        text = text,
        style = SpCaption.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun QuestionRow(
    index: Int,
    question: QuestionItem,
    onStartPractice: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = SpWhite),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Part 标签
                question.section?.let { section ->
                    Text(
                        text = section,
                        style = SpCaption.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                        color = SpAccent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(SpAccent.copy(alpha = 0.1f))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Text(
                    "第 $index 题",
                    style = SpBodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = SpTextPrimary,
                    modifier = Modifier.weight(1f),
                )

                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    tint = SpTextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        question.content.ifBlank { question.title },
                        style = SpBodySmall,
                        color = SpTextSecondary,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onStartPractice,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpAccent),
                    ) {
                        Icon(Icons.Filled.Mic, null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("开始练习", style = SpBodySmall)
                    }
                }
            }
        }
    }
}

// ── ViewModel ──

class HomeworkDetailViewModel : ViewModel() {

    private val _assignment = MutableStateFlow<HomeworkAssignment?>(null)
    val assignment: StateFlow<HomeworkAssignment?> = _assignment.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadAssignment(id: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val api = nestRetrofit.create(HomeworkDetailApi::class.java)
                val resp = api.getAssignment(id)
                _assignment.value = resp.data
            } catch (e: Exception) {
                _errorMessage.value = "加载失败: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitAssignment(id: String) {
        viewModelScope.launch {
            try {
                val api = nestRetrofit.create(HomeworkDetailApi::class.java)
                api.submitAssignment(id)
                // 重新加载
                loadAssignment(id)
            } catch (e: Exception) {
                _errorMessage.value = "提交失败: ${e.message}"
            }
        }
    }
}

private interface HomeworkDetailApi {
    @GET("assignments/{id}")
    suspend fun getAssignment(@Path("id") id: String): ApiResponse<HomeworkAssignment>

    @retrofit2.http.POST("assignments/{id}/submit")
    suspend fun submitAssignment(@Path("id") id: String): ApiResponse<Any>
}
