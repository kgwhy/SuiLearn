package com.suilearn.core.common

import com.suilearn.core.model.AnswerRecord
import com.suilearn.core.model.KnowledgePointProgress
import com.suilearn.core.model.StudyPack

data class HomeSummary(
    val studyPack: StudyPack,
    val todayTitle: String,
    val resumeSessionId: String?,
    val totalQuestionCount: Int,
    val totalPracticedCount: Int,
    val totalCorrectRate: Int,
    val activeWrongCount: Int,
    val weakKnowledgePoints: List<KnowledgePointProgress>,
    val recentRecords: List<AnswerRecord>,
    val recentLearningDays: Int,
)
