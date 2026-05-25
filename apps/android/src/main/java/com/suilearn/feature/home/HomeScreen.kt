package com.suilearn.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.common.HomeSummary
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.PracticeMode
import com.suilearn.ui.AppIconBadge
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.AppPrimaryActionButton
import com.suilearn.ui.EmptyState
import com.suilearn.ui.LoadingState

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        item {
            HomeHeader(summary = summary, onOpenSearch = onOpenSearch)
        }
        item {
            summary?.let {
                HomeHeroCard(summary = it, onResumePractice = onResumePractice)
            } ?: LoadingState()
        }
        if (summary != null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeMetricCard(
                        title = "学习天数",
                        value = "${summary.recentLearningDays} 天",
                        icon = Icons.Outlined.Add,
                        modifier = Modifier.weight(1f),
                    )
                    HomeMetricCard(
                        title = "待复盘",
                        value = "${summary.activeWrongCount} 道",
                        icon = Icons.Outlined.ErrorOutline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            HomeSectionTitle(
                title = "学习路径",
                subtitle = "优先补薄弱点，再进入综合练习",
            )
        }
        item {
            if (uiState.knowledgePoints.isEmpty()) {
                EmptyState("暂无知识点", "请先导入题包。")
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    uiState.knowledgePoints.take(2).forEachIndexed { index, point ->
                        LearningPathRow(
                            point = point,
                            action = if (index == 0) "练习" else "复盘",
                            tone = if (index == 0) LearningPathTone.Mint else LearningPathTone.Gold,
                            onAction = {
                                if (index == 0) {
                                    onStartPractice(PracticeMode.KNOWLEDGE_POINT, point.knowledgePointId)
                                } else {
                                    onOpenKnowledgePoint(point.knowledgePointId)
                                }
                            },
                        )
                    }
                }
            }
        }
        item {
            HomeSectionTitle(
                title = "今日行动",
                subtitle = "只保留不会和底部导航重复的即时操作",
            )
        }
        item {
            TodayActionCard(
                onStartSequentialPractice = { onStartPractice(PracticeMode.SEQUENTIAL, null) },
                onOpenSearch = onOpenSearch,
            )
        }
    }
}

@Composable
private fun HomeHeader(summary: HomeSummary?, onOpenSearch: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = summary?.todayTitle ?: "今天继续",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = summary?.studyPack?.name ?: "SuiLearn 学习",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Surface(
            onClick = onOpenSearch,
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Search, contentDescription = null)
            }
        }
    }
}

@Composable
private fun HomeHeroCard(
    summary: HomeSummary,
    onResumePractice: () -> Unit,
) {
    val progress = if (summary.totalQuestionCount == 0) {
        0f
    } else {
        summary.totalPracticedCount.toFloat() / summary.totalQuestionCount
    }.coerceIn(0f, 1f)

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "学习进度",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Text(
                        text = "已练 ${summary.totalPracticedCount} / ${summary.totalQuestionCount} 题",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (summary.resumeSessionId != null) {
                    AppOutlinedActionButton(
                        text = "继续上次练习",
                        onClick = onResumePractice,
                        modifier = Modifier.width(148.dp),
                    )
                }
            }
            ProgressRing(
                progress = progress,
                label = "${summary.recentLearningDays}",
                caption = "天连续",
            )
        }
    }
}

@Composable
private fun ProgressRing(progress: Float, label: String, caption: String) {
    Box(modifier = Modifier.size(92.dp), contentAlignment = Alignment.Center) {
        val track = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.22f)
        val arc = MaterialTheme.colorScheme.secondary
        Canvas(Modifier.size(84.dp)) {
            val stroke = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
            val side = size.minDimension - stroke.width
            val topLeft = (size.minDimension - side) / 2f
            drawArc(
                color = track,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                size = Size(side, side),
                style = stroke,
            )
            drawArc(
                color = arc,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                size = Size(side, side),
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun HomeMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBadge(
                icon = icon,
                containerColor = if (title == "待复盘") {
                    MaterialTheme.colorScheme.tertiaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (title == "待复盘") {
                    MaterialTheme.colorScheme.tertiary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HomeSectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private enum class LearningPathTone {
    Mint,
    Gold,
}

@Composable
private fun LearningPathRow(
    point: KnowledgePoint,
    action: String,
    tone: LearningPathTone,
    onAction: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIconBadge(
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                containerColor = if (tone == LearningPathTone.Mint) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                contentColor = if (tone == LearningPathTone.Mint) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.secondary
                },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(point.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = point.description.ifBlank { "按知识点巩固相关题目" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            AppOutlinedActionButton(text = action, onClick = onAction)
        }
    }
}

@Composable
private fun TodayActionCard(
    onStartSequentialPractice: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppPrimaryActionButton(
                text = "顺序练习",
                onClick = onStartSequentialPractice,
                icon = Icons.Outlined.Add,
                modifier = Modifier.weight(1f),
            )
            AppOutlinedActionButton(
                text = "搜索题目",
                onClick = onOpenSearch,
                icon = Icons.Outlined.Search,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
