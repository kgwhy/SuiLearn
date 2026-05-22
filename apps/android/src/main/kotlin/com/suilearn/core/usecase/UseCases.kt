package com.suilearn.core.usecase

import com.suilearn.core.common.AppError
import com.suilearn.core.common.AppResult
import com.suilearn.core.common.Clock
import com.suilearn.core.common.IdGenerator
import com.suilearn.core.common.SystemClock
import com.suilearn.core.common.UuidIdGenerator
import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeQuestionState
import com.suilearn.core.model.PracticeSubmission
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.PracticeSessionStatus
import com.suilearn.core.model.ShortAnswerEvaluation
import com.suilearn.core.model.QuestionPack
import com.suilearn.core.model.QuestionPackValidation
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.ShortAnswerReview
import com.suilearn.core.model.SearchResult
import com.suilearn.core.model.MasteryLevel
import com.suilearn.core.model.calculateMasteryLevel
import com.suilearn.core.common.HomeSummary
import com.suilearn.core.repository.AnswerRecordRepository
import com.suilearn.core.repository.FavoriteRepository
import com.suilearn.core.repository.PracticeSessionRepository
import com.suilearn.core.repository.QuestionRepository
import com.suilearn.core.repository.SearchRepository
import com.suilearn.core.repository.SettingsRepository
import com.suilearn.core.repository.StatisticsRepository
import com.suilearn.core.repository.StudyPackRepository
import com.suilearn.core.repository.WrongQuestionRepository

class InitializeQuestionPackUseCase(
    private val studyPackRepository: StudyPackRepository,
    private val questionRepository: QuestionRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock = SystemClock,
) {
    suspend fun execute(pack: QuestionPack): AppResult<Unit> {
        val errors = QuestionPackValidation.validate(pack)
        if (errors.isNotEmpty()) {
            return AppResult.Failure(AppError.ValidationError(errors.joinToString("; ")))
        }

        studyPackRepository.upsertPack(
            com.suilearn.core.model.StudyPack(
                packId = pack.packId,
                name = pack.packName,
                description = pack.description,
                packVersion = pack.packVersion,
                schemaVersion = pack.schemaVersion,
                importedAt = clock.now(),
            )
        )
        settingsRepository.setCurrentPackId(pack.packId)
        studyPackRepository.replaceCategories(
            pack.categories.map {
                com.suilearn.core.model.Category(
                    categoryId = it.categoryId,
                    packId = pack.packId,
                    name = it.name,
                    description = it.description,
                    sortOrder = it.sortOrder,
                )
            }
        )
        studyPackRepository.replaceKnowledgePoints(
            pack.knowledgePoints.map {
                com.suilearn.core.model.KnowledgePoint(
                    knowledgePointId = it.knowledgePointId,
                    packId = pack.packId,
                    categoryId = it.categoryId,
                    name = it.name,
                    description = it.description,
                    sortOrder = it.sortOrder,
                )
            }
        )
        questionRepository.replaceAll(pack)
        return AppResult.Success(Unit)
    }
}

class BuildPracticeSessionUseCase(
    private val questionRepository: QuestionRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val favoriteRepository: FavoriteRepository,
    private val clock: Clock = SystemClock,
    private val idGenerator: IdGenerator = UuidIdGenerator,
) {
    suspend fun execute(mode: PracticeMode, targetId: String? = null): AppResult<PracticeSession> {
        val candidates = when (mode) {
            PracticeMode.SEQUENTIAL -> questionRepository.listQuestions()
            PracticeMode.RANDOM -> questionRepository.listQuestions().shuffled()
            PracticeMode.CATEGORY -> questionRepository.listQuestions().filter { it.categoryId == targetId }
            PracticeMode.KNOWLEDGE_POINT -> questionRepository.listQuestions().filter { it.knowledgePointIds.contains(targetId) }
            PracticeMode.WRONG_QUESTION -> questionRepository.listQuestions().filter {
                wrongQuestionRepository.get(it.questionId)?.status == com.suilearn.core.model.WrongQuestionStatus.ACTIVE
            }
            PracticeMode.FAVORITE -> questionRepository.listQuestions().filter { favoriteRepository.isFavorite(it.questionId) }
        }.filterNot { it.isDeprecated }

        if (candidates.isEmpty()) {
            return AppResult.Failure(AppError.DataError("当前没有可练习的题目。"))
        }

        val session = PracticeSession(
            sessionId = idGenerator.newId(),
            practiceMode = mode,
            targetId = targetId,
            questionIds = candidates.map { it.questionId },
            currentIndex = 0,
            status = PracticeSessionStatus.IN_PROGRESS,
            createdAt = clock.now(),
            updatedAt = clock.now(),
        )
        practiceSessionRepository.save(session)
        return AppResult.Success(session)
    }
}

class StartPracticeFromQuestionUseCase(
    private val questionRepository: QuestionRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
    private val clock: Clock = SystemClock,
    private val idGenerator: IdGenerator = UuidIdGenerator,
) {
    suspend fun execute(questionId: String): AppResult<PracticeSession> {
        val question = questionRepository.getQuestion(questionId)
            ?: return AppResult.Failure(AppError.DataError("未找到题目：$questionId"))

        if (question.isDeprecated) {
            return AppResult.Failure(AppError.ValidationError("该题目已弃用，无法开始练习。"))
        }

        val session = PracticeSession(
            sessionId = idGenerator.newId(),
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = question.categoryId,
            questionIds = listOf(question.questionId),
            currentIndex = 0,
            status = PracticeSessionStatus.IN_PROGRESS,
            createdAt = clock.now(),
            updatedAt = clock.now(),
        )
        practiceSessionRepository.save(session)
        return AppResult.Success(session)
    }
}

class ResumePracticeSessionUseCase(
    private val questionRepository: QuestionRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
) {
    suspend fun execute(): AppResult<PracticeSession?> {
        val session = practiceSessionRepository.getLatestInProgress() ?: return AppResult.Success(null)
        val hasAnyValidQuestion = session.questionIds.any { questionRepository.getQuestion(it)?.isDeprecated == false }
        return if (hasAnyValidQuestion) {
            AppResult.Success(session)
        } else {
            practiceSessionRepository.markAbandoned(session.sessionId)
            AppResult.Success(null)
        }
    }
}

class SubmitAnswerUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val favoriteRepository: FavoriteRepository,
    private val clock: Clock = SystemClock,
    private val idGenerator: IdGenerator = UuidIdGenerator,
) {
    suspend fun execute(
        practiceMode: PracticeMode,
        targetId: String?,
        questionId: String,
        userAnswer: List<String>,
        durationMs: Long,
    ): AppResult<PracticeSubmission> {
        val question = questionRepository.getQuestion(questionId)
            ?: return AppResult.Failure(AppError.DataError("未找到题目：$questionId"))

        if (question.type == QuestionType.SHORT_ANSWER) {
            return AppResult.Failure(AppError.ValidationError("简答题必须使用 EvaluateShortAnswerUseCase。"))
        }

        return recordSubmission(
            question = question,
            practiceMode = practiceMode,
            targetId = targetId,
            questionId = questionId,
            userAnswer = userAnswer,
            durationMs = durationMs,
        )
    }

    private suspend fun recordSubmission(
        question: com.suilearn.core.model.Question,
        practiceMode: PracticeMode,
        targetId: String?,
        questionId: String,
        userAnswer: List<String>,
        durationMs: Long,
    ): AppResult<PracticeSubmission> {
        val isCorrect = when (question.type) {
            QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE -> userAnswer == question.answer
            QuestionType.MULTIPLE_CHOICE -> userAnswer.toSet() == question.answer.toSet()
            QuestionType.SHORT_ANSWER -> false
        }

        val now = clock.now()
        val record = AnswerRecord(
            recordId = idGenerator.newId(),
            questionId = questionId,
            practiceMode = practiceMode,
            targetId = targetId,
            userAnswer = userAnswer,
            isCorrect = isCorrect,
            durationMs = durationMs,
            answeredAt = now,
        )

        answerRecordRepository.add(record)

        if (!isCorrect) {
            wrongQuestionRepository.upsertWrong(questionId, now)
        }

        val wrong = wrongQuestionRepository.get(questionId)
        val favorite = favoriteRepository.isFavorite(questionId)
        return AppResult.Success(
            PracticeSubmission(
                isCorrect = isCorrect,
                explanation = question.explanation,
                answer = userAnswer,
                questionType = question.type,
                isFavorite = favorite,
                wrongStatus = wrong?.status,
                allowNext = true,
            )
        )
    }
}

class EvaluateShortAnswerUseCase(
    private val questionRepository: QuestionRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val favoriteRepository: FavoriteRepository,
    private val clock: Clock = SystemClock,
    private val idGenerator: IdGenerator = UuidIdGenerator,
) {
    suspend fun execute(
        practiceMode: PracticeMode,
        targetId: String?,
        questionId: String,
        userAnswer: List<String>,
        review: ShortAnswerReview,
        durationMs: Long,
    ): AppResult<ShortAnswerEvaluation> {
        val question = questionRepository.getQuestion(questionId)
            ?: return AppResult.Failure(AppError.DataError("未找到题目：$questionId"))

        if (question.type != QuestionType.SHORT_ANSWER) {
            return AppResult.Failure(AppError.ValidationError("EvaluateShortAnswerUseCase 仅接受简答题。"))
        }

        val now = clock.now()
        val isCorrect = review == ShortAnswerReview.PASSED
        answerRecordRepository.add(
            AnswerRecord(
                recordId = idGenerator.newId(),
                questionId = questionId,
                practiceMode = practiceMode,
                targetId = targetId,
                userAnswer = userAnswer,
                isCorrect = isCorrect,
                durationMs = durationMs,
                answeredAt = now,
            )
        )

        if (!isCorrect) {
            wrongQuestionRepository.upsertWrong(questionId, now)
        }

        val wrong = wrongQuestionRepository.get(questionId)
        return AppResult.Success(
            ShortAnswerEvaluation(
                review = review,
                explanation = question.explanation,
                answer = userAnswer,
                questionType = question.type,
                isFavorite = favoriteRepository.isFavorite(questionId),
                wrongStatus = wrong?.status,
                allowNext = true,
            )
        )
    }
}

class ToggleFavoriteQuestionUseCase(
    private val favoriteRepository: FavoriteRepository,
    private val clock: Clock = SystemClock,
) {
    suspend fun execute(questionId: String): AppResult<Boolean> {
        return AppResult.Success(favoriteRepository.toggle(questionId, clock.now()))
    }
}

class MarkWrongQuestionMasteredUseCase(
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val clock: Clock = SystemClock,
) {
    suspend fun execute(questionId: String): AppResult<Unit> {
        wrongQuestionRepository.markMastered(questionId, clock.now())
        return AppResult.Success(Unit)
    }
}

class SearchLearningContentUseCase(
    private val searchRepository: SearchRepository,
) {
    suspend fun execute(query: String) = AppResult.Success(searchRepository.search(query))
}

class GetKnowledgePointDetailUseCase(
    private val statisticsRepository: StatisticsRepository,
) {
    suspend fun execute(knowledgePointId: String) = AppResult.Success(statisticsRepository.getKnowledgePointDetail(knowledgePointId))
}

class GetHomeSummaryUseCase(
    private val studyPackRepository: StudyPackRepository,
    private val questionRepository: QuestionRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
) {
    suspend fun execute(): AppResult<HomeSummary> {
        val pack = studyPackRepository.getCurrentPack()
            ?: return AppResult.Failure(AppError.DataError("尚未初始化题包。"))
        val knowledgePoints = studyPackRepository.listKnowledgePoints()
        val questions = questionRepository.listQuestions()
        val progress = knowledgePoints.map { knowledgePoint ->
            val relatedQuestions = questions.filter { it.knowledgePointIds.contains(knowledgePoint.knowledgePointId) }
            val practicedCount = relatedQuestions.count { answerRecordRepository.countByQuestion(it.questionId) > 0 }
            val totalObjectiveAnswers = relatedQuestions.sumOf { answerRecordRepository.countByQuestion(it.questionId) }
            val totalObjectiveCorrect = relatedQuestions.sumOf { answerRecordRepository.countCorrectByQuestion(it.questionId) }
            val activeWrongCount = relatedQuestions.count { wrongQuestionRepository.get(it.questionId)?.status == com.suilearn.core.model.WrongQuestionStatus.ACTIVE }
            val masteryLevel = calculateMasteryLevel(
                objectiveQuestionCount = relatedQuestions.size,
                practicedQuestionCount = practicedCount,
                totalObjectiveAnswers = totalObjectiveAnswers,
                totalObjectiveCorrect = totalObjectiveCorrect,
                activeWrongCount = activeWrongCount,
            )
            com.suilearn.core.model.KnowledgePointProgress(
                questionCount = relatedQuestions.size,
                practicedCount = practicedCount,
                correctCount = totalObjectiveCorrect,
                activeWrongCount = activeWrongCount,
                masteryLevel = masteryLevel,
            )
        }
        val answerCount = answerRecordRepository.listAll().size
        val correctCount = answerRecordRepository.listAll().count { it.isCorrect }
        val activeWrongCount = wrongQuestionRepository.listActive().size
        val distinctLearningDays = answerRecordRepository.listAll()
            .map { it.answeredAt / (24 * 60 * 60 * 1000) }
            .distinct()
            .count()

        return AppResult.Success(
            HomeSummary(
                studyPack = pack,
                todayTitle = "今天学习",
                resumeSessionId = practiceSessionRepository.getLatestInProgress()?.sessionId,
                totalQuestionCount = questions.size,
                totalPracticedCount = answerCount,
                totalCorrectRate = if (answerCount == 0) 0 else (correctCount * 100 / answerCount),
                activeWrongCount = activeWrongCount,
                weakKnowledgePoints = progress.sortedBy { it.masteryLevel.ordinal }.take(3),
                recentRecords = answerRecordRepository.listRecent(5),
                recentLearningDays = distinctLearningDays,
            )
        )
    }
}

class GetStatisticsSummaryUseCase(
    private val statisticsRepository: StatisticsRepository,
) {
    suspend fun execute() = AppResult.Success(statisticsRepository.getSummary())
}

class ResetLearningDataUseCase(
    private val settingsRepository: SettingsRepository,
    private val practiceSessionRepository: PracticeSessionRepository,
    private val answerRecordRepository: AnswerRecordRepository,
    private val wrongQuestionRepository: WrongQuestionRepository,
    private val favoriteRepository: FavoriteRepository,
) {
    suspend fun execute(): AppResult<Unit> {
        settingsRepository.resetLearningSettingsOnly()
        answerRecordRepository.clear()
        wrongQuestionRepository.clear()
        favoriteRepository.clear()
        practiceSessionRepository.clear()
        return AppResult.Success(Unit)
    }
}
