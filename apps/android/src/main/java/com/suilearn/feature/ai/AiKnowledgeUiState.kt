package com.suilearn.feature.ai

import com.suilearn.core.remote.AiProviderStatus
import com.suilearn.core.remote.GeneratedQuestionDraft
import com.suilearn.core.remote.TaskStatus

data class AiKnowledgeUiState(
    val isLoading: Boolean = true,
    val providerStatus: AiProviderStatus? = null,
    val generatedContents: List<GeneratedQuestionDraft> = emptyList(),
    val selectedTaskStatus: TaskStatus? = null,
    val serviceMessage: String? = null,
    val actionMessage: String? = null,
    val checkingTaskId: String? = null,
    val reviewingContentId: String? = null,
) {
    val isServiceConnected: Boolean
        get() = providerStatus != null && serviceMessage == null
}
