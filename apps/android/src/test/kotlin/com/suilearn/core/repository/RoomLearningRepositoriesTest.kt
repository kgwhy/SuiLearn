package com.suilearn.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.suilearn.core.common.AppResult
import com.suilearn.core.database.SuiLearnDatabase
import com.suilearn.core.importer.QuestionPackJsonParser
import com.suilearn.core.importer.QuestionPackRoomImporter
import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.PracticeSessionStatus
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.WrongQuestionStatus
import com.suilearn.core.usecase.BuildPracticeSessionUseCase
import com.suilearn.core.usecase.ResetLearningDataUseCase
import com.suilearn.core.usecase.ResumePracticeSessionUseCase
import com.suilearn.core.usecase.SubmitAnswerUseCase
import com.suilearn.core.usecase.ToggleFavoriteQuestionUseCase
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
class RoomLearningRepositoriesTest {
    private lateinit var database: SuiLearnDatabase
    private lateinit var answerRecordRepository: RoomAnswerRecordRepository
    private lateinit var wrongQuestionRepository: RoomWrongQuestionRepository
    private lateinit var favoriteRepository: RoomFavoriteRepository
    private lateinit var practiceSessionRepository: RoomPracticeSessionRepository
    private lateinit var settingsRepository: RoomSettingsRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SuiLearnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        QuestionPackRoomImporter(database).import(QuestionPackJsonParser.parse(assetJson()))
        answerRecordRepository = RoomAnswerRecordRepository(database.learningDao())
        wrongQuestionRepository = RoomWrongQuestionRepository(database.learningDao())
        favoriteRepository = RoomFavoriteRepository(database.learningDao())
        practiceSessionRepository = RoomPracticeSessionRepository(database.learningDao(), FixedClock(5000))
        settingsRepository = RoomSettingsRepository(database.appSettingDao(), FixedClock(6000))
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `objective answer submission writes answer record and wrong state into Room`() = runBlocking {
        val pack = QuestionPackJsonParser.parse(assetJson())
        val question = pack.questions.first { it.type != QuestionType.SHORT_ANSWER }
        val questionRepository = InMemoryQuestionRepository().apply { replaceAll(pack) }
        val useCase = SubmitAnswerUseCase(
            questionRepository = questionRepository,
            answerRecordRepository = answerRecordRepository,
            wrongQuestionRepository = wrongQuestionRepository,
            favoriteRepository = favoriteRepository,
            idGenerator = FixedIdGenerator("record_1"),
            clock = FixedClock(1000),
        )

        val result = useCase.execute(
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            questionId = question.questionId,
            userAnswer = listOf("__wrong__"),
            durationMs = 2500,
        )

        assertTrue(result is AppResult.Success)
        assertFalse(result.data.isCorrect)
        assertEquals(1, database.learningDao().listAnswerRecords().size)
        assertEquals(1, answerRecordRepository.countByQuestion(question.questionId))
        assertEquals(0, answerRecordRepository.countCorrectByQuestion(question.questionId))
        assertEquals(1000, answerRecordRepository.latestAnsweredAt())

        val wrong = wrongQuestionRepository.get(question.questionId)
        assertNotNull(wrong)
        assertEquals(WrongQuestionStatus.ACTIVE, wrong.status)
        assertEquals(1, wrong.wrongCount)
    }

    @Test
    fun `answer records round trip user answers through Room`() = runBlocking {
        val record = AnswerRecord(
            recordId = "record_1",
            questionId = database.questionDao().listQuestions().first().questionId,
            practiceMode = PracticeMode.CATEGORY,
            targetId = "category_1",
            userAnswer = listOf("A", "B"),
            isCorrect = true,
            durationMs = 1200,
            answeredAt = 2000,
        )

        answerRecordRepository.add(record)

        assertEquals(listOf(record), answerRecordRepository.listAll())
        assertEquals(listOf(record), answerRecordRepository.listRecent(1))
        assertEquals(1, answerRecordRepository.countCorrectByQuestion(record.questionId))
    }

    @Test
    fun `wrong question state can become mastered and return active on next mistake`() = runBlocking {
        val questionId = database.questionDao().listQuestions().first().questionId

        wrongQuestionRepository.upsertWrong(questionId, 1000)
        wrongQuestionRepository.markMastered(questionId, 2000)
        val mastered = wrongQuestionRepository.get(questionId)

        assertNotNull(mastered)
        assertEquals(WrongQuestionStatus.MASTERED, mastered.status)
        assertEquals(2000, mastered.masteredAt)
        assertEquals(listOf(WrongQuestionStatus.MASTERED), wrongQuestionRepository.listAll().map { it.status })
        assertEquals(0, wrongQuestionRepository.listActive().size)

        wrongQuestionRepository.upsertWrong(questionId, 3000)
        val activeAgain = wrongQuestionRepository.get(questionId)

        assertNotNull(activeAgain)
        assertEquals(WrongQuestionStatus.ACTIVE, activeAgain.status)
        assertEquals(2, activeAgain.wrongCount)
        assertEquals(null, activeAgain.masteredAt)
        assertEquals(listOf(WrongQuestionStatus.ACTIVE), wrongQuestionRepository.listAll().map { it.status })
        assertEquals(1, wrongQuestionRepository.listActive().size)
    }

    @Test
    fun `favorite toggle persists favorite state in Room`() = runBlocking {
        val questionId = database.questionDao().listQuestions().first().questionId
        val useCase = ToggleFavoriteQuestionUseCase(favoriteRepository, FixedClock(4000))

        val added = useCase.execute(questionId)

        assertTrue(added is AppResult.Success)
        assertTrue(added.data)
        assertTrue(favoriteRepository.isFavorite(questionId))
        assertEquals(1, favoriteRepository.listAll().size)
        assertEquals(4000, favoriteRepository.listAll().first().createdAt)

        val removed = useCase.execute(questionId)

        assertTrue(removed is AppResult.Success)
        assertFalse(removed.data)
        assertFalse(favoriteRepository.isFavorite(questionId))
        assertEquals(0, favoriteRepository.listAll().size)
    }

    @Test
    fun `practice session persists and can be restored by a new repository instance`() = runBlocking {
        val session = PracticeSession(
            sessionId = "session_1",
            practiceMode = PracticeMode.CATEGORY,
            targetId = "category_1",
            questionIds = listOf("question_1", "question_2"),
            currentIndex = 1,
            status = PracticeSessionStatus.IN_PROGRESS,
            createdAt = 1000,
            updatedAt = 2000,
        )

        practiceSessionRepository.save(session)
        val restoredRepository = RoomPracticeSessionRepository(database.learningDao(), FixedClock(7000))

        assertEquals(session, restoredRepository.find("session_1"))
        assertEquals(session, restoredRepository.getLatestInProgress())

        restoredRepository.markCompleted("session_1")

        assertEquals(PracticeSessionStatus.COMPLETED, restoredRepository.find("session_1")?.status)
        assertEquals(null, restoredRepository.getLatestInProgress())
    }

    @Test
    fun `resume use case restores latest persisted practice session`() = runBlocking {
        val pack = QuestionPackJsonParser.parse(assetJson())
        val questionRepository = InMemoryQuestionRepository().apply { replaceAll(pack) }
        val firstQuestionId = questionRepository.listQuestions().first().questionId
        val buildUseCase = BuildPracticeSessionUseCase(
            questionRepository = questionRepository,
            practiceSessionRepository = practiceSessionRepository,
            wrongQuestionRepository = wrongQuestionRepository,
            favoriteRepository = favoriteRepository,
            clock = FixedClock(8000),
            idGenerator = FixedIdGenerator("session_1"),
        )

        val built = buildUseCase.execute(PracticeMode.SEQUENTIAL)
        assertTrue(built is AppResult.Success)

        val restoredSessionRepository = RoomPracticeSessionRepository(database.learningDao())
        val resumeUseCase = ResumePracticeSessionUseCase(questionRepository, restoredSessionRepository)
        val resumed = resumeUseCase.execute()

        assertTrue(resumed is AppResult.Success)
        assertEquals("session_1", resumed.data?.sessionId)
        assertEquals(firstQuestionId, resumed.data?.questionIds?.first())
    }

    @Test
    fun `settings keep current pack and reset clears learning state without deleting content`() = runBlocking {
        val questionId = database.questionDao().listQuestions().first().questionId
        settingsRepository.setCurrentPackId("java_interview_v1")
        answerRecordRepository.add(
            AnswerRecord(
                recordId = "record_1",
                questionId = questionId,
                practiceMode = PracticeMode.SEQUENTIAL,
                targetId = null,
                userAnswer = listOf("A"),
                isCorrect = false,
                durationMs = 1000,
                answeredAt = 1000,
            )
        )
        wrongQuestionRepository.upsertWrong(questionId, 1000)
        favoriteRepository.toggle(questionId, 1000)
        practiceSessionRepository.save(
            PracticeSession(
                sessionId = "session_1",
                practiceMode = PracticeMode.SEQUENTIAL,
                targetId = null,
                questionIds = listOf(questionId),
                currentIndex = 0,
                status = PracticeSessionStatus.IN_PROGRESS,
                createdAt = 1000,
                updatedAt = 1000,
            )
        )

        val result = ResetLearningDataUseCase(
            settingsRepository = settingsRepository,
            practiceSessionRepository = practiceSessionRepository,
            answerRecordRepository = answerRecordRepository,
            wrongQuestionRepository = wrongQuestionRepository,
            favoriteRepository = favoriteRepository,
        ).execute()

        assertTrue(result is AppResult.Success)
        assertEquals("java_interview_v1", settingsRepository.getCurrentPackId())
        assertEquals(1, database.studyPackDao().listStudyPacks().size)
        assertTrue(database.questionDao().listQuestions().isNotEmpty())
        assertEquals(0, answerRecordRepository.listAll().size)
        assertEquals(0, wrongQuestionRepository.listActive().size)
        assertEquals(0, favoriteRepository.listAll().size)
        assertEquals(null, practiceSessionRepository.getLatestInProgress())
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

private class FixedClock(private val now: Long) : com.suilearn.core.common.Clock {
    override fun now(): Long = now
}

private class FixedIdGenerator(private val id: String) : com.suilearn.core.common.IdGenerator {
    override fun newId(): String = id
}
