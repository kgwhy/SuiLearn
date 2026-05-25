package com.suilearn.feature.practice

import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.ShortAnswerReview

sealed interface PracticeEvent {
    data class StartPractice(
        val mode: PracticeMode,
        val targetId: String? = null,
    ) : PracticeEvent

    data class StartFromQuestion(
        val questionId: String,
    ) : PracticeEvent

    data class Resume(
        val sessionId: String? = null,
    ) : PracticeEvent

    data class SubmitAnswer(
        val answer: List<String>,
        val durationMs: Long = 0L,
    ) : PracticeEvent

    data class ReviewShortAnswer(
        val review: ShortAnswerReview,
        val durationMs: Long = 0L,
    ) : PracticeEvent

    object ToggleFavorite : PracticeEvent

    object NextQuestion : PracticeEvent
}
