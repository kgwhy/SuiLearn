package com.suilearn.feature.search

import com.suilearn.core.model.QuestionSearchResult

data class SearchUiState(
    val query: String = "",
    val searchResults: List<QuestionSearchResult> = emptyList(),
)

