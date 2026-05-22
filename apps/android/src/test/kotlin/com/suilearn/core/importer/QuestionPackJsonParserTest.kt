package com.suilearn.core.importer

import com.suilearn.core.model.QuestionPackValidation
import com.suilearn.core.model.QuestionType
import com.suilearn.di.AppDependencies
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestionPackJsonParserTest {
    @Test
    fun `parses bundled question pack JSON`() {
        val pack = QuestionPackJsonParser.parse(assetJson())

        assertEquals(1, pack.schemaVersion)
        assertEquals("java_interview_v1", pack.packId)
        assertEquals("Java 基础", pack.packName)
        assertEquals(2, pack.categories.size)
        assertEquals(2, pack.knowledgePoints.size)
        assertEquals(4, pack.questions.size)
        assertEquals(QuestionType.SINGLE_CHOICE, pack.questions.first().type)
        assertEquals(listOf("B"), pack.questions.first().answer)
        assertTrue(QuestionPackValidation.validate(pack).isEmpty())
    }

    @Test
    fun `app dependencies import bundled JSON into in memory repositories`() = runBlocking {
        val dependencies = AppDependencies(
            object : QuestionPackSource {
                override fun loadQuestionPackJson(): String = assetJson()
            }
        )

        dependencies.ensureSeeded()

        assertEquals("java_interview_v1", dependencies.studyPackRepository.getCurrentPack()?.packId)
        assertEquals(2, dependencies.studyPackRepository.listCategories().size)
        assertEquals(2, dependencies.studyPackRepository.listKnowledgePoints().size)
        assertEquals(4, dependencies.questionRepository.listQuestions().size)
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
