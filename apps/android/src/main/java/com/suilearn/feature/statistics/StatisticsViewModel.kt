package com.suilearn.feature.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.usecase.GetStatisticsSummaryUseCase
import com.suilearn.di.AppDependencies
import com.suilearn.ui.model.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatisticsViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val getStatisticsSummaryUseCase: GetStatisticsSummaryUseCase = dependencies.getStatisticsSummaryUseCase

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        onEvent(StatisticsEvent.Refresh)
        viewModelScope.launch {
            dependencies.refreshEvents.collect { onEvent(StatisticsEvent.Refresh) }
        }
    }

    fun onEvent(event: StatisticsEvent) {
        when (event) {
            StatisticsEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val summary = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                getStatisticsSummaryUseCase.execute().dataOrNull()
            }
            _uiState.update { it.copy(statisticsSummary = summary) }
        }
    }
}
