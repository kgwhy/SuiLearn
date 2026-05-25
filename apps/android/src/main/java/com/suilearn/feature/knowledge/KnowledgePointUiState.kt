package com.suilearn.feature.knowledge

import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.KnowledgePointDetail
import com.suilearn.core.model.KnowledgePointProgressSummary
import com.suilearn.ui.model.QuestionSummaryUiModel

data class KnowledgePointUiState(
    val point: KnowledgePoint? = null,
    val detail: KnowledgePointDetail? = null,
    val groups: List<KnowledgeCategoryUiModel> = emptyList(),
    val relatedQuestions: List<QuestionSummaryUiModel> = emptyList(),
)

data class KnowledgeCategoryUiModel(
    val categoryId: String,
    val categoryName: String,
    val points: List<KnowledgePointProgressSummary>,
)
