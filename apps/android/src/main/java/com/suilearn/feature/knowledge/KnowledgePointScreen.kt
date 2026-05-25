package com.suilearn.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.model.MasteryLevel
import com.suilearn.core.model.KnowledgePointProgressSummary
import com.suilearn.ui.AppIconBadge
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.EmptyState
import com.suilearn.ui.MetricChip
import com.suilearn.ui.ProgressRow

@Composable
fun KnowledgeMapScreen(
    knowledgePointViewModel: KnowledgePointViewModel,
    onOpenKnowledgePoint: (String) -> Unit,
    onStartPractice: (String) -> Unit,
) {
    val uiState by knowledgePointViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        knowledgePointViewModel.onEvent(KnowledgePointEvent.LoadList)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = "知识点地图", subtitle = "按掌握状态找到下一组要巩固的内容。") {
                Text("按分类查看知识点进度、错题和掌握状态。")
            }
        }
        items(uiState.groups, key = { it.categoryId }) { group ->
            AppSectionCard(title = group.categoryName.ifBlank { "未分类" }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    group.points.forEach { point ->
                        KnowledgePointProgressRow(
                            progress = point,
                            onOpen = { onOpenKnowledgePoint(point.knowledgePoint.knowledgePointId) },
                            onStartPractice = { onStartPractice(point.knowledgePoint.knowledgePointId) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun KnowledgePointScreen(
    knowledgePointViewModel: KnowledgePointViewModel,
    knowledgePointId: String,
    onStartPractice: (String) -> Unit,
) {
    val uiState by knowledgePointViewModel.uiState.collectAsStateWithLifecycle()
    val point = uiState.point
    val detail = uiState.detail?.takeIf { it.knowledgePoint.knowledgePointId == knowledgePointId }

    LaunchedEffect(knowledgePointId) {
        knowledgePointViewModel.onEvent(KnowledgePointEvent.Load(knowledgePointId))
    }

    if (point == null || detail == null) {
        EmptyState(
            title = "未找到知识点",
            subtitle = "当前题包中不包含该内容。",
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = point.name, subtitle = point.description, action = "练习", onActionClick = { onStartPractice(point.knowledgePointId) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("掌握度：${detail.masteryLevel.label()}")
                    Text("关联题目：${detail.relatedQuestionIds.size}")
                    Text("已答题：${detail.answeredCount}")
                    Text("错题：${detail.activeWrongCount}")
                }
            }
        }
        item {
            AppSectionCard(title = "关联题目") {
                if (uiState.relatedQuestions.isEmpty()) {
                    Text("无")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.relatedQuestions.forEach { question ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(question.stem)
                                Text("${question.categoryName} · ${question.knowledgePointNames.joinToString("、")}")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KnowledgePointProgressRow(
    progress: KnowledgePointProgressSummary,
    onOpen: () -> Unit,
    onStartPractice: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBadge(icon = Icons.AutoMirrored.Outlined.MenuBook)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(progress.knowledgePoint.name, fontWeight = FontWeight.Bold)
                Text(progress.knowledgePoint.description, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            AppOutlinedActionButton(text = "练习", onClick = onStartPractice)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricChip("题目", progress.questionCount.toString())
            MetricChip("已练习", progress.practicedCount.toString())
            MetricChip("错题", progress.activeWrongCount.toString())
            MetricChip("状态", progress.masteryLevel.label())
        }
        ProgressRow(
            label = "进度",
            progress = if (progress.questionCount == 0) 0f else progress.practicedCount.toFloat() / progress.questionCount,
            rightLabel = "${progress.practicedCount}/${progress.questionCount} · ${(progress.accuracy * 100).toInt()}%",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppOutlinedActionButton(text = "详情", onClick = onOpen)
        }
    }
}

private fun MasteryLevel.label(): String = when (this) {
    MasteryLevel.NOT_STARTED -> "未开始"
    MasteryLevel.WEAK -> "薄弱"
    MasteryLevel.LEARNING -> "学习中"
    MasteryLevel.MASTERED -> "已掌握"
}
