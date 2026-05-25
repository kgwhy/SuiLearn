package com.suilearn.feature.wrongbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.feature.common.QuestionSummaryRow
import com.suilearn.ui.AppSectionCard

@Composable
fun WrongBookScreen(
    wrongBookViewModel: WrongBookViewModel,
    onStartWrongPractice: () -> Unit,
) {
    val uiState by wrongBookViewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = "错题本", subtitle = "优先处理未掌握内容。", action = "练习", onActionClick = onStartWrongPractice) {
                Text("默认显示未掌握错题，可按知识点筛选。")
            }
        }
        item {
            AppSectionCard(title = "筛选") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.selectedKnowledgePointId == null,
                        onClick = { wrongBookViewModel.onEvent(WrongBookEvent.SelectKnowledgePoint(null)) },
                        label = { Text("全部") },
                    )
                    uiState.knowledgePointGroups.take(4).forEach { group ->
                        FilterChip(
                            selected = uiState.selectedKnowledgePointId == group.knowledgePointId,
                            onClick = { wrongBookViewModel.onEvent(WrongBookEvent.SelectKnowledgePoint(group.knowledgePointId)) },
                            label = { Text("${group.name} ${group.activeCount}/${group.totalCount}") },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("显示已掌握")
                    Switch(
                        checked = uiState.showMastered,
                        onCheckedChange = { wrongBookViewModel.onEvent(WrongBookEvent.ShowMasteredChanged(it)) },
                    )
                }
            }
        }
        items(uiState.wrongQuestions, key = { it.questionId }) { wrong ->
            QuestionSummaryRow(
                model = wrong,
                leadingIcon = Icons.Outlined.ErrorOutline,
                trailingAction = "已掌握",
                onTrailingAction = { wrongBookViewModel.onEvent(WrongBookEvent.MarkMastered(wrong.questionId)) },
            )
        }
    }
}
