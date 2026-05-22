package com.suilearn.ui.model

data class QuestionSummaryUiModel(
    val questionId: String,
    val stem: String,
    val categoryName: String,
    val knowledgePointNames: List<String>,
    val auxText: String,
)

