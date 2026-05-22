package com.suilearn.core.usecase

import com.suilearn.core.common.AppResult
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.MasteryLevel
import com.suilearn.core.model.ShortAnswerReview
import com.suilearn.core.model.QuestionPack
import com.suilearn.core.model.QuestionPackCategory
import com.suilearn.core.model.QuestionPackKnowledgePoint
import com.suilearn.core.model.QuestionPackOption
import com.suilearn.core.model.QuestionPackQuestion
import com.suilearn.core.model.QuestionType
import com.suilearn.core.repository.InMemoryFavoriteRepository
import com.suilearn.core.repository.InMemoryAnswerRecordRepository
import com.suilearn.core.repository.InMemoryPracticeSessionRepository
import com.suilearn.core.repository.InMemoryQuestionRepository
import com.suilearn.core.repository.InMemorySearchRepository
import com.suilearn.core.repository.InMemorySettingsRepository
import com.suilearn.core.repository.InMemoryStatisticsRepository
import com.suilearn.core.repository.InMemoryStudyPackRepository
import com.suilearn.core.repository.InMemoryWrongQuestionRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UseCaseTest {
    private fun samplePack(): QuestionPack = QuestionPack(
        schemaVersion = 1,
        packId = "java_interview_v1",
        packName = "Java 八股学习包",
        packVersion = 1,
        description = "Sample pack",
        categories = listOf(
            QuestionPackCategory("jvm", "JVM", "JVM", 1),
            QuestionPackCategory("concurrency", "并发", "Concurrency", 2),
        ),
        knowledgePoints = listOf(
            QuestionPackKnowledgePoint("volatile", "concurrency", "volatile", "volatile", 1),
        ),
        questions = listOf(
            QuestionPackQuestion(
                questionId = "concurrency_volatile_001",
                categoryId = "concurrency",
                type = QuestionType.SINGLE_CHOICE,
                stem = "volatile 的作用是什么？",
                options = listOf(
                    QuestionPackOption("A", "保证复合操作原子性"),
                    QuestionPackOption("B", "保证可见性和一定的有序性"),
                ),
                answer = listOf("B"),
                explanation = "volatile 保证可见性。",
                difficulty = 2,
                knowledgePointIds = listOf("volatile"),
                sortOrder = 1,
            )
        ),
    )

    @Test
    fun `initialize pack validates and stores content`() = runBlocking {
        val studyPackRepository = InMemoryStudyPackRepository()
        val questionRepository = InMemoryQuestionRepository()
        val settingsRepository = InMemorySettingsRepository()
        val useCase = InitializeQuestionPackUseCase(studyPackRepository, questionRepository, settingsRepository)

        val result = useCase.execute(samplePack())

        assertTrue(result is AppResult.Success)
        assertEquals("java_interview_v1", studyPackRepository.getCurrentPack()?.packId)
        assertEquals(1, questionRepository.listQuestions().size)
    }

    @Test
    fun `submit answer writes wrong record on mistake`() = runBlocking {
        val questionRepository = InMemoryQuestionRepository()
        questionRepository.replaceAll(samplePack())
        val answerRecordRepository = InMemoryAnswerRecordRepository()
        val wrongQuestionRepository = InMemoryWrongQuestionRepository()
        val favoriteRepository = InMemoryFavoriteRepository()
        val useCase = SubmitAnswerUseCase(questionRepository, answerRecordRepository, wrongQuestionRepository, favoriteRepository)

        val result = useCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = "concurrency_volatile_001",
            userAnswer = listOf("A"),
            durationMs = 1200,
        )

        assertTrue(result is AppResult.Success)
        assertTrue(result.data.isCorrect.not())
        assertEquals(1, wrongQuestionRepository.listActive().size)
        assertEquals(1, answerRecordRepository.listAll().size)
    }

    @Test
    fun `evaluate short answer writes answer record for accuracy`() = runBlocking {
        val shortAnswerPack = samplePack().copy(
            questions = listOf(
                QuestionPackQuestion(
                    questionId = "concurrency_short_001",
                    categoryId = "concurrency",
                    type = QuestionType.SHORT_ANSWER,
                    stem = "简述 volatile 的作用。",
                    options = emptyList(),
                    answer = listOf("保证可见性"),
                    explanation = "volatile 能保证可见性。",
                    difficulty = 2,
                    knowledgePointIds = listOf("volatile"),
                    sortOrder = 1,
                )
            )
        )
        val questionRepository = InMemoryQuestionRepository().apply { replaceAll(shortAnswerPack) }
        val answerRecordRepository = InMemoryAnswerRecordRepository()
        val wrongQuestionRepository = InMemoryWrongQuestionRepository()
        val favoriteRepository = InMemoryFavoriteRepository()
        val useCase = EvaluateShortAnswerUseCase(
            questionRepository,
            answerRecordRepository,
            wrongQuestionRepository,
            favoriteRepository,
        )

        val result = useCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = "concurrency_short_001",
            userAnswer = listOf("保证可见性"),
            review = ShortAnswerReview.PASSED,
            durationMs = 1200,
        )

        assertTrue(result is AppResult.Success)
        assertEquals(1, answerRecordRepository.listAll().size)
        assertTrue(answerRecordRepository.listAll().first().isCorrect)
        assertEquals(0, wrongQuestionRepository.listActive().size)
        assertEquals(ShortAnswerReview.PASSED, result.data.review)
        assertTrue(result.data.allowNext)
    }

    @Test
    fun `search returns keyword matches`() = runBlocking {
        val studyPackRepository = InMemoryStudyPackRepository().apply {
            replaceKnowledgePoints(
                listOf(
                    com.suilearn.core.model.KnowledgePoint(
                        knowledgePointId = "volatile",
                        packId = "java_interview_v1",
                        categoryId = "concurrency",
                        name = "volatile",
                        description = "可见性",
                        sortOrder = 1,
                    )
                )
            )
        }
        val questionRepository = InMemoryQuestionRepository().apply { replaceAll(samplePack()) }
        val answerRecordRepository = InMemoryAnswerRecordRepository()
        val favoriteRepository = InMemoryFavoriteRepository()
        val wrongQuestionRepository = InMemoryWrongQuestionRepository()
        val searchRepository = InMemorySearchRepository(
            questionRepository,
            studyPackRepository,
            answerRecordRepository,
            wrongQuestionRepository,
        )
        val useCase = SearchLearningContentUseCase(searchRepository)

        val result = useCase.execute("volatile")

        assertTrue(result.data.isNotEmpty())
    }

    @Test
    fun `statistics uses recoverable session`() = runBlocking {
        val questionRepository = InMemoryQuestionRepository().apply { replaceAll(samplePack()) }
        val answerRecordRepository = InMemoryAnswerRecordRepository()
        val studyPackRepository = InMemoryStudyPackRepository()
        val practiceSessionRepository = InMemoryPracticeSessionRepository()
        val wrongQuestionRepository = InMemoryWrongQuestionRepository()
        val statisticsRepository = InMemoryStatisticsRepository(
            questionRepository,
            studyPackRepository,
            answerRecordRepository,
            wrongQuestionRepository,
            practiceSessionRepository,
        )
        val useCase = GetStatisticsSummaryUseCase(statisticsRepository)

        val result = useCase.execute()

        assertEquals(0, result.data.totalAnsweredQuestions)
    }

    @Test
    fun `knowledge point mastery level is shared across summary and detail`() = runBlocking {
        val studyPackRepository = InMemoryStudyPackRepository().apply {
            replaceKnowledgePoints(
                listOf(
                    com.suilearn.core.model.KnowledgePoint(
                        knowledgePointId = "volatile",
                        packId = "java_interview_v1",
                        categoryId = "concurrency",
                        name = "volatile",
                        description = "可见性",
                        sortOrder = 1,
                    )
                )
            )
        }
        val questionRepository = InMemoryQuestionRepository().apply {
            replaceAll(
                samplePack().copy(
                    questions = listOf(
                        QuestionPackQuestion(
                            questionId = "concurrency_volatile_001",
                            categoryId = "concurrency",
                            type = QuestionType.SINGLE_CHOICE,
                            stem = "volatile 的作用是什么？",
                            options = listOf(
                                QuestionPackOption("A", "保证复合操作原子性"),
                                QuestionPackOption("B", "保证可见性和一定的有序性"),
                            ),
                            answer = listOf("B"),
                            explanation = "volatile 保证可见性。",
                            difficulty = 2,
                            knowledgePointIds = listOf("volatile"),
                            sortOrder = 1,
                        ),
                        QuestionPackQuestion(
                            questionId = "concurrency_volatile_002",
                            categoryId = "concurrency",
                            type = QuestionType.SINGLE_CHOICE,
                            stem = "volatile 能保证什么？",
                            options = listOf(
                                QuestionPackOption("A", "原子性"),
                                QuestionPackOption("B", "可见性"),
                            ),
                            answer = listOf("B"),
                            explanation = "volatile 保证可见性。",
                            difficulty = 2,
                            knowledgePointIds = listOf("volatile"),
                            sortOrder = 2,
                        ),
                    )
                )
            )
        }
        val answerRecordRepository = InMemoryAnswerRecordRepository().apply {
            add(
                com.suilearn.core.model.AnswerRecord(
                    recordId = "r1",
                    questionId = "concurrency_volatile_001",
                    practiceMode = PracticeMode.SEQUENTIAL,
                    targetId = null,
                    userAnswer = listOf("B"),
                    isCorrect = true,
                    durationMs = 1000,
                    answeredAt = 1,
                )
            )
            add(
                com.suilearn.core.model.AnswerRecord(
                    recordId = "r2",
                    questionId = "concurrency_volatile_002",
                    practiceMode = PracticeMode.SEQUENTIAL,
                    targetId = null,
                    userAnswer = listOf("A"),
                    isCorrect = false,
                    durationMs = 1000,
                    answeredAt = 2,
                )
            )
        }
        val wrongQuestionRepository = InMemoryWrongQuestionRepository().apply {
            upsertWrong("concurrency_volatile_002", 2)
        }
        val statisticsRepository = InMemoryStatisticsRepository(
            questionRepository,
            studyPackRepository,
            answerRecordRepository,
            wrongQuestionRepository,
            InMemoryPracticeSessionRepository(),
        )

        val summary = statisticsRepository.getKnowledgePointDetail("volatile")

        assertTrue(summary != null)
        assertEquals(MasteryLevel.WEAK, summary.masteryLevel)
    }

    @Test
    fun `reset learning data keeps imported pack and clears learning state only`() = runBlocking {
        val studyPackRepository = InMemoryStudyPackRepository()
        val questionRepository = InMemoryQuestionRepository()
        val settingsRepository = InMemorySettingsRepository()
        InitializeQuestionPackUseCase(studyPackRepository, questionRepository, settingsRepository).execute(samplePack())

        val answerRecordRepository = InMemoryAnswerRecordRepository().apply {
            add(
                com.suilearn.core.model.AnswerRecord(
                    recordId = "r1",
                    questionId = "concurrency_volatile_001",
                    practiceMode = PracticeMode.SEQUENTIAL,
                    targetId = null,
                    userAnswer = listOf("A"),
                    isCorrect = false,
                    durationMs = 1000,
                    answeredAt = 1,
                )
            )
        }
        val wrongQuestionRepository = InMemoryWrongQuestionRepository().apply { upsertWrong("concurrency_volatile_001", 1) }
        val favoriteRepository = InMemoryFavoriteRepository().apply { toggle("concurrency_volatile_001", 1) }
        val practiceSessionRepository = InMemoryPracticeSessionRepository().apply {
            save(
                com.suilearn.core.model.PracticeSession(
                    sessionId = "s1",
                    practiceMode = PracticeMode.SEQUENTIAL,
                    targetId = null,
                    questionIds = listOf("concurrency_volatile_001"),
                    currentIndex = 0,
                    status = com.suilearn.core.model.PracticeSessionStatus.IN_PROGRESS,
                    createdAt = 1,
                    updatedAt = 1,
                )
            )
        }

        val result = ResetLearningDataUseCase(
            settingsRepository,
            practiceSessionRepository,
            answerRecordRepository,
            wrongQuestionRepository,
            favoriteRepository,
        ).execute()

        assertTrue(result is AppResult.Success)
        assertEquals("java_interview_v1", studyPackRepository.getCurrentPack()?.packId)
        assertEquals(1, questionRepository.listQuestions().size)
        assertEquals(0, answerRecordRepository.listAll().size)
        assertEquals(0, wrongQuestionRepository.listActive().size)
        assertTrue(!favoriteRepository.isFavorite("concurrency_volatile_001"))
        assertTrue(practiceSessionRepository.getLatestInProgress() == null)
    }
}
