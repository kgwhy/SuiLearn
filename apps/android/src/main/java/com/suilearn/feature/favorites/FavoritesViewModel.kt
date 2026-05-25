package com.suilearn.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.usecase.ToggleFavoriteQuestionUseCase
import com.suilearn.di.AppDependencies
import com.suilearn.feature.common.buildQuestionSummaryUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val toggleFavoriteQuestionUseCase: ToggleFavoriteQuestionUseCase = dependencies.toggleFavoriteQuestionUseCase

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        onEvent(FavoritesEvent.Refresh)
        viewModelScope.launch {
            dependencies.refreshEvents.collect { onEvent(FavoritesEvent.Refresh) }
        }
    }

    fun onEvent(event: FavoritesEvent) {
        when (event) {
            FavoritesEvent.Refresh -> refresh()
            is FavoritesEvent.ToggleFavorite -> toggleFavorite(event.questionId)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val favorites = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                val categoryNames = dependencies.studyPackRepository.listCategories().associate { it.categoryId to it.name }
                val knowledgePointNames = dependencies.studyPackRepository.listKnowledgePoints().associate { it.knowledgePointId to it.name }
                dependencies.favoriteRepository.listAll().mapNotNull {
                    buildQuestionSummaryUiModel(
                        questionRepository = dependencies.questionRepository,
                        questionId = it.questionId,
                        auxText = "收藏于 ${it.createdAt}",
                        categoryNames = categoryNames,
                        knowledgePointNames = knowledgePointNames,
                    )
                }
            }
            _uiState.update { it.copy(favorites = favorites) }
        }
    }

    private fun toggleFavorite(questionId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                toggleFavoriteQuestionUseCase.execute(questionId)
            }
            refresh()
            dependencies.notifyDataChanged()
        }
    }
}
