package com.suilearn.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.usecase.GetHomeSummaryUseCase
import com.suilearn.di.AppDependencies
import com.suilearn.ui.model.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val getHomeSummaryUseCase: GetHomeSummaryUseCase = dependencies.getHomeSummaryUseCase

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        onEvent(HomeEvent.Refresh)
        viewModelScope.launch {
            dependencies.refreshEvents.collect { onEvent(HomeEvent.Refresh) }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            HomeEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                HomeUiState(
                    homeSummary = getHomeSummaryUseCase.execute().dataOrNull(),
                    knowledgePoints = dependencies.studyPackRepository.listKnowledgePoints(),
                )
            }
            _uiState.update { state }
        }
    }
}
