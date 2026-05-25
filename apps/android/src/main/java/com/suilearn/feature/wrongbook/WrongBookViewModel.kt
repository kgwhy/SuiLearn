package com.suilearn.feature.wrongbook

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.usecase.MarkWrongQuestionMasteredUseCase
import com.suilearn.di.AppDependencies
import com.suilearn.feature.common.buildQuestionSummaryUiModel
import com.suilearn.core.model.WrongQuestionStatus
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
            is WrongBookEvent.SelectKnowledgePoint -> {
                _uiState.update { it.copy(selectedKnowledgePointId = event.knowledgePointId) }
                refresh()
            }
            is WrongBookEvent.ShowMasteredChanged -> {
                _uiState.update { it.copy(showMastered = event.showMastered) }
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            val wrongQuestions = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                val categoryNames = dependencies.studyPackRepository.listCategories().associate { it.categoryId to it.name }
                val knowledgePoints = dependencies.studyPackRepository.listKnowledgePoints()
                val knowledgePointNames = knowledgePoints.associate { it.knowledgePointId to it.name }
                val questions = dependencies.questionRepository.listQuestions().associateBy { it.questionId }
                val allWrongQuestions = dependencies.wrongQuestionRepository.listAll()
                val groups = knowledgePoints.mapNotNull { point ->
                    val relatedWrongQuestions = allWrongQuestions.filter { wrong ->
                        questions[wrong.questionId]?.knowledgePointIds?.contains(point.knowledgePointId) == true
                    }
                    if (relatedWrongQuestions.isEmpty()) {
                        null
                    } else {
                        WrongBookKnowledgePointGroup(
                            knowledgePointId = point.knowledgePointId,
                            name = point.name,
                            activeCount = relatedWrongQuestions.count { it.status == WrongQuestionStatus.ACTIVE },
                            masteredCount = relatedWrongQuestions.count { it.status == WrongQuestionStatus.MASTERED },
                        )
                    }
                }.sortedByDescending { it.totalCount }
                val selectedKnowledgePointId = _uiState.value.selectedKnowledgePointId
                val showMastered = _uiState.value.showMastered
                val visibleWrongQuestions = allWrongQuestions.filter { wrong ->
                    val question = questions[wrong.questionId] ?: return@filter false
                    val matchesKnowledgePoint = selectedKnowledgePointId == null ||
                        question.knowledgePointIds.contains(selectedKnowledgePointId)
                    val matchesStatus = showMastered || wrong.status == WrongQuestionStatus.ACTIVE
                    matchesKnowledgePoint && matchesStatus
                }
                val rows = visibleWrongQuestions.mapNotNull {
                    buildQuestionSummaryUiModel(
                        questionRepository = dependencies.questionRepository,
                        questionId = it.questionId,
                        auxText = if (it.status == WrongQuestionStatus.MASTERED) {
                            "已掌握 / 错答 ${it.wrongCount} 次"
                        } else {
                            "错答 ${it.wrongCount} 次"
                        },
                        categoryNames = categoryNames,
                        knowledgePointNames = knowledgePointNames,
                    )
                }
                groups to rows
            }
            _uiState.update { it.copy(knowledgePointGroups = wrongQuestions.first, wrongQuestions = wrongQuestions.second) }
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
