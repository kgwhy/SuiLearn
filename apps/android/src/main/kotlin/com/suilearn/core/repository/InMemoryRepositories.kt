package com.suilearn.core.repository

import com.suilearn.core.common.Clock
import com.suilearn.core.common.IdGenerator
import com.suilearn.core.common.SystemClock
import com.suilearn.core.common.UuidIdGenerator
import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.Category
import com.suilearn.core.model.CategoryProgress
import com.suilearn.core.model.FavoriteQuestion
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.KnowledgePointDetail
import com.suilearn.core.model.KnowledgePointProgress
import com.suilearn.core.model.KnowledgePointProgressSummary
import com.suilearn.core.model.MasteryLevel
import com.suilearn.core.model.calculateMasteryLevel
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.PracticeSessionStatus
import com.suilearn.core.model.Question
import com.suilearn.core.model.QuestionPack
import com.suilearn.core.model.QuestionPackCategory
import com.suilearn.core.model.QuestionPackKnowledgePoint
import com.suilearn.core.model.RecentLearningRecord
import com.suilearn.core.model.SearchResult
import com.suilearn.core.model.SearchResultType
import com.suilearn.core.model.StatisticsSummary
import com.suilearn.core.model.StudyPack
import com.suilearn.core.model.WrongQuestion
import com.suilearn.core.model.WrongQuestionStatus

class InMemoryStudyPackRepository : StudyPackRepository {
    private var currentPack: StudyPack? = null
    private val knowledgePoints = linkedMapOf<String, KnowledgePoint>()
    private val categories = linkedMapOf<String, Category>()

    override suspend fun getCurrentPack(): StudyPack? = currentPack

    override suspend fun upsertPack(pack: StudyPack) {
        currentPack = pack
    }

    override suspend fun listKnowledgePoints(): List<KnowledgePoint> = knowledgePoints.values.toList()

    override suspend fun listCategories(): List<Category> = categories.values.toList()

    override suspend fun replaceKnowledgePoints(items: List<KnowledgePoint>) {
        knowledgePoints.clear()
        items.forEach { knowledgePoints[it.knowledgePointId] = it }
    }

    override suspend fun replaceCategories(items: List<Category>) {
        categories.clear()
        items.forEach { categories[it.categoryId] = it }
    }
}

class InMemoryQuestionRepository : QuestionRepository {
    private val questions = linkedMapOf<String, Question>()

    override suspend fun listQuestions(): List<Question> = questions.values.sortedBy { it.sortOrder }

    override suspend fun getQuestion(questionId: String): Question? = questions[questionId]

    override suspend fun replaceAll(pack: QuestionPack) {
        questions.clear()
        pack.questions.forEach { item ->
            questions[item.questionId] = Question(
                questionId = item.questionId,
                packId = pack.packId,
                categoryId = item.categoryId,
                type = item.type,
                stem = item.stem,
                answer = item.answer,
                explanation = item.explanation,
                difficulty = item.difficulty,
                isDeprecated = item.deprecated,
                sortOrder = item.sortOrder,
                options = item.options.mapIndexed { index, option ->
                    com.suilearn.core.model.QuestionOption(
                        optionId = "${item.questionId}_option_${index + 1}",
                        questionId = item.questionId,
                        optionKey = option.key,
                        content = option.content,
                        sortOrder = index + 1,
                    )
                },
                knowledgePointIds = item.knowledgePointIds,
            )
        }
    }
}

class InMemoryAnswerRecordRepository(
    private val clock: Clock = SystemClock,
    private val idGenerator: IdGenerator = UuidIdGenerator,
) : AnswerRecordRepository {
    private val records = mutableListOf<AnswerRecord>()

    override suspend fun listAll(): List<AnswerRecord> = records.toList()

    override suspend fun listRecent(limit: Int): List<AnswerRecord> = records.sortedByDescending { it.answeredAt }.take(limit)

    override suspend fun add(record: AnswerRecord) {
        records += record
    }

    override suspend fun clear() {
        records.clear()
    }

    override suspend fun countByQuestion(questionId: String): Int = records.count { it.questionId == questionId }

    override suspend fun countCorrectByQuestion(questionId: String): Int = records.count { it.questionId == questionId && it.isCorrect }

    override suspend fun latestAnsweredAt(): Long? = records.maxOfOrNull { it.answeredAt }
}

class InMemoryPracticeSessionRepository(
    private val clock: Clock = SystemClock,
) : PracticeSessionRepository {
    private val sessions = linkedMapOf<String, PracticeSession>()

    override suspend fun save(session: PracticeSession) {
        sessions[session.sessionId] = session
    }

    override suspend fun update(session: PracticeSession) {
        sessions[session.sessionId] = session
    }

    override suspend fun getLatestInProgress(): PracticeSession? =
        sessions.values.filter { it.status == PracticeSessionStatus.IN_PROGRESS }
            .sortedByDescending { it.updatedAt }
            .firstOrNull()

    override suspend fun find(sessionId: String): PracticeSession? = sessions[sessionId]

    override suspend fun markAbandoned(sessionId: String) {
        sessions[sessionId] = sessions[sessionId]?.copy(status = PracticeSessionStatus.ABANDONED, updatedAt = clock.now())
            ?: return
    }

    override suspend fun markCompleted(sessionId: String) {
        sessions[sessionId] = sessions[sessionId]?.copy(status = PracticeSessionStatus.COMPLETED, updatedAt = clock.now())
            ?: return
    }

    override suspend fun clear() {
        sessions.clear()
    }
}

class InMemoryWrongQuestionRepository(
    private val clock: Clock = SystemClock,
) : WrongQuestionRepository {
    private val wrongQuestions = linkedMapOf<String, WrongQuestion>()

    override suspend fun listAll(): List<WrongQuestion> =
        wrongQuestions.values.sortedWith(
            compareBy<WrongQuestion> { if (it.status == WrongQuestionStatus.ACTIVE) 0 else 1 }
                .thenByDescending { it.lastWrongAt }
        )

    override suspend fun listActive(): List<WrongQuestion> = wrongQuestions.values.filter { it.status == WrongQuestionStatus.ACTIVE }

    override suspend fun upsertWrong(questionId: String, at: Long) {
        val current = wrongQuestions[questionId]
        wrongQuestions[questionId] = if (current == null) {
            WrongQuestion(
                questionId = questionId,
                status = WrongQuestionStatus.ACTIVE,
                wrongCount = 1,
                firstWrongAt = at,
                lastWrongAt = at,
                masteredAt = null,
            )
        } else {
            current.copy(
                status = WrongQuestionStatus.ACTIVE,
                wrongCount = current.wrongCount + 1,
                lastWrongAt = at,
                masteredAt = null,
            )
        }
    }

    override suspend fun markMastered(questionId: String, at: Long) {
        val current = wrongQuestions[questionId] ?: return
        wrongQuestions[questionId] = current.copy(status = WrongQuestionStatus.MASTERED, masteredAt = at)
    }

    override suspend fun get(questionId: String): WrongQuestion? = wrongQuestions[questionId]

    override suspend fun clear() {
        wrongQuestions.clear()
    }
}

class InMemoryFavoriteRepository(
    private val clock: Clock = SystemClock,
) : FavoriteRepository {
    private val favorites = linkedMapOf<String, FavoriteQuestion>()

    override suspend fun listAll(): List<FavoriteQuestion> = favorites.values.toList()

    override suspend fun toggle(questionId: String, now: Long): Boolean {
        if (favorites.containsKey(questionId)) {
            favorites.remove(questionId)
            return false
        }
        favorites[questionId] = FavoriteQuestion(questionId, now)
        return true
    }

    override suspend fun isFavorite(questionId: String): Boolean = favorites.containsKey(questionId)

    override suspend fun clear() {
        favorites.clear()
    }
}

class InMemorySettingsRepository : SettingsRepository {
    private var currentPackId: String? = null

    override suspend fun getCurrentPackId(): String? = currentPackId

    override suspend fun setCurrentPackId(packId: String?) {
        currentPackId = packId
    }

    override suspend fun resetLearningSettingsOnly() {
        // In-memory settings only tracks the selected pack.
        // Resetting learning data should not wipe the imported pack or content.
    }
}

class InMemorySearchRepository(
    private val questionRepository: QuestionRepository,
    private val studyPackRepository: StudyPackRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
) : SearchRepository {
    override suspend fun search(query: String): List<SearchResult> {
        val keyword = query.trim()
        if (keyword.isBlank()) return emptyList()

        val categoryNames = studyPackRepository.listCategories().associate { it.categoryId to it.name }
        val knowledgePoints = studyPackRepository.listKnowledgePoints().associateBy { it.knowledgePointId }
        val questions = questionRepository.listQuestions().filter {
            it.stem.contains(keyword, ignoreCase = true) ||
                it.explanation.contains(keyword, ignoreCase = true) ||
                it.answer.any { answer -> answer.contains(keyword, ignoreCase = true) } ||
                it.options.any { option -> option.content.contains(keyword, ignoreCase = true) } ||
                categoryNames[it.categoryId]?.contains(keyword, ignoreCase = true) == true ||
                it.knowledgePointIds.any { id ->
                    val point = knowledgePoints[id]
                    point?.name?.contains(keyword, ignoreCase = true) == true ||
                        point?.description?.contains(keyword, ignoreCase = true) == true
                }
        }

        val questionResults = questions.map {
            val matchedKnowledgePointNames = it.knowledgePointIds.mapNotNull { id -> knowledgePoints[id]?.name }
            val matchedFields = buildList {
                if (it.stem.contains(keyword, ignoreCase = true)) add("stem")
                if (it.explanation.contains(keyword, ignoreCase = true)) add("explanation")
                if (it.answer.any { answer -> answer.contains(keyword, ignoreCase = true) }) add("answer")
                if (it.options.any { option -> option.content.contains(keyword, ignoreCase = true) }) add("options")
                if (categoryNames[it.categoryId]?.contains(keyword, ignoreCase = true) == true) add("category")
                matchedKnowledgePointNames
                    .filter { name -> name.contains(keyword, ignoreCase = true) }
                    .forEach { name -> add("knowledgePoint:$name") }
            }
            SearchResult(
                id = it.questionId,
                type = SearchResultType.QUESTION,
                title = it.stem,
                summary = it.explanation.take(80),
                categoryName = categoryNames[it.categoryId].orEmpty(),
                difficulty = it.difficulty,
                hasAnswered = answerRecordRepository.countByQuestion(it.questionId) > 0,
                hasWrongRecord = wrongQuestionRepository.get(it.questionId)?.status == WrongQuestionStatus.ACTIVE,
                matchedFields = matchedFields.ifEmpty { listOf("content") },
            )
        }

        val knowledgePointResults = studyPackRepository.listKnowledgePoints()
            .filter {
                it.name.contains(keyword, ignoreCase = true) ||
                    it.description.contains(keyword, ignoreCase = true) ||
                    categoryNames[it.categoryId]?.contains(keyword, ignoreCase = true) == true
            }
            .map {
                SearchResult(
                    id = it.knowledgePointId,
                    type = SearchResultType.KNOWLEDGE_POINT,
                    title = it.name,
                    summary = it.description,
                    categoryName = categoryNames[it.categoryId].orEmpty(),
                    difficulty = null,
                    hasAnswered = false,
                    hasWrongRecord = false,
                    matchedFields = buildList {
                        if (it.name.contains(keyword, ignoreCase = true)) add("name")
                        if (it.description.contains(keyword, ignoreCase = true)) add("description")
                        if (categoryNames[it.categoryId]?.contains(keyword, ignoreCase = true) == true) add("category")
                    }.ifEmpty { listOf("knowledgePoint") },
                )
            }

        return questionResults + knowledgePointResults
    }
}

class InMemoryStatisticsRepository(
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
        val accuracy = if (records.isEmpty()) 0.0 else correct.toDouble() / records.size
        val activeWrongCount = wrongQuestionRepository.listActive().size
        val knowledgeProgress = getKnowledgePointProgress()
        val topWeak = knowledgeProgress
            .filter { it.activeWrongCount > 0 || it.masteryLevel == MasteryLevel.WEAK }
            .sortedWith(compareByDescending<KnowledgePointProgressSummary> { it.activeWrongCount }.thenBy { it.accuracy })
            .take(3)

        return StatisticsSummary(
            totalAnsweredQuestions = answered,
            totalAccuracy = accuracy,
            activeWrongQuestionCount = activeWrongCount,
            topWeakKnowledgePoints = topWeak.map { it.knowledgePoint.knowledgePointId },
            latestPracticeAt = answerRecordRepository.latestAnsweredAt(),
            latestRecoverableSessionId = practiceSessionRepository.getLatestInProgress()?.sessionId,
            categoryProgress = getCategoryProgress(),
            knowledgePointProgress = knowledgeProgress,
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
        val points = studyPackRepository.listKnowledgePoints()
        val questions = questionRepository.listQuestions().filterNot { it.isDeprecated }
        val records = answerRecordRepository.listAll()
        val activeWrongIds = wrongQuestionRepository.listActive().map { it.questionId }.toSet()

        return points.map { point ->
            val relatedQuestions = questions.filter { it.knowledgePointIds.contains(point.knowledgePointId) }
            val ids = relatedQuestions.map { it.questionId }.toSet()
            val relatedRecords = records.filter { it.questionId in ids }
            val activeWrongCount = ids.count { it in activeWrongIds }
            KnowledgePointProgressSummary(
                knowledgePoint = point,
                categoryName = categories[point.categoryId].orEmpty(),
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
        val questions = questionRepository.listQuestions().filter { it.knowledgePointIds.contains(knowledgePointId) }
        if (questions.isEmpty()) return null

        val answeredQuestions = questions.count { answerRecordRepository.countByQuestion(it.questionId) > 0 }
        val activeWrongCount = questions.count { wrongQuestionRepository.get(it.questionId)?.status == WrongQuestionStatus.ACTIVE }
        val totalObjectiveAnswers = questions.sumOf { answerRecordRepository.countByQuestion(it.questionId) }
        val totalObjectiveCorrect = questions.sumOf { answerRecordRepository.countCorrectByQuestion(it.questionId) }
        val masteryLevel = calculateMasteryLevel(
            objectiveQuestionCount = questions.size,
            practicedQuestionCount = answeredQuestions,
            totalObjectiveAnswers = totalObjectiveAnswers,
            totalObjectiveCorrect = totalObjectiveCorrect,
            activeWrongCount = activeWrongCount,
        )

        val knowledgePoint = studyPackRepository.listKnowledgePoints().firstOrNull { it.knowledgePointId == knowledgePointId }
            ?: return null

        return KnowledgePointDetail(
            knowledgePoint = knowledgePoint,
            relatedQuestionIds = questions.map { it.questionId },
            masteryLevel = masteryLevel,
            activeWrongCount = activeWrongCount,
            answeredCount = answeredQuestions,
        )
    }
}

private fun List<AnswerRecord>.accuracy(): Double =
    if (isEmpty()) 0.0 else count { it.isCorrect }.toDouble() / size
