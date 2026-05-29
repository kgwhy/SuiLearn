package com.suilearn.feature.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suilearn.ui.AppIconBadge
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.AppPrimaryActionButton
import com.suilearn.ui.AppSectionCard

@Composable
fun AiKnowledgeEntryScreen(
    onStartLocalPractice: () -> Unit,
    onOpenKnowledgeMap: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AiKnowledgeHero()
        }
        item {
            AppSectionCard(
                title = "当前状态",
                subtitle = "后端和 AI Provider 尚未配置时，这里只作为第二版入口提示。",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    CapabilityStatusRow("本地 Java 八股题包", "可离线刷题")
                    CapabilityStatusRow("错题本 / 收藏 / 统计", "不依赖 AI")
                    CapabilityStatusRow("资料导入 / RAG 问答", "后续由服务端和 Web 工作台承接")
                }
            }
        }
        item {
            AppSectionCard(
                title = "后续会接入",
                subtitle = "Android 只承接必要入口和结果确认，不做完整知识库工作台。",
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionalScopeText("从知识点或错题发起 AI 生成请求")
                    OptionalScopeText("查看任务状态和生成结果")
                    OptionalScopeText("确认、保存、删除或修正生成内容")
                    OptionalScopeText("消费后端已保存的 AI 题目进入本地练习闭环")
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AppPrimaryActionButton(
                    text = "继续离线刷题",
                    onClick = onStartLocalPractice,
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    modifier = Modifier.weight(1f),
                )
                AppOutlinedActionButton(
                    text = "看知识点",
                    onClick = onOpenKnowledgeMap,
                    icon = Icons.Outlined.Search,
                    modifier = Modifier.weight(1f),
                )
            }
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
                    text = "AI / 知识库是可选增强",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = "第一版本地刷题、错题、收藏、统计和搜索不依赖任何 AI 配置。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun CapabilityStatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OptionalScopeText(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Text(
            text = text,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
