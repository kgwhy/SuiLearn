package com.suilearn.core.importer

import androidx.room.withTransaction
import com.suilearn.core.common.Clock
import com.suilearn.core.common.SystemClock
import com.suilearn.core.database.CategoryEntity
import com.suilearn.core.database.KnowledgePointEntity
import com.suilearn.core.database.QuestionEntity
import com.suilearn.core.database.QuestionKnowledgePointEntity
import com.suilearn.core.database.QuestionOptionEntity
import com.suilearn.core.database.StudyPackEntity
import com.suilearn.core.database.SuiLearnDatabase
import com.suilearn.core.database.encodeStringListAsJson
import com.suilearn.core.model.QuestionPack

class QuestionPackRoomImporter(
    private val database: SuiLearnDatabase,
    private val clock: Clock = SystemClock,
) {
    suspend fun import(pack: QuestionPack) {
        val importedAt = clock.now()
        database.withTransaction {
            val studyPackDao = database.studyPackDao()
            val questionDao = database.questionDao()

            questionDao.deleteKnowledgePointRefs()
            questionDao.deleteOptions()

            studyPackDao.upsertStudyPack(
                StudyPackEntity(
                    packId = pack.packId,
                    name = pack.packName,
                    description = pack.description,
                    packVersion = pack.packVersion,
                    schemaVersion = pack.schemaVersion,
                    importedAt = importedAt,
                )
            )
            studyPackDao.upsertCategories(
                pack.categories.map {
                    CategoryEntity(
                        categoryId = it.categoryId,
                        packId = pack.packId,
                        name = it.name,
                        description = it.description,
                        sortOrder = it.sortOrder,
                    )
                }
            )
            studyPackDao.upsertKnowledgePoints(
                pack.knowledgePoints.map {
                    KnowledgePointEntity(
                        knowledgePointId = it.knowledgePointId,
                        packId = pack.packId,
                        categoryId = it.categoryId,
                        name = it.name,
                        description = it.description,
                        sortOrder = it.sortOrder,
                    )
                }
            )
            questionDao.upsertQuestions(
                pack.questions.map {
                    QuestionEntity(
                        questionId = it.questionId,
                        packId = pack.packId,
                        categoryId = it.categoryId,
                        type = it.type.name,
                        stem = it.stem,
                        answer = encodeStringListAsJson(it.answer),
                        explanation = it.explanation,
                        difficulty = it.difficulty,
                        isDeprecated = it.deprecated,
                        sortOrder = it.sortOrder,
                    )
                }
            )
            questionDao.upsertOptions(
                pack.questions.flatMap { question ->
                    question.options.mapIndexed { index, option ->
                        QuestionOptionEntity(
                            optionId = "${question.questionId}_option_${index + 1}",
                            questionId = question.questionId,
                            optionKey = option.key,
                            content = option.content,
                            sortOrder = index + 1,
                        )
                    }
                }
            )
            questionDao.upsertKnowledgePointRefs(
                pack.questions.flatMap { question ->
                    question.knowledgePointIds.map {
                        QuestionKnowledgePointEntity(
                            questionId = question.questionId,
                            knowledgePointId = it,
                        )
                    }
                }
            )
        }
    }
}
