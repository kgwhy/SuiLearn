package com.suilearn.core.repository

import com.suilearn.core.common.Clock
import com.suilearn.core.common.SystemClock
import com.suilearn.core.database.AnswerRecordEntity
import com.suilearn.core.database.AppSettingDao
import com.suilearn.core.database.AppSettingEntity
import com.suilearn.core.database.FavoriteQuestionEntity
import com.suilearn.core.database.LearningDao
import com.suilearn.core.database.PracticeSessionEntity
import com.suilearn.core.database.WrongQuestionEntity
import com.suilearn.core.database.decodeStringListFromJson
import com.suilearn.core.database.encodeStringListAsJson
import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.FavoriteQuestion
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.PracticeSessionStatus
import com.suilearn.core.model.WrongQuestion
import com.suilearn.core.model.WrongQuestionStatus

class RoomAnswerRecordRepository(
    private val learningDao: LearningDao,
) : AnswerRecordRepository {
    override suspend fun listAll(): List<AnswerRecord> =
        learningDao.listAnswerRecords().map { it.toDomain() }

    override suspend fun listRecent(limit: Int): List<AnswerRecord> =
        learningDao.listRecentAnswerRecords(limit).map { it.toDomain() }

    override suspend fun add(record: AnswerRecord) {
        learningDao.insertAnswerRecord(record.toEntity())
    }

    override suspend fun clear() {
        learningDao.deleteAnswerRecords()
    }

    override suspend fun countByQuestion(questionId: String): Int =
        learningDao.countAnswerRecordsByQuestion(questionId)

    override suspend fun countCorrectByQuestion(questionId: String): Int =
        learningDao.countCorrectAnswerRecordsByQuestion(questionId)

    override suspend fun latestAnsweredAt(): Long? = learningDao.latestAnsweredAt()
}

class RoomWrongQuestionRepository(
    private val learningDao: LearningDao,
) : WrongQuestionRepository {
    override suspend fun listActive(): List<WrongQuestion> =
        learningDao.listActiveWrongQuestions().map { it.toDomain() }

    override suspend fun upsertWrong(questionId: String, at: Long) {
        val current = learningDao.findWrongQuestion(questionId)
        val next = if (current == null) {
            WrongQuestionEntity(
                questionId = questionId,
                status = WrongQuestionStatus.ACTIVE.name,
                wrongCount = 1,
                firstWrongAt = at,
                lastWrongAt = at,
                masteredAt = null,
            )
        } else {
            current.copy(
                status = WrongQuestionStatus.ACTIVE.name,
                wrongCount = current.wrongCount + 1,
                lastWrongAt = at,
                masteredAt = null,
            )
        }
        learningDao.upsertWrongQuestion(next)
    }

    override suspend fun markMastered(questionId: String, at: Long) {
        val current = learningDao.findWrongQuestion(questionId) ?: return
        learningDao.upsertWrongQuestion(
            current.copy(
                status = WrongQuestionStatus.MASTERED.name,
                masteredAt = at,
            )
        )
    }

    override suspend fun get(questionId: String): WrongQuestion? =
        learningDao.findWrongQuestion(questionId)?.toDomain()

    override suspend fun clear() {
        learningDao.deleteWrongQuestions()
    }
}

class RoomFavoriteRepository(
    private val learningDao: LearningDao,
) : FavoriteRepository {
    override suspend fun listAll(): List<FavoriteQuestion> =
        learningDao.listFavoriteQuestions().map { it.toDomain() }

    override suspend fun toggle(questionId: String, now: Long): Boolean {
        val current = learningDao.findFavoriteQuestion(questionId)
        return if (current == null) {
            learningDao.upsertFavoriteQuestion(FavoriteQuestionEntity(questionId, now))
            true
        } else {
            learningDao.deleteFavoriteQuestion(questionId)
            false
        }
    }

    override suspend fun isFavorite(questionId: String): Boolean =
        learningDao.findFavoriteQuestion(questionId) != null

    override suspend fun clear() {
        learningDao.deleteFavoriteQuestions()
    }
}

class RoomPracticeSessionRepository(
    private val learningDao: LearningDao,
    private val clock: Clock = SystemClock,
) : PracticeSessionRepository {
    override suspend fun save(session: PracticeSession) {
        learningDao.upsertPracticeSession(session.toEntity())
    }

    override suspend fun update(session: PracticeSession) {
        learningDao.upsertPracticeSession(session.toEntity())
    }

    override suspend fun getLatestInProgress(): PracticeSession? =
        learningDao.getLatestInProgressSession()?.toDomain()

    override suspend fun find(sessionId: String): PracticeSession? =
        learningDao.findPracticeSession(sessionId)?.toDomain()

    override suspend fun markAbandoned(sessionId: String) {
        val current = learningDao.findPracticeSession(sessionId) ?: return
        learningDao.upsertPracticeSession(
            current.copy(
                status = PracticeSessionStatus.ABANDONED.name,
                updatedAt = clock.now(),
            )
        )
    }

    override suspend fun markCompleted(sessionId: String) {
        val current = learningDao.findPracticeSession(sessionId) ?: return
        learningDao.upsertPracticeSession(
            current.copy(
                status = PracticeSessionStatus.COMPLETED.name,
                updatedAt = clock.now(),
            )
        )
    }

    override suspend fun clear() {
        learningDao.deletePracticeSessions()
    }
}

class RoomSettingsRepository(
    private val appSettingDao: AppSettingDao,
    private val clock: Clock = SystemClock,
) : SettingsRepository {
    override suspend fun getCurrentPackId(): String? =
        appSettingDao.find(CURRENT_PACK_ID_KEY)?.value

    override suspend fun setCurrentPackId(packId: String?) {
        if (packId == null) {
            appSettingDao.delete(CURRENT_PACK_ID_KEY)
        } else {
            appSettingDao.upsert(
                AppSettingEntity(
                    key = CURRENT_PACK_ID_KEY,
                    value = packId,
                    updatedAt = clock.now(),
                )
            )
        }
    }

    override suspend fun resetLearningSettingsOnly() {
        // The current pack is app state, not learning progress, so reset keeps it.
    }

    private companion object {
        const val CURRENT_PACK_ID_KEY = "current_pack_id"
    }
}

private fun AnswerRecord.toEntity(): AnswerRecordEntity =
    AnswerRecordEntity(
        recordId = recordId,
        questionId = questionId,
        practiceMode = practiceMode.name,
        targetId = targetId,
        userAnswer = encodeStringListAsJson(userAnswer),
        isCorrect = isCorrect,
        durationMs = durationMs,
        answeredAt = answeredAt,
    )

private fun AnswerRecordEntity.toDomain(): AnswerRecord =
    AnswerRecord(
        recordId = recordId,
        questionId = questionId,
        practiceMode = PracticeMode.valueOf(practiceMode),
        targetId = targetId,
        userAnswer = decodeStringListFromJson(userAnswer),
        isCorrect = isCorrect,
        durationMs = durationMs,
        answeredAt = answeredAt,
    )

private fun WrongQuestionEntity.toDomain(): WrongQuestion =
    WrongQuestion(
        questionId = questionId,
        status = WrongQuestionStatus.valueOf(status),
        wrongCount = wrongCount,
        firstWrongAt = firstWrongAt,
        lastWrongAt = lastWrongAt,
        masteredAt = masteredAt,
    )

private fun FavoriteQuestionEntity.toDomain(): FavoriteQuestion =
    FavoriteQuestion(
        questionId = questionId,
        createdAt = createdAt,
    )

private fun PracticeSession.toEntity(): PracticeSessionEntity =
    PracticeSessionEntity(
        sessionId = sessionId,
        practiceMode = practiceMode.name,
        targetId = targetId,
        questionIds = encodeStringListAsJson(questionIds),
        currentIndex = currentIndex,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

private fun PracticeSessionEntity.toDomain(): PracticeSession =
    PracticeSession(
        sessionId = sessionId,
        practiceMode = PracticeMode.valueOf(practiceMode),
        targetId = targetId,
        questionIds = decodeStringListFromJson(questionIds),
        currentIndex = currentIndex,
        status = PracticeSessionStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
