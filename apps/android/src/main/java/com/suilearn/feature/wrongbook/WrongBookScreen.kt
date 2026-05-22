package com.suilearn.feature.wrongbook

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
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
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            AppSectionCard(title = "错题本", action = "练习", onActionClick = onStartWrongPractice) {
                Text("这里显示当前错题。")
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
