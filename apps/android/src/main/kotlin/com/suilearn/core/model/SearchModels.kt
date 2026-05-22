package com.suilearn.core.model

enum class SearchResultType {
    QUESTION,
    KNOWLEDGE_POINT,
}

data class SearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val summary: String,
    val categoryName: String,
    val difficulty: Int?,
    val hasAnswered: Boolean,
    val hasWrongRecord: Boolean,
    val matchedFields: List<String>,
)

data class KnowledgePointDetail(
    val knowledgePoint: KnowledgePoint,
    val relatedQuestionIds: List<String>,
    val masteryLevel: MasteryLevel,
    val activeWrongCount: Int,
    val answeredCount: Int,
)

data class QuestionSearchResult(
    val id: String,
    val type: SearchResultType,
    val title: String,
    val summary: String,
    val categoryName: String,
    val difficulty: Int?,
    val hasAnswered: Boolean,
    val hasWrongRecord: Boolean,
    val matchedFields: List<String>,
)
