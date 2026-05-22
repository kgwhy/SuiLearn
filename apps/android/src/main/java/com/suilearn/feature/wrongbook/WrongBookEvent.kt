package com.suilearn.feature.wrongbook

sealed interface WrongBookEvent {
    object Refresh : WrongBookEvent

    data class MarkMastered(
        val questionId: String,
    ) : WrongBookEvent
}
