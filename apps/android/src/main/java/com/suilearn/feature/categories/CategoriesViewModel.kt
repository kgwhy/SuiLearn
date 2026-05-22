package com.suilearn.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.di.AppDependencies
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CategoriesViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CategoriesUiState())
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        onEvent(CategoriesEvent.Refresh)
        viewModelScope.launch {
            dependencies.refreshEvents.collect { onEvent(CategoriesEvent.Refresh) }
        }
    }

    fun onEvent(event: CategoriesEvent) {
        when (event) {
            CategoriesEvent.Refresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val categories = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                dependencies.studyPackRepository.listCategories()
            }
            _uiState.update { it.copy(categories = categories) }
        }
    }
}
