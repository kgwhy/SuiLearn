package com.suilearn.feature.knowledge

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

class KnowledgePointViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KnowledgePointUiState())
    val uiState: StateFlow<KnowledgePointUiState> = _uiState.asStateFlow()

    private var currentKnowledgePointId: String? = null

    init {
        viewModelScope.launch {
            dependencies.refreshEvents.collect {
                currentKnowledgePointId?.let { onEvent(KnowledgePointEvent.Load(it)) }
            }
        }
    }

    fun onEvent(event: KnowledgePointEvent) {
        when (event) {
            is KnowledgePointEvent.Load -> load(event.knowledgePointId)
        }
    }

    private fun load(knowledgePointId: String) {
        currentKnowledgePointId = knowledgePointId
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                val point = dependencies.studyPackRepository.listKnowledgePoints()
                    .firstOrNull { it.knowledgePointId == knowledgePointId }
                KnowledgePointUiState(
                    point = point,
                    detail = dependencies.statisticsRepository.getKnowledgePointDetail(knowledgePointId),
                )
            }
            _uiState.update { state }
        }
    }
}
