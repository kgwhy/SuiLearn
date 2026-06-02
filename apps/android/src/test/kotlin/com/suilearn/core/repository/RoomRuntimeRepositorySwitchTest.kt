package com.suilearn.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.suilearn.core.common.AppResult
import com.suilearn.core.database.SuiLearnDatabase
import com.suilearn.core.importer.QuestionPackSource
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.QuestionType
import com.suilearn.di.AppDependencies
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RoomRuntimeRepositorySwitchTest {
    private lateinit var database: SuiLearnDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SuiLearnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `runtime repositories use Room and keep learning state across dependency recreation`(): Unit = runBlocking {
        val firstRuntime = AppDependencies(assetSource(), database)
        firstRuntime.ensureSeeded()

        assertTrue(firstRuntime.studyPackRepository is RoomStudyPackRepository)
        assertTrue(firstRuntime.questionRepository is RoomQuestionRepository)
        assertTrue(firstRuntime.answerRecordRepository is RoomAnswerRecordRepository)
        assertTrue(firstRuntime.practiceSessionRepository is RoomPracticeSessionRepository)
        assertTrue(firstRuntime.settingsRepository is RoomSettingsRepository)
        assertTrue(firstRuntime.statisticsRepository is RoomStatisticsRepository)

        val question = firstRuntime.questionRepository.listQuestions().first { it.type != QuestionType.SHORT_ANSWER }
        val sessionResult = firstRuntime.buildPracticeSessionUseCase.execute(PracticeMode.SEQUENTIAL)
        assertTrue(sessionResult is AppResult.Success)

        val submission = firstRuntime.submitAnswerUseCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = question.questionId,
            userAnswer = listOf("__wrong__"),
            durationMs = 1500,
        )
        assertTrue(submission is AppResult.Success)
        assertTrue(!submission.data.isCorrect)

        val favorite = firstRuntime.toggleFavoriteQuestionUseCase.execute(question.questionId)
        assertTrue(favorite is AppResult.Success)
        assertTrue(favorite.data)

        val restartedRuntime = AppDependencies(assetSource(), database)
        restartedRuntime.ensureSeeded()

        assertEquals(1, restartedRuntime.answerRecordRepository.countByQuestion(question.questionId))
        assertNotNull(restartedRuntime.wrongQuestionRepository.get(question.questionId))
        assertTrue(restartedRuntime.favoriteRepository.isFavorite(question.questionId))
        assertNotNull(restartedRuntime.practiceSessionRepository.getLatestInProgress())

        val search = restartedRuntime.searchLearningContentUseCase.execute("volatile")
        assertTrue(search.data.isNotEmpty())

        val statistics = restartedRuntime.getStatisticsSummaryUseCase.execute()
        assertEquals(1, statistics.data.totalAnsweredQuestions)
        assertEquals(1, statistics.data.activeWrongQuestionCount)
        assertNotNull(statistics.data.latestRecoverableSessionId)
    }

    @Test
    fun `ensure seeded keeps learning records when bundled pack version and question count change`(): Unit = runBlocking {
        val source = MutableQuestionPackSource(minimalPackJson(packVersion = 1, includeSecondQuestion = false))
        val firstRuntime = AppDependencies(source, database)
        firstRuntime.ensureSeeded()

        val question = firstRuntime.questionRepository.getQuestion("q1")
        assertNotNull(question)
        val sessionResult = firstRuntime.buildPracticeSessionUseCase.execute(PracticeMode.SEQUENTIAL)
        assertTrue(sessionResult is AppResult.Success)

        val submission = firstRuntime.submitAnswerUseCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = question.questionId,
            userAnswer = listOf("A"),
            durationMs = 1500,
        )
        assertTrue(submission is AppResult.Success)
        assertTrue(!submission.data.isCorrect)

        val favorite = firstRuntime.toggleFavoriteQuestionUseCase.execute(question.questionId)
        assertTrue(favorite is AppResult.Success)
        assertTrue(favorite.data)

        source.json = minimalPackJson(packVersion = 2, includeSecondQuestion = true)
        val updatedRuntime = AppDependencies(source, database)
        updatedRuntime.ensureSeeded()

        assertEquals(2, updatedRuntime.studyPackRepository.getCurrentPack()?.packVersion)
        assertEquals(2, updatedRuntime.questionRepository.listQuestions().size)
        assertEquals(1, updatedRuntime.answerRecordRepository.countByQuestion(question.questionId))
        assertNotNull(updatedRuntime.wrongQuestionRepository.get(question.questionId))
        assertTrue(updatedRuntime.favoriteRepository.isFavorite(question.questionId))
        assertNotNull(updatedRuntime.practiceSessionRepository.getLatestInProgress())
    }

    @Test
    fun `ensure seeded deprecates removed questions and narrows current content after pack shrink`(): Unit = runBlocking {
        val source = MutableQuestionPackSource(minimalPackJson(packVersion = 1, includeSecondQuestion = true))
        val firstRuntime = AppDependencies(source, database)
        firstRuntime.ensureSeeded()

        val removedQuestion = firstRuntime.questionRepository.getQuestion("q2")
        assertNotNull(removedQuestion)
        val firstSession = firstRuntime.buildPracticeSessionUseCase.execute(PracticeMode.SEQUENTIAL)
        assertTrue(firstSession is AppResult.Success)
        assertEquals(listOf("q1", "q2"), firstSession.data.questionIds)

        val submission = firstRuntime.submitAnswerUseCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = removedQuestion.questionId,
            userAnswer = listOf("A"),
            durationMs = 1500,
        )
        assertTrue(submission is AppResult.Success)
        assertTrue(!submission.data.isCorrect)

        val favorite = firstRuntime.toggleFavoriteQuestionUseCase.execute(removedQuestion.questionId)
        assertTrue(favorite is AppResult.Success)
        assertTrue(favorite.data)

        source.json = minimalPackJson(packVersion = 2, includeSecondQuestion = false)
        val updatedRuntime = AppDependencies(source, database)
        updatedRuntime.ensureSeeded()

        assertEquals(2, updatedRuntime.studyPackRepository.getCurrentPack()?.packVersion)
        assertEquals(true, database.questionDao().findQuestion("q2")?.isDeprecated)
        assertEquals(listOf("q1"), updatedRuntime.questionRepository.listQuestions().map { it.questionId })
        assertEquals(1, updatedRuntime.answerRecordRepository.countByQuestion("q2"))
        assertNotNull(updatedRuntime.wrongQuestionRepository.get("q2"))
        assertTrue(updatedRuntime.favoriteRepository.isFavorite("q2"))

        val updatedSession = updatedRuntime.buildPracticeSessionUseCase.execute(PracticeMode.SEQUENTIAL)
        assertTrue(updatedSession is AppResult.Success)
        assertEquals(listOf("q1"), updatedSession.data.questionIds)

        assertEquals(listOf("java_basics"), updatedRuntime.studyPackRepository.listCategories().map { it.categoryId })
        assertEquals(listOf("kp"), updatedRuntime.studyPackRepository.listKnowledgePoints().map { it.knowledgePointId })
        val search = updatedRuntime.searchLearningContentUseCase.execute("legacy")
        assertTrue(search is AppResult.Success)
        assertFalse(search.data.any { it.id == "q2" || it.id == "legacy_kp" })
    }

    @Test
    fun `reset learning data keeps Room content available`(): Unit = runBlocking {
        val runtime = AppDependencies(assetSource(), database)
        runtime.ensureSeeded()
        val question = runtime.questionRepository.listQuestions().first { it.type != QuestionType.SHORT_ANSWER }

        runtime.submitAnswerUseCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = question.questionId,
            userAnswer = listOf("__wrong__"),
            durationMs = 1500,
        )
        runtime.toggleFavoriteQuestionUseCase.execute(question.questionId)
        runtime.buildPracticeSessionUseCase.execute(PracticeMode.SEQUENTIAL)

        val reset = runtime.resetLearningDataUseCase.execute()
        assertTrue(reset is AppResult.Success)

        assertEquals("java_interview_v1", runtime.settingsRepository.getCurrentPackId())
        assertTrue(runtime.questionRepository.listQuestions().isNotEmpty())
        assertEquals(0, runtime.answerRecordRepository.listAll().size)
        assertEquals(0, runtime.wrongQuestionRepository.listActive().size)
        assertEquals(0, runtime.favoriteRepository.listAll().size)
        assertEquals(null, runtime.practiceSessionRepository.getLatestInProgress())
    }

    private fun assetSource(): QuestionPackSource =
        object : QuestionPackSource {
            override fun loadQuestionPackJson(): String = assetJson()
        }

    private fun assetJson(): String {
        val candidates = listOf(
            File("src/main/assets/question_pack_java_interview.json"),
            File("apps/android/src/main/assets/question_pack_java_interview.json"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("Cannot find bundled question pack asset.")
        return file.readText(Charsets.UTF_8)
    }

    private class MutableQuestionPackSource(
        var json: String,
    ) : QuestionPackSource {
        override fun loadQuestionPackJson(): String = json
    }

    private fun minimalPackJson(packVersion: Int, includeSecondQuestion: Boolean): String {
        val secondCategory = if (includeSecondQuestion) {
            """
            ,
                {
                  "categoryId": "collections",
                  "name": "Legacy Category",
                  "description": "Removed category",
                  "sortOrder": 2
                }
            """.trimIndent()
        } else {
            ""
        }
        val secondKnowledgePoint = if (includeSecondQuestion) {
            """
            ,
                {
                  "knowledgePointId": "legacy_kp",
                  "categoryId": "collections",
                  "name": "Legacy Topic",
                  "description": "Removed knowledge point",
                  "sortOrder": 2
                }
            """.trimIndent()
        } else {
            ""
        }
        val secondQuestion = if (includeSecondQuestion) {
            """
            ,
            {
              "questionId": "q2",
              "categoryId": "collections",
              "type": "SINGLE_CHOICE",
              "stem": "Which keyword declares a legacy immutable reference?",
              "options": [
                { "key": "A", "content": "var" },
                { "key": "B", "content": "val" }
              ],
              "answer": ["B"],
              "explanation": "Legacy content should disappear from current practice.",
              "difficulty": 1,
              "knowledgePointIds": ["legacy_kp"],
              "sortOrder": 2
            }
            """.trimIndent()
        } else {
            ""
        }
        return """
            {
              "schemaVersion": 1,
              "packId": "test_pack",
              "packName": "Test Pack",
              "packVersion": $packVersion,
              "description": "Regression pack",
              "categories": [
                {
                  "categoryId": "java_basics",
                  "name": "Basics",
                  "description": "Basic topics",
                  "sortOrder": 1
                }
                $secondCategory
              ],
              "knowledgePoints": [
                {
                  "knowledgePointId": "kp",
                  "categoryId": "java_basics",
                  "name": "Kotlin val",
                  "description": "Read-only references",
                  "sortOrder": 1
                }
                $secondKnowledgePoint
              ],
              "questions": [
                {
                  "questionId": "q1",
                  "categoryId": "java_basics",
                  "type": "SINGLE_CHOICE",
                  "stem": "Which keyword declares a mutable variable?",
                  "options": [
                    { "key": "A", "content": "val" },
                    { "key": "B", "content": "var" }
                  ],
                  "answer": ["B"],
                  "explanation": "var declares a mutable local variable.",
                  "difficulty": 1,
                  "knowledgePointIds": ["kp"],
                  "sortOrder": 1
                }$secondQuestion
              ]
            }
        """.trimIndent()
    }
}
