package com.suilearn.feature.wrongbook

import com.suilearn.ui.model.QuestionSummaryUiModel

data class WrongBookUiState(
    val wrongQuestions: List<QuestionSummaryUiModel> = emptyList(),
    val knowledgePointGroups: List<WrongBookKnowledgePointGroup> = emptyList(),
    val selectedKnowledgePointId: String? = null,
    val showMastered: Boolean = false,
)

data class WrongBookKnowledgePointGroup(
    val knowledgePointId: String,
    val name: String,
    val activeCount: Int,
    val masteredCount: Int,
) {
    val totalCount: Int = activeCount + masteredCount
}
