package com.suilearn.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.model.MasteryLevel
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.EmptyState

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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
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
                if (detail.relatedQuestionIds.isEmpty()) {
                    Text("无")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        detail.relatedQuestionIds.forEach { id -> Text(id) }
                    }
                }
            }
        }
    }
}

private fun MasteryLevel.label(): String = when (this) {
    MasteryLevel.NOT_STARTED -> "未开始"
    MasteryLevel.WEAK -> "薄弱"
    MasteryLevel.LEARNING -> "学习中"
    MasteryLevel.MASTERED -> "已掌握"
}
