package com.suilearn.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.PracticeMode
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.EmptyState
import com.suilearn.ui.LoadingState
import com.suilearn.ui.MetricChip

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onStartPractice: (PracticeMode, String?) -> Unit,
    onResumePractice: () -> Unit,
    onOpenKnowledgePoint: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenStatistics: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenWrongBook: () -> Unit,
) {
    val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.homeSummary

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
                Text("SuiLearn 学习", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = summary?.todayTitle ?: "继续学习",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        item {
            summary?.let {
                AppSectionCard(title = it.studyPack.name, subtitle = it.studyPack.description) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricChip("题目", it.totalQuestionCount.toString())
                            MetricChip("已练习", it.totalPracticedCount.toString())
                            MetricChip("正确率", "${it.totalCorrectRate}%")
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricChip("错题", it.activeWrongCount.toString())
                            MetricChip("学习天数", it.recentLearningDays.toString())
                            MetricChip("可继续", if (it.resumeSessionId == null) "否" else "是")
                        }
                        if (it.resumeSessionId != null) {
                            Button(onClick = onResumePractice) {
                                Icon(Icons.Outlined.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("继续练习")
                            }
                        }
                    }
                }
            } ?: LoadingState()
        }
        item {
            AppSectionCard(title = "快速开始") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = { onStartPractice(PracticeMode.SEQUENTIAL, null) }) {
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("顺序练习")
                    }
                    ElevatedButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("搜索")
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "知识点") {
                if (uiState.knowledgePoints.isEmpty()) {
                    EmptyState("暂无知识点", "请先导入题包。")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.knowledgePoints.take(4).forEach { point ->
                            KnowledgePointRow(point = point, onClick = { onOpenKnowledgePoint(point.knowledgePointId) })
                        }
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "快捷入口") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onOpenStatistics) { Text("统计") }
                    OutlinedButton(onClick = onOpenFavorites) { Text("收藏") }
                    OutlinedButton(onClick = onOpenWrongBook) { Text("错题本") }
                }
            }
        }
    }
}

@Composable
private fun KnowledgePointRow(point: KnowledgePoint, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(point.name, fontWeight = FontWeight.SemiBold)
                Text(point.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedButton(onClick = onClick) { Text("打开") }
        }
    }
}
