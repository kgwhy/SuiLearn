package com.suilearn.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.model.QuestionSearchResult
import com.suilearn.core.usecase.SearchLearningContentUseCase
import com.suilearn.di.AppDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchViewModel(
    dependencies: AppDependencies,
) : ViewModel() {
    private val searchLearningContentUseCase: SearchLearningContentUseCase = dependencies.searchLearningContentUseCase

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun onEvent(event: SearchEvent) {
        when (event) {
            SearchEvent.Refresh -> refresh()
            is SearchEvent.QueryChanged -> updateSearch(event.query)
        }
    }

    private fun refresh() {
        updateSearch(_uiState.value.query)
    }

    private fun updateSearch(query: String) {
        _uiState.update { it.copy(query = query) }
        viewModelScope.launch {
            val results = withContext(Dispatchers.IO) {
                searchLearningContentUseCase.execute(query).data.map {
                    QuestionSearchResult(
                        id = it.id,
                        type = it.type,
                        title = it.title,
                        summary = it.summary,
                        categoryName = it.categoryName,
                        difficulty = it.difficulty,
                        hasAnswered = it.hasAnswered,
                        hasWrongRecord = it.hasWrongRecord,
                        matchedFields = it.matchedFields,
                    )
                }
            }
            _uiState.update {
                if (it.query == query) {
                    it.copy(searchResults = results)
                } else {
                    it
                }
            }
        }
    }
}
