package com.suilearn.core.model

enum class WrongQuestionStatus {
    ACTIVE,
    MASTERED,
}

data class AnswerRecord(
    val recordId: String,
    val questionId: String,
    val practiceMode: PracticeMode,
    val targetId: String?,
    val userAnswer: List<String>,
    val isCorrect: Boolean,
    val durationMs: Long,
    val answeredAt: Long,
)

data class WrongQuestion(
    val questionId: String,
    val status: WrongQuestionStatus,
    val wrongCount: Int,
    val firstWrongAt: Long,
    val lastWrongAt: Long,
    val masteredAt: Long?,
)

data class FavoriteQuestion(
    val questionId: String,
    val createdAt: Long,
)

enum class MasteryLevel {
    NOT_STARTED,
    WEAK,
    LEARNING,
    MASTERED,
}

data class StatisticsSummary(
    val totalAnsweredQuestions: Int,
    val totalAccuracy: Double,
    val activeWrongQuestionCount: Int,
    val topWeakKnowledgePoints: List<String>,
    val latestPracticeAt: Long?,
    val latestRecoverableSessionId: String?,
)

data class KnowledgePointProgress(
    val questionCount: Int,
    val practicedCount: Int,
    val correctCount: Int,
    val activeWrongCount: Int,
    val masteryLevel: MasteryLevel,
)
