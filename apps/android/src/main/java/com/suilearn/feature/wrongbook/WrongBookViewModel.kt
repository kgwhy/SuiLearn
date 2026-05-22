package com.suilearn.feature.wrongbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.usecase.MarkWrongQuestionMasteredUseCase
import com.suilearn.di.AppDependencies
import com.suilearn.feature.common.buildQuestionSummaryUiModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WrongBookViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val markWrongQuestionMasteredUseCase: MarkWrongQuestionMasteredUseCase = dependencies.markWrongQuestionMasteredUseCase

    private val _uiState = MutableStateFlow(WrongBookUiState())
    val uiState: StateFlow<WrongBookUiState> = _uiState.asStateFlow()

    init {
        onEvent(WrongBookEvent.Refresh)
        viewModelScope.launch {
            dependencies.refreshEvents.collect { onEvent(WrongBookEvent.Refresh) }
        }
    }

    fun onEvent(event: WrongBookEvent) {
        when (event) {
            WrongBookEvent.Refresh -> refresh()
            is WrongBookEvent.MarkMastered -> markMastered(event.questionId)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val wrongQuestions = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                val categoryNames = dependencies.studyPackRepository.listCategories().associate { it.categoryId to it.name }
                val knowledgePointNames = dependencies.studyPackRepository.listKnowledgePoints().associate { it.knowledgePointId to it.name }
                dependencies.wrongQuestionRepository.listActive().mapNotNull {
                    buildQuestionSummaryUiModel(
                        questionRepository = dependencies.questionRepository,
                        questionId = it.questionId,
                        auxText = "错答 ${it.wrongCount} 次",
                        categoryNames = categoryNames,
                        knowledgePointNames = knowledgePointNames,
                    )
                }
            }
            _uiState.update { it.copy(wrongQuestions = wrongQuestions) }
        }
    }

    private fun markMastered(questionId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                markWrongQuestionMasteredUseCase.execute(questionId)
            }
            refresh()
            dependencies.notifyDataChanged()
        }
    }
}
