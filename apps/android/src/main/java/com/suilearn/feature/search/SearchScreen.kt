package com.suilearn.feature.search

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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.model.QuestionSearchResult
import com.suilearn.core.model.SearchResultType
import com.suilearn.ui.AppOutlinedActionButton
import com.suilearn.ui.AppSectionCard

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    onOpenKnowledgePoint: (String) -> Unit,
    onStartPractice: (String) -> Unit,
) {
    val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        searchViewModel.onEvent(SearchEvent.Refresh)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(20.dp),
    ) {
        item {
            AppSectionCard(title = "搜索题目", subtitle = "从题干、分类和知识点里快速定位。") {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = { newValue: String ->
                        searchViewModel.onEvent(SearchEvent.QueryChanged(newValue))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索题目或知识点") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                )
            }
        }
        items(uiState.searchResults, key = { "${it.type}:${it.id}" }) { result ->
            SearchResultRow(
                result = result,
                onOpenKnowledgePoint = onOpenKnowledgePoint,
                onStartPractice = onStartPractice,
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    result: QuestionSearchResult,
    onOpenKnowledgePoint: (String) -> Unit,
    onStartPractice: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(result.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(result.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("类型：${result.type.label()}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (result.categoryName.isNotBlank()) {
                    Text("分类：${result.categoryName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppOutlinedActionButton(
                    text = if (result.type == SearchResultType.QUESTION) "去练习" else "打开",
                    onClick = {
                        if (result.type == SearchResultType.QUESTION) {
                            onStartPractice(result.id)
                        } else {
                            onOpenKnowledgePoint(result.id)
                        }
                    },
                )
            }
        }
    }
}

private fun SearchResultType.label(): String = when (this) {
    SearchResultType.QUESTION -> "题目"
    SearchResultType.KNOWLEDGE_POINT -> "知识点"
}
