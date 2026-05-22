package com.suilearn.feature.favorites

import com.suilearn.ui.model.QuestionSummaryUiModel

data class FavoritesUiState(
    val favorites: List<QuestionSummaryUiModel> = emptyList(),
)

