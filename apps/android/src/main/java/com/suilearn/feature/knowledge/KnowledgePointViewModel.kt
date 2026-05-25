package com.suilearn.feature.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.di.AppDependencies
import com.suilearn.feature.common.buildQuestionSummaryUiModel
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
            KnowledgePointEvent.LoadList -> loadList()
            is KnowledgePointEvent.Load -> load(event.knowledgePointId)
        }
    }

    private fun loadList() {
        viewModelScope.launch {
            val groups = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                val categories = dependencies.studyPackRepository.listCategories().associateBy { it.categoryId }
                dependencies.statisticsRepository.getKnowledgePointProgress()
                    .groupBy { it.knowledgePoint.categoryId }
                    .map { (categoryId, points) ->
                        KnowledgeCategoryUiModel(
                            categoryId = categoryId,
                            categoryName = categories[categoryId]?.name.orEmpty(),
                            points = points.sortedBy { it.knowledgePoint.sortOrder },
                        )
                    }
                    .sortedBy { categories[it.categoryId]?.sortOrder ?: Int.MAX_VALUE }
            }
            _uiState.update { it.copy(groups = groups) }
        }
    }

    private fun load(knowledgePointId: String) {
        currentKnowledgePointId = knowledgePointId
        viewModelScope.launch {
            val state = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                val point = dependencies.studyPackRepository.listKnowledgePoints()
                    .firstOrNull { it.knowledgePointId == knowledgePointId }
                val detail = dependencies.statisticsRepository.getKnowledgePointDetail(knowledgePointId)
                val categoryNames = dependencies.studyPackRepository.listCategories().associate { it.categoryId to it.name }
                val knowledgePointNames = dependencies.studyPackRepository.listKnowledgePoints().associate { it.knowledgePointId to it.name }
                KnowledgePointUiState(
                    point = point,
                    detail = detail,
                    relatedQuestions = detail?.relatedQuestionIds.orEmpty().mapNotNull { questionId ->
                        buildQuestionSummaryUiModel(
                            questionRepository = dependencies.questionRepository,
                            questionId = questionId,
                            auxText = "关联题目",
                            categoryNames = categoryNames,
                            knowledgePointNames = knowledgePointNames,
                        )
                    },
                    groups = _uiState.value.groups,
                )
            }
            _uiState.update { state }
        }
    }
}
