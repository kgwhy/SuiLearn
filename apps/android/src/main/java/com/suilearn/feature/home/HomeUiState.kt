package com.suilearn.feature.home

import com.suilearn.core.common.HomeSummary
import com.suilearn.core.model.KnowledgePoint

data class HomeUiState(
    val homeSummary: HomeSummary? = null,
    val knowledgePoints: List<KnowledgePoint> = emptyList(),
)

