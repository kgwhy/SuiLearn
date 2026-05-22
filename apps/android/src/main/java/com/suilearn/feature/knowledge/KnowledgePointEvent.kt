package com.suilearn.feature.knowledge

sealed interface KnowledgePointEvent {
    data class Load(
        val knowledgePointId: String,
    ) : KnowledgePointEvent
}
