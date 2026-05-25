package com.suilearn.feature.knowledge

sealed interface KnowledgePointEvent {
    object LoadList : KnowledgePointEvent

    data class Load(
        val knowledgePointId: String,
    ) : KnowledgePointEvent
}
