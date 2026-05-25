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
        val categoryIds = pack.categories.map { it.categoryId }
        val knowledgePointIds = pack.knowledgePoints.map { it.knowledgePointId }.toSet()

        assertEquals(1, pack.schemaVersion)
        assertEquals("java_interview_v1", pack.packId)
        assertEquals(2, pack.packVersion)
        assertEquals("Java \u516b\u80a1\u5b66\u4e60\u5305", pack.packName)
        assertEquals(ExpectedCategoryIds, categoryIds)
        assertEquals(11, pack.categories.size)
        assertTrue(pack.knowledgePoints.size >= 30)
        assertEquals(50, pack.questions.size)
        assertEquals(QuestionType.entries.toSet(), pack.questions.map { it.type }.toSet())
        assertEquals(QuestionType.SINGLE_CHOICE, pack.questions.first().type)
        assertEquals(listOf("B"), pack.questions.first().answer)
        assertEquals((1..50).toList(), pack.questions.map { it.sortOrder }.sorted())
        assertTrue(pack.knowledgePoints.all { it.categoryId in categoryIds })
        assertTrue(
            pack.questions.all {
                it.categoryId in categoryIds &&
                    it.knowledgePointIds.isNotEmpty() &&
                    it.knowledgePointIds.all { id -> id in knowledgePointIds } &&
                    it.answer.isNotEmpty() &&
                    it.explanation.isNotBlank()
            }
        )
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
        assertEquals(11, dependencies.studyPackRepository.listCategories().size)
        assertTrue(dependencies.studyPackRepository.listKnowledgePoints().size >= 30)
        assertEquals(50, dependencies.questionRepository.listQuestions().size)
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

    private companion object {
        val ExpectedCategoryIds = listOf(
            "jvm",
            "java_basics",
            "collections",
            "concurrency",
            "spring",
            "mysql",
            "redis",
            "computer_networks",
            "operating_system",
            "design_patterns",
            "project_scenarios",
        )
    }
}
