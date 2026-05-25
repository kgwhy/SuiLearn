package com.suilearn.feature.categories

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.ui.AppIconBadge
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.MetricChip
import com.suilearn.ui.ProgressRow

@Composable
fun CategoriesScreen(
    categoriesViewModel: CategoriesViewModel,
    onStartCategory: (String) -> Unit,
) {
    val uiState by categoriesViewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = "题库分类", subtitle = "选择一个方向进入专项练习。") {
                Text("分类进度会影响首页学习路径推荐。")
            }
        }
        items(uiState.categories, key = { it.category.categoryId }) { progress ->
            val category = progress.category
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIconBadge(icon = Icons.AutoMirrored.Outlined.MenuBook)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(category.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        AppOutlinedActionButton(text = "练习", onClick = { onStartCategory(category.categoryId) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricChip("题目", progress.questionCount.toString())
                        MetricChip("正确率", "${(progress.accuracy * 100).toInt()}%")
                        MetricChip("错题", progress.activeWrongCount.toString())
                    }
                    ProgressRow(
                        label = "进度",
                        progress = if (progress.questionCount == 0) 0f else progress.practicedCount.toFloat() / progress.questionCount,
                        rightLabel = "${progress.practicedCount}/${progress.questionCount}",
                    )
                }
            }
        }
    }
}
