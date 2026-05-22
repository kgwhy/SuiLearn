package com.suilearn.feature.wrongbook

import com.suilearn.ui.model.QuestionSummaryUiModel

data class WrongBookUiState(
    val wrongQuestions: List<QuestionSummaryUiModel> = emptyList(),
)

