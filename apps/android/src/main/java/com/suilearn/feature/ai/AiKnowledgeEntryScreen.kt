package com.suilearn.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.remote.AiProviderStatus
import com.suilearn.core.remote.GeneratedQuestionDraft
import com.suilearn.core.remote.TaskStatus
import com.suilearn.ui.AppIconBadge
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.AppPrimaryActionButton
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.EmptyState
import com.suilearn.ui.LoadingState

@Composable
fun AiKnowledgeEntryScreen(
    aiKnowledgeViewModel: AiKnowledgeViewModel,
    onStartLocalPractice: () -> Unit,
    onOpenKnowledgeMap: () -> Unit,
) {
    val uiState by aiKnowledgeViewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 28.dp),
    ) {
        item {
            AiKnowledgeHero()
        }
        item {
            ProviderStatusCard(
                uiState = uiState,
                onRefresh = aiKnowledgeViewModel::refresh,
            )
        }
        item {
            GeneratedContentsSection(
                uiState = uiState,
                onCheckTask = aiKnowledgeViewModel::checkTask,
                onSave = aiKnowledgeViewModel::saveDraft,
                onDiscard = aiKnowledgeViewModel::discardDraft,
            )
        }
        uiState.selectedTaskStatus?.let { taskStatus ->
            item {
                TaskStatusCard(taskStatus)
            }
        }
        item {
            OfflineActions(
                onStartLocalPractice = onStartLocalPractice,
                onOpenKnowledgeMap = onOpenKnowledgeMap,
            )
        }
    }
}

@Composable
private fun AiKnowledgeHero() {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AppIconBadge(
                icon = Icons.Outlined.AutoAwesome,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.tertiary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "AI / 知识库入口",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "第二版只查看远程状态和待确认结果；本地刷题始终可用。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ProviderStatusCard(
    uiState: AiKnowledgeUiState,
    onRefresh: () -> Unit,
) {
    AppSectionCard(
        title = "AI Provider",
        subtitle = "默认连接本地后端 http://10.0.2.2:8080/api/v2；不可达时不影响离线学习。",
    ) {
        when {
            uiState.isLoading -> LoadingState()
            uiState.providerStatus == null -> ServiceFallback(uiState.serviceMessage ?: "服务未连接，仍可离线刷题。")
            else -> ProviderDetails(uiState.providerStatus)
        }
        Spacer(Modifier.height(10.dp))
        AppOutlinedActionButton(
            text = "刷新状态",
            onClick = onRefresh,
            icon = Icons.Outlined.Refresh,
            modifier = Modifier.fillMaxWidth(),
        )
        uiState.actionMessage?.let {
            Spacer(Modifier.height(10.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ServiceFallback(message: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = message,
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProviderDetails(status: AiProviderStatus) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        LabelValueRow("Provider", status.providerType)
        LabelValueRow("configured / available", "${status.configured} / ${status.available}")
        LabelValueRow("chatModel", status.chatModel ?: "未返回")
        LabelValueRow("embeddingModel", status.embeddingModel ?: "未返回")
        LabelValueRow("embeddingDimensions", status.embeddingDimensions?.toString() ?: "未返回")
        status.message?.takeIf { it.isNotBlank() }?.let {
            LabelValueRow("message", it)
        }
    }
}

@Composable
private fun GeneratedContentsSection(
    uiState: AiKnowledgeUiState,
    onCheckTask: (String?) -> Unit,
    onSave: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    AppSectionCard(
        title = "生成题确认",
        subtitle = "只确认后端状态，不写入本地 Room 正式题库。",
    ) {
        when {
            uiState.isLoading -> LoadingState()
            uiState.generatedContents.isEmpty() -> EmptyState(
                title = "暂无待确认或已保存生成题",
                subtitle = "后端可用后，这里会显示 PENDING_REVIEW 和 SAVED 结果。",
            )
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.generatedContents.forEach { draft ->
                    GeneratedDraftCard(
                        draft = draft,
                        checkingTaskId = uiState.checkingTaskId,
                        reviewingContentId = uiState.reviewingContentId,
                        onCheckTask = onCheckTask,
                        onSave = onSave,
                        onDiscard = onDiscard,
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneratedDraftCard(
    draft: GeneratedQuestionDraft,
    checkingTaskId: String?,
    reviewingContentId: String?,
    onCheckTask: (String?) -> Unit,
    onSave: (String) -> Unit,
    onDiscard: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = draft.stem,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            LabelValueRow("status", draft.status)
            LabelValueRow("generationTaskId", draft.generationTaskId ?: "未返回")
            LabelValueRow("knowledgeBaseId", draft.knowledgeBaseId)
            AppOutlinedActionButton(
                text = if (checkingTaskId == draft.generationTaskId) "查询中" else "查任务",
                onClick = { onCheckTask(draft.generationTaskId) },
                icon = Icons.Outlined.TaskAlt,
                modifier = Modifier.fillMaxWidth(),
            )
            if (draft.status == "PENDING_REVIEW") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AppPrimaryActionButton(
                        text = if (reviewingContentId == draft.id) "保存中" else "保存",
                        onClick = { onSave(draft.id) },
                        icon = Icons.Outlined.CheckCircle,
                        modifier = Modifier.weight(1f),
                    )
                    AppOutlinedActionButton(
                        text = "丢弃",
                        onClick = { onDiscard(draft.id) },
                        icon = Icons.Outlined.DeleteOutline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskStatusCard(status: TaskStatus) {
    AppSectionCard(
        title = "任务状态",
        subtitle = "当前只读取任务进度，不在 Android 承载完整工作台。",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            LabelValueRow("taskId", status.id)
            LabelValueRow("kind", status.kind)
            LabelValueRow("status", status.status)
            LabelValueRow("currentStep", status.currentStep ?: "未返回")
            LabelValueRow("errorMessage", status.errorMessage ?: "无")
        }
    }
}

@Composable
private fun OfflineActions(
    onStartLocalPractice: () -> Unit,
    onOpenKnowledgeMap: () -> Unit,
) {
    AppSectionCard(
        title = "本地学习",
        subtitle = "远程 AI 状态不会阻塞第一版刷题闭环。",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AppPrimaryActionButton(
                text = "继续离线刷题",
                onClick = onStartLocalPractice,
                icon = Icons.AutoMirrored.Outlined.MenuBook,
                modifier = Modifier.fillMaxWidth(),
            )
            AppOutlinedActionButton(
                text = "看知识点",
                onClick = onOpenKnowledgeMap,
                icon = Icons.Outlined.Search,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun LabelValueRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
