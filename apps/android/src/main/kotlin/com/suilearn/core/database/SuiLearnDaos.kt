package com.suilearn.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface StudyPackDao {
    @Query("SELECT * FROM study_packs ORDER BY imported_at DESC")
    suspend fun listStudyPacks(): List<StudyPackEntity>

    @Query("SELECT * FROM study_packs ORDER BY imported_at DESC LIMIT 1")
    suspend fun getLatestStudyPack(): StudyPackEntity?

    @Query("SELECT * FROM categories ORDER BY sort_order")
    suspend fun listCategories(): List<CategoryEntity>

    @Query("SELECT * FROM knowledge_points ORDER BY sort_order")
    suspend fun listKnowledgePoints(): List<KnowledgePointEntity>

    @Query(
        """
        SELECT DISTINCT kp.* FROM knowledge_points kp
        JOIN categories c ON c.category_id = kp.category_id
        WHERE kp.name LIKE :pattern ESCAPE '\'
            OR kp.description LIKE :pattern ESCAPE '\'
            OR c.name LIKE :pattern ESCAPE '\'
        ORDER BY kp.sort_order
        """
    )
    suspend fun searchKnowledgePoints(pattern: String): List<KnowledgePointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStudyPack(entity: StudyPackEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(entities: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKnowledgePoints(entities: List<KnowledgePointEntity>)

    @Query("DELETE FROM categories")
    suspend fun deleteCategories()

    @Query("DELETE FROM knowledge_points")
    suspend fun deleteKnowledgePoints()
}

@Dao
interface QuestionDao {
    @Query("SELECT * FROM questions ORDER BY sort_order")
    suspend fun listQuestions(): List<QuestionEntity>

    @Transaction
    @Query("SELECT * FROM questions WHERE is_deprecated = 0 ORDER BY sort_order")
    suspend fun listActiveQuestions(): List<QuestionEntity>

    @Query("SELECT * FROM question_options ORDER BY question_id, sort_order")
    suspend fun listOptions(): List<QuestionOptionEntity>

    @Query("SELECT * FROM question_knowledge_points ORDER BY question_id, knowledge_point_id")
    suspend fun listKnowledgePointRefs(): List<QuestionKnowledgePointEntity>

    @Query("SELECT * FROM questions WHERE question_id = :questionId")
    suspend fun findQuestion(questionId: String): QuestionEntity?

    @Query(
        """
        SELECT DISTINCT q.* FROM questions q
        LEFT JOIN categories c ON c.category_id = q.category_id
        LEFT JOIN question_options o ON o.question_id = q.question_id
        LEFT JOIN question_knowledge_points qkp ON qkp.question_id = q.question_id
        LEFT JOIN knowledge_points kp ON kp.knowledge_point_id = qkp.knowledge_point_id
        WHERE q.is_deprecated = 0
            AND (
                q.stem LIKE :pattern ESCAPE '\'
                OR q.explanation LIKE :pattern ESCAPE '\'
                OR q.answer LIKE :pattern ESCAPE '\'
                OR c.name LIKE :pattern ESCAPE '\'
                OR o.content LIKE :pattern ESCAPE '\'
                OR kp.name LIKE :pattern ESCAPE '\'
                OR kp.description LIKE :pattern ESCAPE '\'
            )
        ORDER BY q.sort_order
        """
    )
    suspend fun searchQuestions(pattern: String): List<QuestionEntity>

    @Query("SELECT * FROM question_options WHERE question_id = :questionId ORDER BY sort_order")
    suspend fun listOptions(questionId: String): List<QuestionOptionEntity>

    @Query("SELECT * FROM question_knowledge_points WHERE question_id = :questionId")
    suspend fun listKnowledgePointRefs(questionId: String): List<QuestionKnowledgePointEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuestions(entities: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOptions(entities: List<QuestionOptionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertKnowledgePointRefs(entities: List<QuestionKnowledgePointEntity>)

    @Query("DELETE FROM question_knowledge_points")
    suspend fun deleteKnowledgePointRefs()

    @Query("DELETE FROM question_options")
    suspend fun deleteOptions()

    @Query("DELETE FROM questions")
    suspend fun deleteQuestions()
}

@Dao
interface LearningDao {
    @Query("SELECT * FROM answer_records ORDER BY answered_at DESC")
    suspend fun listAnswerRecords(): List<AnswerRecordEntity>

    @Query("SELECT * FROM answer_records ORDER BY answered_at DESC LIMIT :limit")
    suspend fun listRecentAnswerRecords(limit: Int): List<AnswerRecordEntity>

    @Query("SELECT COUNT(*) FROM answer_records WHERE question_id = :questionId")
    suspend fun countAnswerRecordsByQuestion(questionId: String): Int

    @Query("SELECT COUNT(*) FROM answer_records WHERE question_id = :questionId AND is_correct = 1")
    suspend fun countCorrectAnswerRecordsByQuestion(questionId: String): Int

    @Query("SELECT MAX(answered_at) FROM answer_records")
    suspend fun latestAnsweredAt(): Long?

    @Query("SELECT * FROM wrong_questions WHERE status = 'ACTIVE' ORDER BY last_wrong_at DESC")
    suspend fun listActiveWrongQuestions(): List<WrongQuestionEntity>

    @Query(
        """
        SELECT * FROM wrong_questions
        ORDER BY CASE status WHEN 'ACTIVE' THEN 0 ELSE 1 END, last_wrong_at DESC
        """
    )
    suspend fun listWrongQuestions(): List<WrongQuestionEntity>

    @Query("SELECT * FROM wrong_questions WHERE question_id = :questionId")
    suspend fun findWrongQuestion(questionId: String): WrongQuestionEntity?

    @Query("SELECT * FROM favorite_questions ORDER BY created_at DESC")
    suspend fun listFavoriteQuestions(): List<FavoriteQuestionEntity>

    @Query("SELECT * FROM favorite_questions WHERE question_id = :questionId")
    suspend fun findFavoriteQuestion(questionId: String): FavoriteQuestionEntity?

    @Query("SELECT * FROM practice_sessions WHERE status = 'IN_PROGRESS' ORDER BY updated_at DESC LIMIT 1")
    suspend fun getLatestInProgressSession(): PracticeSessionEntity?

    @Query("SELECT * FROM practice_sessions WHERE session_id = :sessionId")
    suspend fun findPracticeSession(sessionId: String): PracticeSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswerRecord(entity: AnswerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWrongQuestion(entity: WrongQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFavoriteQuestion(entity: FavoriteQuestionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPracticeSession(entity: PracticeSessionEntity)

    @Update
    suspend fun updatePracticeSession(entity: PracticeSessionEntity)

    @Query("DELETE FROM favorite_questions WHERE question_id = :questionId")
    suspend fun deleteFavoriteQuestion(questionId: String)

    @Query("DELETE FROM answer_records")
    suspend fun deleteAnswerRecords()

    @Query("DELETE FROM wrong_questions")
    suspend fun deleteWrongQuestions()

    @Query("DELETE FROM favorite_questions")
    suspend fun deleteFavoriteQuestions()

    @Query("DELETE FROM practice_sessions")
    suspend fun deletePracticeSessions()
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings WHERE key = :key")
    suspend fun find(key: String): AppSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppSettingEntity)

    @Query("DELETE FROM app_settings WHERE key = :key")
    suspend fun delete(key: String)
}
