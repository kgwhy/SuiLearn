package com.suilearn.feature.wrongbook

sealed interface WrongBookEvent {
    object Refresh : WrongBookEvent

    data class MarkMastered(
        val questionId: String,
    ) : WrongBookEvent

    data class SelectKnowledgePoint(
        val knowledgePointId: String?,
    ) : WrongBookEvent

    data class ShowMasteredChanged(
        val showMastered: Boolean,
    ) : WrongBookEvent
}
