package com.suilearn.feature.knowledge

import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.KnowledgePointDetail

data class KnowledgePointUiState(
    val point: KnowledgePoint? = null,
    val detail: KnowledgePointDetail? = null,
)

