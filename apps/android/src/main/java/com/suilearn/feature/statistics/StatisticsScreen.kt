package com.suilearn.feature.statistics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            AppSectionCard(title = "统计") {
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
    }
}
