package com.suilearn.feature.common

import com.suilearn.core.repository.QuestionRepository
import com.suilearn.ui.model.QuestionSummaryUiModel

suspend fun buildQuestionSummaryUiModel(
    questionRepository: QuestionRepository,
    questionId: String,
    auxText: String,
    categoryNames: Map<String, String>,
    knowledgePointNames: Map<String, String>,
): QuestionSummaryUiModel? {
    val question = questionRepository.getQuestion(questionId)
    val stem = when {
        question == null -> "题目已删除"
        question.isDeprecated -> "${question.stem}（已弃用）"
        else -> question.stem
    }
    val categoryName = question?.categoryId?.let(categoryNames::get).orEmpty()
    val knowledgePointNamesList = question?.knowledgePointIds.orEmpty().mapNotNull(knowledgePointNames::get)
    return QuestionSummaryUiModel(
        questionId = questionId,
        stem = stem,
        categoryName = categoryName,
        knowledgePointNames = knowledgePointNamesList,
        auxText = auxText,
    )
}
