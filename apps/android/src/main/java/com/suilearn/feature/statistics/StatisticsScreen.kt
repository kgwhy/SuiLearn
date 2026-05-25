package com.suilearn.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.LoadingState
import com.suilearn.ui.MetricChip
import com.suilearn.ui.ProgressRow

@Composable
fun StatisticsScreen(statisticsViewModel: StatisticsViewModel) {
    val uiState by statisticsViewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        statisticsViewModel.onEvent(StatisticsEvent.Refresh)
    }
    val summary = uiState.statisticsSummary

    if (summary == null) {
        LoadingState()
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = "复盘统计", subtitle = "观察答题量、正确率和薄弱点变化。") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("已答题", summary.totalAnsweredQuestions.toString())
                        MetricChip("正确率", "${(summary.totalAccuracy * 100).toInt()}%")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("错题", summary.activeWrongQuestionCount.toString())
                        MetricChip("可恢复", if (summary.latestRecoverableSessionId == null) "否" else "是")
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "概览") {
                Text("最近练习时间：${summary.latestPracticeAt?.toString() ?: "无"}")
            }
        }
        item {
            AppSectionCard(title = "分类正确率") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.categoryProgress.forEach { category ->
                        ProgressRow(
                            label = category.category.name,
                            progress = category.accuracy.toFloat(),
                            rightLabel = "${(category.accuracy * 100).toInt()}% · ${category.practicedCount}/${category.questionCount} · 错题 ${category.activeWrongCount}",
                        )
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "知识点掌握") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    summary.knowledgePointProgress.forEach { point ->
                        ProgressRow(
                            label = point.knowledgePoint.name,
                            progress = point.accuracy.toFloat(),
                            rightLabel = "${(point.accuracy * 100).toInt()}% · ${point.masteryLevel} · 错题 ${point.activeWrongCount}",
                        )
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "错题最多知识点") {
                if (summary.topWeakKnowledgePointProgress.isEmpty()) {
                    Text("暂无错题")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.topWeakKnowledgePointProgress.forEach { point ->
                            Text("${point.knowledgePoint.name}：${point.activeWrongCount} 道 ACTIVE 错题")
                        }
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "最近学习记录") {
                if (summary.recentLearningRecords.isEmpty()) {
                    Text("暂无记录")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        summary.recentLearningRecords.forEach { record ->
                            Text("${if (record.isCorrect) "正确" else "未答对"} · ${record.categoryName} · ${record.stem.take(28)}")
                        }
                    }
                }
            }
        }
    }
}
