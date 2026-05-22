package com.suilearn.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "study_packs")
data class StudyPackEntity(
    @PrimaryKey
    @ColumnInfo(name = "pack_id")
    val packId: String,
    val name: String,
    val description: String,
    @ColumnInfo(name = "pack_version")
    val packVersion: Int,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int,
    @ColumnInfo(name = "imported_at")
    val importedAt: Long,
)

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = StudyPackEntity::class,
            parentColumns = ["pack_id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pack_id")],
)
data class CategoryEntity(
    @PrimaryKey
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    @ColumnInfo(name = "pack_id")
    val packId: String,
    val name: String,
    val description: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

@Entity(
    tableName = "knowledge_points",
    foreignKeys = [
        ForeignKey(
            entity = StudyPackEntity::class,
            parentColumns = ["pack_id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("pack_id"),
        Index("category_id"),
    ],
)
data class KnowledgePointEntity(
    @PrimaryKey
    @ColumnInfo(name = "knowledge_point_id")
    val knowledgePointId: String,
    @ColumnInfo(name = "pack_id")
    val packId: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    val name: String,
    val description: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = StudyPackEntity::class,
            parentColumns = ["pack_id"],
            childColumns = ["pack_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["category_id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("pack_id"),
        Index("category_id"),
    ],
)
data class QuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "pack_id")
    val packId: String,
    @ColumnInfo(name = "category_id")
    val categoryId: String,
    val type: String,
    val stem: String,
    val answer: String,
    val explanation: String,
    val difficulty: Int,
    @ColumnInfo(name = "is_deprecated")
    val isDeprecated: Boolean,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

@Entity(
    tableName = "question_options",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("question_id")],
)
data class QuestionOptionEntity(
    @PrimaryKey
    @ColumnInfo(name = "option_id")
    val optionId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "option_key")
    val optionKey: String,
    val content: String,
    @ColumnInfo(name = "sort_order")
    val sortOrder: Int,
)

@Entity(
    tableName = "question_knowledge_points",
    primaryKeys = ["question_id", "knowledge_point_id"],
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = KnowledgePointEntity::class,
            parentColumns = ["knowledge_point_id"],
            childColumns = ["knowledge_point_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index("question_id"),
        Index("knowledge_point_id"),
    ],
)
data class QuestionKnowledgePointEntity(
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "knowledge_point_id")
    val knowledgePointId: String,
)

@Entity(
    tableName = "answer_records",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("question_id")],
)
data class AnswerRecordEntity(
    @PrimaryKey
    @ColumnInfo(name = "record_id")
    val recordId: String,
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "practice_mode")
    val practiceMode: String,
    @ColumnInfo(name = "target_id")
    val targetId: String?,
    @ColumnInfo(name = "user_answer")
    val userAnswer: String,
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    @ColumnInfo(name = "duration_ms")
    val durationMs: Long,
    @ColumnInfo(name = "answered_at")
    val answeredAt: Long,
)

@Entity(tableName = "practice_sessions")
data class PracticeSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "session_id")
    val sessionId: String,
    @ColumnInfo(name = "practice_mode")
    val practiceMode: String,
    @ColumnInfo(name = "target_id")
    val targetId: String?,
    @ColumnInfo(name = "question_ids")
    val questionIds: String,
    @ColumnInfo(name = "current_index")
    val currentIndex: Int,
    val status: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)

@Entity(
    tableName = "wrong_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class WrongQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    val status: String,
    @ColumnInfo(name = "wrong_count")
    val wrongCount: Int,
    @ColumnInfo(name = "first_wrong_at")
    val firstWrongAt: Long,
    @ColumnInfo(name = "last_wrong_at")
    val lastWrongAt: Long,
    @ColumnInfo(name = "mastered_at")
    val masteredAt: Long?,
)

@Entity(
    tableName = "favorite_questions",
    foreignKeys = [
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["question_id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
)
data class FavoriteQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Long,
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey
    val key: String,
    val value: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long,
)
