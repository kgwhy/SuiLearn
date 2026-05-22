package com.suilearn.core.repository

import com.suilearn.core.model.KnowledgePointDetail
import com.suilearn.core.model.SearchResult
import com.suilearn.core.model.SearchResultType
import com.suilearn.core.model.StatisticsSummary
import com.suilearn.core.model.WrongQuestionStatus
import com.suilearn.core.model.calculateMasteryLevel

class RoomSearchRepository(
    private val questionRepository: QuestionRepository,
    private val studyPackRepository: StudyPackRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
) : SearchRepository {
    override suspend fun search(query: String): List<SearchResult> {
        val keyword = query.trim()
        if (keyword.isBlank()) return emptyList()

        val categories = studyPackRepository.listCategories().associate { it.categoryId to it.name }
        val knowledgePoints = studyPackRepository.listKnowledgePoints().associateBy { it.knowledgePointId }
        val questions = questionRepository.listQuestions().filter {
            it.stem.contains(keyword, ignoreCase = true) ||
                it.explanation.contains(keyword, ignoreCase = true) ||
                it.answer.any { answer -> answer.contains(keyword, ignoreCase = true) } ||
                it.options.any { option -> option.content.contains(keyword, ignoreCase = true) } ||
                it.knowledgePointIds.any { id ->
                    val point = knowledgePoints[id]
                    point?.name?.contains(keyword, ignoreCase = true) == true ||
                        point?.description?.contains(keyword, ignoreCase = true) == true
                }
        }

        val questionResults = questions.map {
            val matchedKnowledgePointNames = it.knowledgePointIds.mapNotNull { id -> knowledgePoints[id]?.name }
            SearchResult(
                id = it.questionId,
                type = SearchResultType.QUESTION,
                title = it.stem,
                summary = it.explanation.take(80),
                categoryName = categories[it.categoryId].orEmpty(),
                difficulty = it.difficulty,
                hasAnswered = answerRecordRepository.countByQuestion(it.questionId) > 0,
                hasWrongRecord = wrongQuestionRepository.get(it.questionId)?.status == WrongQuestionStatus.ACTIVE,
                matchedFields = listOf("stem", "explanation", "answer", "options") +
                    matchedKnowledgePointNames.map { name -> "knowledgePoint:$name" },
            )
        }

        val knowledgePointResults = knowledgePoints.values
            .filter { it.name.contains(keyword, ignoreCase = true) || it.description.contains(keyword, ignoreCase = true) }
            .map {
                SearchResult(
                    id = it.knowledgePointId,
                    type = SearchResultType.KNOWLEDGE_POINT,
                    title = it.name,
                    summary = it.description,
                    categoryName = categories[it.categoryId].orEmpty(),
                    difficulty = null,
                    hasAnswered = false,
                    hasWrongRecord = false,
                    matchedFields = listOf("name", "description"),
                )
            }

        return questionResults + knowledgePointResults
    }
}

class RoomStatisticsRepository(
    private val questionRepository: QuestionRepository,
    private val studyPackRepository: StudyPackRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
) : StatisticsRepository {
    override suspend fun getSummary(): StatisticsSummary {
        val records = answerRecordRepository.listAll()
        val answered = records.size
        val correct = records.count { it.isCorrect }
        val activeWrongQuestions = wrongQuestionRepository.listActive()

        return StatisticsSummary(
            totalAnsweredQuestions = answered,
            totalAccuracy = if (answered == 0) 0.0 else correct.toDouble() / answered,
            activeWrongQuestionCount = activeWrongQuestions.size,
            topWeakKnowledgePoints = topWeakKnowledgePoints(activeWrongQuestions.map { it.questionId }.toSet()),
            latestPracticeAt = answerRecordRepository.latestAnsweredAt(),
            latestRecoverableSessionId = practiceSessionRepository.getLatestInProgress()?.sessionId,
        )
    }

    override suspend fun getKnowledgePointDetail(knowledgePointId: String): KnowledgePointDetail? {
        val knowledgePoint = studyPackRepository.listKnowledgePoints().firstOrNull { it.knowledgePointId == knowledgePointId }
            ?: return null
        val questions = questionRepository.listQuestions().filter { it.knowledgePointIds.contains(knowledgePointId) }
        if (questions.isEmpty()) return null

        val answeredQuestions = questions.count { answerRecordRepository.countByQuestion(it.questionId) > 0 }
        val activeWrongCount = questions.count { wrongQuestionRepository.get(it.questionId)?.status == WrongQuestionStatus.ACTIVE }
        val totalObjectiveAnswers = questions.sumOf { answerRecordRepository.countByQuestion(it.questionId) }
        val totalObjectiveCorrect = questions.sumOf { answerRecordRepository.countCorrectByQuestion(it.questionId) }

        return KnowledgePointDetail(
            knowledgePoint = knowledgePoint,
            relatedQuestionIds = questions.map { it.questionId },
            masteryLevel = calculateMasteryLevel(
                objectiveQuestionCount = questions.size,
                practicedQuestionCount = answeredQuestions,
                totalObjectiveAnswers = totalObjectiveAnswers,
                totalObjectiveCorrect = totalObjectiveCorrect,
                activeWrongCount = activeWrongCount,
            ),
            activeWrongCount = activeWrongCount,
            answeredCount = answeredQuestions,
        )
    }

    private suspend fun topWeakKnowledgePoints(activeWrongQuestionIds: Set<String>): List<String> {
        if (activeWrongQuestionIds.isEmpty()) return emptyList()
        val questions = questionRepository.listQuestions()
        val weakCounts = questions
            .filter { it.questionId in activeWrongQuestionIds }
            .flatMap { it.knowledgePointIds }
            .groupingBy { it }
            .eachCount()

        return weakCounts.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }
}
