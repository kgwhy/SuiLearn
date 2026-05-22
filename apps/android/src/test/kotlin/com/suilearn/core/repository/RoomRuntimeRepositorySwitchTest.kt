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
}
