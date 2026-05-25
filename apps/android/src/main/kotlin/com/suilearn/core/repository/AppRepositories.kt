package com.suilearn.core.repository

import com.suilearn.core.common.AppResult
import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.FavoriteQuestion
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.KnowledgePointDetail
import com.suilearn.core.model.CategoryProgress
import com.suilearn.core.model.KnowledgePointProgressSummary
import com.suilearn.core.model.RecentLearningRecord
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeQuestionState
import com.suilearn.core.model.PracticeSubmission
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.Question
import com.suilearn.core.model.QuestionPack
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.SearchResult
import com.suilearn.core.model.StatisticsSummary
import com.suilearn.core.model.StudyPack
import com.suilearn.core.model.ShortAnswerEvaluation
import com.suilearn.core.model.ShortAnswerReview
import com.suilearn.core.model.WrongQuestion

interface StudyPackRepository {
    suspend fun getCurrentPack(): StudyPack?
    suspend fun upsertPack(pack: StudyPack)
    suspend fun listKnowledgePoints(): List<KnowledgePoint>
    suspend fun listCategories(): List<com.suilearn.core.model.Category>
    suspend fun replaceCategories(items: List<com.suilearn.core.model.Category>)
    suspend fun replaceKnowledgePoints(items: List<KnowledgePoint>)
}

interface QuestionRepository {
    suspend fun listQuestions(): List<Question>
    suspend fun getQuestion(questionId: String): Question?
    suspend fun replaceAll(pack: QuestionPack)
}

interface AnswerRecordRepository {
    suspend fun listAll(): List<AnswerRecord>
    suspend fun listRecent(limit: Int = 20): List<AnswerRecord>
    suspend fun add(record: AnswerRecord)
    suspend fun clear()
    suspend fun countByQuestion(questionId: String): Int
    suspend fun countCorrectByQuestion(questionId: String): Int
    suspend fun latestAnsweredAt(): Long?
}

interface PracticeRepository {
    fun buildSession(mode: PracticeMode, targetId: String? = null): AppResult<PracticeSession>
    fun resumeLatestSession(): AppResult<PracticeSession?>
    fun getCurrentQuestion(sessionId: String): Question?
    fun submitAnswer(sessionId: String, questionId: String, userAnswer: List<String>, durationMs: Long): AppResult<PracticeSubmission>
}

interface PracticeSessionRepository {
    suspend fun save(session: PracticeSession)
    suspend fun update(session: PracticeSession)
    suspend fun getLatestInProgress(): PracticeSession?
    suspend fun find(sessionId: String): PracticeSession?
    suspend fun markAbandoned(sessionId: String)
    suspend fun markCompleted(sessionId: String)
    suspend fun clear()
}

interface WrongQuestionRepository {
    suspend fun listAll(): List<WrongQuestion>
    suspend fun listActive(): List<WrongQuestion>
    suspend fun upsertWrong(questionId: String, at: Long)
    suspend fun markMastered(questionId: String, at: Long)
    suspend fun get(questionId: String): WrongQuestion?
    suspend fun clear()
}

interface FavoriteRepository {
    suspend fun listAll(): List<FavoriteQuestion>
    suspend fun toggle(questionId: String, now: Long): Boolean
    suspend fun isFavorite(questionId: String): Boolean
    suspend fun clear()
}

interface SearchRepository {
    suspend fun search(query: String): List<SearchResult>
}

interface StatisticsRepository {
    suspend fun getSummary(): StatisticsSummary
    suspend fun getCategoryProgress(): List<CategoryProgress>
    suspend fun getKnowledgePointProgress(): List<KnowledgePointProgressSummary>
    suspend fun getRecentLearningRecords(limit: Int = 10): List<RecentLearningRecord>
    suspend fun getKnowledgePointDetail(knowledgePointId: String): KnowledgePointDetail?
}

interface SettingsRepository {
    suspend fun getCurrentPackId(): String?
    suspend fun setCurrentPackId(packId: String?)
    suspend fun resetLearningSettingsOnly()
    suspend fun resetLearningDataAtomic() = resetLearningSettingsOnly()
}
