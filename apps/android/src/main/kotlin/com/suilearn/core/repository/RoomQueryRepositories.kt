package com.suilearn.core.repository

import com.suilearn.core.database.CategoryEntity
import com.suilearn.core.database.KnowledgePointEntity
import com.suilearn.core.database.QuestionDao
import com.suilearn.core.database.QuestionEntity
import com.suilearn.core.database.StudyPackDao
import com.suilearn.core.database.decodeStringListFromJson
import com.suilearn.core.model.Category
import com.suilearn.core.model.CategoryProgress
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.KnowledgePointDetail
import com.suilearn.core.model.KnowledgePointProgressSummary
import com.suilearn.core.model.Question
import com.suilearn.core.model.QuestionOption
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.RecentLearningRecord
import com.suilearn.core.model.SearchResult
import com.suilearn.core.model.SearchResultType
import com.suilearn.core.model.StatisticsSummary
import com.suilearn.core.model.WrongQuestionStatus
import com.suilearn.core.model.calculateMasteryLevel

class RoomSearchRepository(
    private val questionDao: QuestionDao,
    private val studyPackDao: StudyPackDao,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
) : SearchRepository {
    override suspend fun search(query: String): List<SearchResult> {
        val keyword = query.trim()
        if (keyword.isBlank()) return emptyList()

        val pattern = keyword.toLikePattern()
        val categories = studyPackDao.listCategories().associate { it.categoryId to it.name }
        val knowledgePoints = studyPackDao.listKnowledgePoints().associateBy { it.knowledgePointId }
        val questions = questionDao.searchQuestions(pattern).map { it.toQuestion(questionDao) }

        val questionResults = questions.map {
            val matchedKnowledgePointNames = it.knowledgePointIds.mapNotNull { id -> knowledgePoints[id]?.name }
            val matchedFields = buildList {
                if (it.stem.contains(keyword, ignoreCase = true)) add("stem")
                if (it.explanation.contains(keyword, ignoreCase = true)) add("explanation")
                if (it.answer.any { answer -> answer.contains(keyword, ignoreCase = true) }) add("answer")
                if (it.options.any { option -> option.content.contains(keyword, ignoreCase = true) }) add("options")
                if (categories[it.categoryId]?.contains(keyword, ignoreCase = true) == true) add("category")
                matchedKnowledgePointNames
                    .filter { name -> name.contains(keyword, ignoreCase = true) }
                    .forEach { name -> add("knowledgePoint:$name") }
            }
            SearchResult(
                id = it.questionId,
                type = SearchResultType.QUESTION,
                title = it.stem,
                summary = it.explanation.take(80),
                categoryName = categories[it.categoryId].orEmpty(),
                difficulty = it.difficulty,
                hasAnswered = answerRecordRepository.countByQuestion(it.questionId) > 0,
                hasWrongRecord = wrongQuestionRepository.get(it.questionId)?.status == WrongQuestionStatus.ACTIVE,
                matchedFields = matchedFields.ifEmpty { listOf("content") },
            )
        }

        val knowledgePointResults = studyPackDao.searchKnowledgePoints(pattern)
            .map {
                val point = it.toDomain()
                SearchResult(
                    id = point.knowledgePointId,
                    type = SearchResultType.KNOWLEDGE_POINT,
                    title = point.name,
                    summary = point.description,
                    categoryName = categories[point.categoryId].orEmpty(),
                    difficulty = null,
                    hasAnswered = false,
                    hasWrongRecord = false,
                    matchedFields = buildList {
                        if (point.name.contains(keyword, ignoreCase = true)) add("name")
                        if (point.description.contains(keyword, ignoreCase = true)) add("description")
                        if (categories[point.categoryId]?.contains(keyword, ignoreCase = true) == true) add("category")
                    }.ifEmpty { listOf("knowledgePoint") },
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
        val answered = records.map { it.questionId }.distinct().size
        val correct = records.count { it.isCorrect }
        val attempts = records.size
        val activeWrongQuestions = wrongQuestionRepository.listActive()
        val knowledgePointProgress = getKnowledgePointProgress()
        val topWeak = knowledgePointProgress
            .filter { it.activeWrongCount > 0 || it.masteryLevel == com.suilearn.core.model.MasteryLevel.WEAK }
            .sortedWith(compareByDescending<KnowledgePointProgressSummary> { it.activeWrongCount }.thenBy { it.accuracy })
            .take(3)

        return StatisticsSummary(
            totalAnsweredQuestions = answered,
            totalAccuracy = if (attempts == 0) 0.0 else correct.toDouble() / attempts,
            activeWrongQuestionCount = activeWrongQuestions.size,
            topWeakKnowledgePoints = topWeak.map { it.knowledgePoint.knowledgePointId },
            latestPracticeAt = answerRecordRepository.latestAnsweredAt(),
            latestRecoverableSessionId = practiceSessionRepository.getLatestInProgress()?.sessionId,
            categoryProgress = getCategoryProgress(),
            knowledgePointProgress = knowledgePointProgress,
            topWeakKnowledgePointProgress = topWeak,
            recentLearningRecords = getRecentLearningRecords(),
        )
    }

    override suspend fun getCategoryProgress(): List<CategoryProgress> {
        val categories = studyPackRepository.listCategories()
        val questions = questionRepository.listQuestions().filterNot { it.isDeprecated }
        val records = answerRecordRepository.listAll()
        val activeWrongIds = wrongQuestionRepository.listActive().map { it.questionId }.toSet()

        return categories.map { category ->
            val categoryQuestions = questions.filter { it.categoryId == category.categoryId }
            val ids = categoryQuestions.map { it.questionId }.toSet()
            val categoryRecords = records.filter { it.questionId in ids }
            CategoryProgress(
                category = category,
                questionCount = categoryQuestions.size,
                practicedCount = categoryRecords.map { it.questionId }.distinct().size,
                accuracy = categoryRecords.accuracy(),
                activeWrongCount = ids.count { it in activeWrongIds },
            )
        }
    }

    override suspend fun getKnowledgePointProgress(): List<KnowledgePointProgressSummary> {
        val categories = studyPackRepository.listCategories().associate { it.categoryId to it.name }
        val knowledgePoints = studyPackRepository.listKnowledgePoints()
        val questions = questionRepository.listQuestions().filterNot { it.isDeprecated }
        val records = answerRecordRepository.listAll()
        val activeWrongIds = wrongQuestionRepository.listActive().map { it.questionId }.toSet()

        return knowledgePoints.map { knowledgePoint ->
            val relatedQuestions = questions.filter { it.knowledgePointIds.contains(knowledgePoint.knowledgePointId) }
            val ids = relatedQuestions.map { it.questionId }.toSet()
            val relatedRecords = records.filter { it.questionId in ids }
            val activeWrongCount = ids.count { it in activeWrongIds }
            KnowledgePointProgressSummary(
                knowledgePoint = knowledgePoint,
                categoryName = categories[knowledgePoint.categoryId].orEmpty(),
                questionCount = relatedQuestions.size,
                practicedCount = relatedRecords.map { it.questionId }.distinct().size,
                accuracy = relatedRecords.accuracy(),
                activeWrongCount = activeWrongCount,
                masteryLevel = calculateMasteryLevel(
                    objectiveQuestionCount = relatedQuestions.size,
                    practicedQuestionCount = relatedRecords.map { it.questionId }.distinct().size,
                    totalObjectiveAnswers = relatedRecords.size,
                    totalObjectiveCorrect = relatedRecords.count { it.isCorrect },
                    activeWrongCount = activeWrongCount,
                ),
            )
        }
    }

    override suspend fun getRecentLearningRecords(limit: Int): List<RecentLearningRecord> {
        val categories = studyPackRepository.listCategories().associate { it.categoryId to it.name }
        return answerRecordRepository.listRecent(limit).mapNotNull { record ->
            val question = questionRepository.getQuestion(record.questionId) ?: return@mapNotNull null
            RecentLearningRecord(
                questionId = question.questionId,
                stem = question.stem,
                categoryName = categories[question.categoryId].orEmpty(),
                isCorrect = record.isCorrect,
                answeredAt = record.answeredAt,
            )
        }
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
}

private fun String.toLikePattern(): String =
    buildString {
        append('%')
        this@toLikePattern.forEach { char ->
            when (char) {
                '%', '_', '\\' -> append('\\').append(char)
                else -> append(char)
            }
        }
        append('%')
    }

private fun List<com.suilearn.core.model.AnswerRecord>.accuracy(): Double =
    if (isEmpty()) 0.0 else count { it.isCorrect }.toDouble() / size

private suspend fun QuestionEntity.toQuestion(questionDao: QuestionDao): Question =
    Question(
        questionId = questionId,
        packId = packId,
        categoryId = categoryId,
        type = QuestionType.valueOf(type),
        stem = stem,
        answer = decodeStringListFromJson(answer),
        explanation = explanation,
        difficulty = difficulty,
        isDeprecated = isDeprecated,
        sortOrder = sortOrder,
        options = questionDao.listOptions(questionId).map {
            QuestionOption(
                optionId = it.optionId,
                questionId = it.questionId,
                optionKey = it.optionKey,
                content = it.content,
                sortOrder = it.sortOrder,
            )
        },
        knowledgePointIds = questionDao.listKnowledgePointRefs(questionId).map { it.knowledgePointId },
    )

private fun KnowledgePointEntity.toDomain(): KnowledgePoint =
    KnowledgePoint(
        knowledgePointId = knowledgePointId,
        packId = packId,
        categoryId = categoryId,
        name = name,
        description = description,
        sortOrder = sortOrder,
    )

private fun CategoryEntity.toDomain(): Category =
    Category(
        categoryId = categoryId,
        packId = packId,
        name = name,
        description = description,
        sortOrder = sortOrder,
    )
