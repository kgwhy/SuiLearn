package com.suilearn.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.usecase.ResetLearningDataUseCase
import com.suilearn.di.AppDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val resetLearningDataUseCase: ResetLearningDataUseCase = dependencies.resetLearningDataUseCase

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        onEvent(SettingsEvent.Refresh)
        viewModelScope.launch {
            dependencies.refreshEvents.collect { onEvent(SettingsEvent.Refresh) }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.Refresh -> refresh()
            SettingsEvent.ResetLocalData -> resetLocalData()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val pack = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                dependencies.studyPackRepository.getCurrentPack()
            }
            _uiState.update {
                it.copy(
                    studyPackName = pack?.name,
                    packVersion = pack?.packVersion,
                )
            }
        }
    }

    private fun resetLocalData() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                resetLearningDataUseCase.execute()
            }
            refresh()
            dependencies.notifyDataChanged()
        }
    }
}
