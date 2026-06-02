package com.suilearn.core.repository

import com.suilearn.core.database.CategoryEntity
import com.suilearn.core.database.KnowledgePointEntity
import com.suilearn.core.database.QuestionDao
import com.suilearn.core.database.QuestionEntity
import com.suilearn.core.database.QuestionKnowledgePointEntity
import com.suilearn.core.database.QuestionOptionEntity
import com.suilearn.core.database.StudyPackDao
import com.suilearn.core.database.StudyPackEntity
import com.suilearn.core.database.decodeStringListFromJson
import com.suilearn.core.database.encodeStringListAsJson
import com.suilearn.core.model.Category
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.Question
import com.suilearn.core.model.QuestionOption
import com.suilearn.core.model.QuestionPack
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.StudyPack

class RoomStudyPackRepository(
    private val studyPackDao: StudyPackDao,
) : StudyPackRepository {
    override suspend fun getCurrentPack(): StudyPack? =
        studyPackDao.getLatestStudyPack()?.toDomain()

    override suspend fun upsertPack(pack: StudyPack) {
        studyPackDao.upsertStudyPack(pack.toEntity())
    }

    override suspend fun listKnowledgePoints(): List<KnowledgePoint> =
        studyPackDao.listKnowledgePoints().map { it.toDomain() }

    override suspend fun listCategories(): List<Category> =
        studyPackDao.listCategories().map { it.toDomain() }

    override suspend fun replaceCategories(items: List<Category>) {
        studyPackDao.upsertCategories(items.map { it.toEntity() })
    }

    override suspend fun replaceKnowledgePoints(items: List<KnowledgePoint>) {
        studyPackDao.upsertKnowledgePoints(items.map { it.toEntity() })
    }
}

class RoomQuestionRepository(
    private val questionDao: QuestionDao,
) : QuestionRepository {
    override suspend fun listQuestions(): List<Question> =
        questionDao.listActiveQuestions().map { it.toDomain() }

    override suspend fun getQuestion(questionId: String): Question? =
        questionDao.findQuestion(questionId)?.toDomain()

    override suspend fun replaceAll(pack: QuestionPack) {
        val questionIds = pack.questions.map { it.questionId }
        questionDao.deleteKnowledgePointRefs(questionIds)
        questionDao.deleteOptions(questionIds)
        questionDao.markMissingQuestionsDeprecated(pack.packId, questionIds)

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

    private suspend fun QuestionEntity.toDomain(): Question =
        Question(
            questionId = questionId,
            packId = packId,
            categoryId = categoryId,
            type = QuestionType.valueOf(type),
            stem = stem,
            answer = decodeStringListFromJson(answer),
            explanation = explanation,
            difficulty = difficulty,
            isDeprecated = isDeprecated,
            sortOrder = sortOrder,
            options = questionDao.listOptions(questionId).map { it.toDomain() },
            knowledgePointIds = questionDao.listKnowledgePointRefs(questionId).map { it.knowledgePointId },
        )
}

private fun StudyPackEntity.toDomain(): StudyPack =
    StudyPack(
        packId = packId,
        name = name,
        description = description,
        packVersion = packVersion,
        schemaVersion = schemaVersion,
        importedAt = importedAt,
    )

private fun StudyPack.toEntity(): StudyPackEntity =
    StudyPackEntity(
        packId = packId,
        name = name,
        description = description,
        packVersion = packVersion,
        schemaVersion = schemaVersion,
        importedAt = importedAt,
    )

private fun CategoryEntity.toDomain(): Category =
    Category(
        categoryId = categoryId,
        packId = packId,
        name = name,
        description = description,
        sortOrder = sortOrder,
    )

private fun Category.toEntity(): CategoryEntity =
    CategoryEntity(
        categoryId = categoryId,
        packId = packId,
        name = name,
        description = description,
        sortOrder = sortOrder,
    )

private fun KnowledgePointEntity.toDomain(): KnowledgePoint =
    KnowledgePoint(
        knowledgePointId = knowledgePointId,
        packId = packId,
        categoryId = categoryId,
        name = name,
        description = description,
        sortOrder = sortOrder,
    )

private fun KnowledgePoint.toEntity(): KnowledgePointEntity =
    KnowledgePointEntity(
        knowledgePointId = knowledgePointId,
        packId = packId,
        categoryId = categoryId,
        name = name,
        description = description,
        sortOrder = sortOrder,
    )

private fun QuestionOptionEntity.toDomain(): QuestionOption =
    QuestionOption(
        optionId = optionId,
        questionId = questionId,
        optionKey = optionKey,
        content = content,
        sortOrder = sortOrder,
    )
