package com.suilearn.core.importer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.suilearn.core.common.Clock
import com.suilearn.core.database.SuiLearnDatabase
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
class QuestionPackRoomImporterTest {
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
    fun `imports bundled question pack content into Room`() = runBlocking {
        val pack = QuestionPackJsonParser.parse(assetJson())
        val importer = QuestionPackRoomImporter(database, FixedClock)

        importer.import(pack)

        assertEquals(1, database.studyPackDao().listStudyPacks().size)
        assertEquals(2, database.studyPackDao().listCategories().size)
        assertEquals(2, database.studyPackDao().listKnowledgePoints().size)
        assertEquals(4, database.questionDao().listQuestions().size)
        assertEquals(pack.questions.sumOf { it.options.size }, database.questionDao().listOptions().size)
        assertEquals(
            pack.questions.sumOf { it.knowledgePointIds.size },
            database.questionDao().listKnowledgePointRefs().size,
        )

        val importedPack = database.studyPackDao().getLatestStudyPack()
        assertNotNull(importedPack)
        assertEquals(pack.packId, importedPack.packId)
        assertEquals(FixedClock.now(), importedPack.importedAt)
    }

    @Test
    fun `imported question references point to existing categories and knowledge points`() = runBlocking {
        val pack = QuestionPackJsonParser.parse(assetJson())

        QuestionPackRoomImporter(database, FixedClock).import(pack)

        val categoryIds = database.studyPackDao().listCategories().map { it.categoryId }.toSet()
        val knowledgePointIds = database.studyPackDao().listKnowledgePoints().map { it.knowledgePointId }.toSet()
        val questionIds = database.questionDao().listQuestions().map { it.questionId }.toSet()

        assertTrue(database.questionDao().listQuestions().all { it.categoryId in categoryIds })
        assertTrue(database.questionDao().listOptions().all { it.questionId in questionIds })
        assertTrue(
            database.questionDao().listKnowledgePointRefs().all {
                it.questionId in questionIds && it.knowledgePointId in knowledgePointIds
            }
        )
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

    private object FixedClock : Clock {
        override fun now(): Long = 1_700_000_000_000
    }
}
