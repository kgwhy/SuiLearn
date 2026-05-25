package com.suilearn.core.model

enum class PracticeMode {
    SEQUENTIAL,
    RANDOM,
    CATEGORY,
    KNOWLEDGE_POINT,
    WRONG_QUESTION,
    FAVORITE,
}

enum class PracticeSessionStatus {
    IN_PROGRESS,
    COMPLETED,
    ABANDONED,
}

data class PracticeSession(
    val sessionId: String,
    val practiceMode: PracticeMode,
    val targetId: String?,
    val questionIds: List<String>,
    val currentIndex: Int,
    val status: PracticeSessionStatus,
    val createdAt: Long,
    val updatedAt: Long,
)

enum class ShortAnswerReview {
    PASSED,
    NOT_PASSED,
}

data class ShortAnswerEvaluation(
    val review: ShortAnswerReview,
    val explanation: String,
    val answer: List<String>,
    val questionType: QuestionType,
    val isFavorite: Boolean,
    val wrongStatus: WrongQuestionStatus?,
    val allowNext: Boolean,
)

data class PracticeQuestionState(
    val session: PracticeSession,
    val question: Question,
    val index: Int,
    val total: Int,
    val isFavorite: Boolean = false,
    val selectedAnswers: Set<String> = emptySet(),
    val shortAnswerText: String = "",
    val submitted: Boolean = false,
    val isCorrect: Boolean? = null,
    val showExplanation: Boolean = false,
    val loading: Boolean = false,
)

data class PracticeSubmission(
    val isCorrect: Boolean,
    val explanation: String,
    val answer: List<String>,
    val questionType: QuestionType,
    val isFavorite: Boolean,
    val wrongStatus: WrongQuestionStatus?,
    val allowNext: Boolean,
)
