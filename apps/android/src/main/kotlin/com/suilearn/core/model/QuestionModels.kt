package com.suilearn.core.model

enum class QuestionType {
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER,
}

data class QuestionOption(
    val optionId: String,
    val questionId: String,
    val optionKey: String,
    val content: String,
    val sortOrder: Int,
)

data class Question(
    val questionId: String,
    val packId: String,
    val categoryId: String,
    val type: QuestionType,
    val stem: String,
    val answer: List<String>,
    val explanation: String,
    val difficulty: Int,
    val isDeprecated: Boolean,
    val sortOrder: Int,
    val options: List<QuestionOption> = emptyList(),
    val knowledgePointIds: List<String> = emptyList(),
)

data class QuestionPack(
    val schemaVersion: Int,
    val packId: String,
    val packName: String,
    val packVersion: Int,
    val description: String,
    val categories: List<QuestionPackCategory>,
    val knowledgePoints: List<QuestionPackKnowledgePoint>,
    val questions: List<QuestionPackQuestion>,
)

data class QuestionPackCategory(
    val categoryId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
)

data class QuestionPackKnowledgePoint(
    val knowledgePointId: String,
    val categoryId: String,
    val name: String,
    val description: String,
    val sortOrder: Int,
)

data class QuestionPackQuestion(
    val questionId: String,
    val categoryId: String,
    val type: QuestionType,
    val stem: String,
    val options: List<QuestionPackOption>,
    val answer: List<String>,
    val explanation: String,
    val difficulty: Int,
    val knowledgePointIds: List<String>,
    val sortOrder: Int,
    val deprecated: Boolean = false,
)

data class QuestionPackOption(
    val key: String,
    val content: String,
)
