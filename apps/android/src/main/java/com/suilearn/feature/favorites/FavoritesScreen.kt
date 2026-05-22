package com.suilearn.feature.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.feature.common.QuestionSummaryRow
import com.suilearn.ui.AppSectionCard

@Composable
fun FavoritesScreen(
    favoritesViewModel: FavoritesViewModel,
    onStartFavoritePractice: () -> Unit,
) {
    val uiState by favoritesViewModel.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            AppSectionCard(title = "收藏", action = "练习", onActionClick = onStartFavoritePractice) {
                Text("这里显示已收藏的题目。")
            }
        }
        items(uiState.favorites, key = { it.questionId }) { favorite ->
            QuestionSummaryRow(
                model = favorite,
                leadingIcon = Icons.Outlined.StarBorder,
                trailingAction = null,
                onTrailingAction = null,
            )
        }
    }
}
