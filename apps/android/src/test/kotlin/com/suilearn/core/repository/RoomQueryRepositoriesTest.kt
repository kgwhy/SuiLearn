package com.suilearn.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.suilearn.core.database.SuiLearnDatabase
import com.suilearn.core.importer.QuestionPackJsonParser
import com.suilearn.core.importer.QuestionPackRoomImporter
import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.PracticeSessionStatus
import com.suilearn.core.model.SearchResultType
import com.suilearn.core.usecase.GetStatisticsSummaryUseCase
import com.suilearn.core.usecase.SearchLearningContentUseCase
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class RoomQueryRepositoriesTest {
    private lateinit var database: SuiLearnDatabase
    private lateinit var studyPackRepository: RoomStudyPackRepository
    private lateinit var questionRepository: RoomQuestionRepository
    private lateinit var answerRecordRepository: RoomAnswerRecordRepository
    private lateinit var wrongQuestionRepository: RoomWrongQuestionRepository
    private lateinit var practiceSessionRepository: RoomPracticeSessionRepository

    @Before
    fun setUp() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SuiLearnDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        QuestionPackRoomImporter(database).import(QuestionPackJsonParser.parse(assetJson()))
        studyPackRepository = RoomStudyPackRepository(database.studyPackDao())
        questionRepository = RoomQuestionRepository(database.questionDao())
        answerRecordRepository = RoomAnswerRecordRepository(database.learningDao())
        wrongQuestionRepository = RoomWrongQuestionRepository(database.learningDao())
        practiceSessionRepository = RoomPracticeSessionRepository(database.learningDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `Room search matches content categories and escaped like keywords`() = runBlocking {
        val useCase = SearchLearningContentUseCase(
            RoomSearchRepository(
                database.questionDao(),
                database.studyPackDao(),
                answerRecordRepository,
                wrongQuestionRepository,
            )
        )

        val keywordResults = useCase.execute("volatile").data
        assertTrue(keywordResults.isNotEmpty())
        assertTrue(keywordResults.any { it.matchedFields.any { field -> field.startsWith("knowledgePoint") } })

        val categoryResults = useCase.execute("JVM").data
        assertTrue(categoryResults.any { it.type == SearchResultType.QUESTION && it.categoryName == "JVM" })
        assertTrue(categoryResults.any { it.type == SearchResultType.KNOWLEDGE_POINT && it.categoryName == "JVM" })

        val underscoreResults = useCase.execute("_").data
        assertTrue(underscoreResults.any { it.id == "tcp_handshake" || it.id == "network_tcp_handshake_001" })

        listOf("%", "\\").forEach { keyword ->
            assertTrue(useCase.execute(keyword).data.isEmpty(), "Expected no literal match for escaped keyword $keyword")
        }
    }

    @Test
    fun `Room statistics summarize category knowledge point weak and recent progress`() = runBlocking {
        answerRecordRepository.add(record("r1", "jvm_memory_area_001", isCorrect = false, answeredAt = 1000))
        answerRecordRepository.add(record("r2", "jvm_gc_roots_001", isCorrect = false, answeredAt = 2000))
        answerRecordRepository.add(record("r3", "jvm_memory_area_001", isCorrect = true, answeredAt = 3000))
        answerRecordRepository.add(record("r4", "java_oop_001", isCorrect = true, answeredAt = 4000))
        wrongQuestionRepository.upsertWrong("jvm_memory_area_001", 1000)
        wrongQuestionRepository.upsertWrong("jvm_gc_roots_001", 2000)
        practiceSessionRepository.save(
            PracticeSession(
                sessionId = "session_1",
                practiceMode = PracticeMode.SEQUENTIAL,
                targetId = null,
                questionIds = listOf("jvm_memory_area_001", "jvm_gc_roots_001"),
                currentIndex = 1,
                status = PracticeSessionStatus.IN_PROGRESS,
                createdAt = 1000,
                updatedAt = 5000,
            )
        )

        val summary = GetStatisticsSummaryUseCase(
            RoomStatisticsRepository(
                questionRepository,
                studyPackRepository,
                answerRecordRepository,
                wrongQuestionRepository,
                practiceSessionRepository,
            )
        ).execute().data

        assertEquals(3, summary.totalAnsweredQuestions)
        assertEquals(0.5, summary.totalAccuracy, 0.0001)
        assertEquals(2, summary.activeWrongQuestionCount)
        assertEquals("session_1", summary.latestRecoverableSessionId)

        val jvmProgress = summary.categoryProgress.first { it.category.categoryId == "jvm" }
        assertEquals(2, jvmProgress.practicedCount)
        assertEquals(1.0 / 3.0, jvmProgress.accuracy, 0.0001)
        assertEquals(2, jvmProgress.activeWrongCount)

        val memoryModelProgress = summary.knowledgePointProgress.first {
            it.knowledgePoint.knowledgePointId == "jvm_memory_model"
        }
        assertEquals(1, memoryModelProgress.practicedCount)
        assertEquals(0.5, memoryModelProgress.accuracy, 0.0001)
        assertEquals(1, memoryModelProgress.activeWrongCount)

        assertTrue(summary.topWeakKnowledgePointProgress.isNotEmpty())
        assertTrue(summary.topWeakKnowledgePointProgress.all { it.activeWrongCount > 0 })
        assertEquals("java_oop_001", summary.recentLearningRecords.first().questionId)
        assertEquals(4, summary.recentLearningRecords.size)
    }

    private fun record(
        recordId: String,
        questionId: String,
        isCorrect: Boolean,
        answeredAt: Long,
    ): AnswerRecord =
        AnswerRecord(
            recordId = recordId,
            questionId = questionId,
            practiceMode = PracticeMode.SEQUENTIAL,
            targetId = null,
            userAnswer = if (isCorrect) listOf("A") else listOf("__wrong__"),
            isCorrect = isCorrect,
            durationMs = 1000,
            answeredAt = answeredAt,
        )

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
