package com.suilearn.core.remote

data class AiProviderStatus(
    val providerType: String,
    val configured: Boolean,
    val available: Boolean,
    val chatModel: String?,
    val embeddingModel: String?,
    val embeddingDimensions: Int?,
    val message: String?,
)

data class GeneratedQuestionDraft(
    val id: String,
    val knowledgeBaseId: String,
    val generationTaskId: String?,
    val status: String,
    val stem: String,
)

data class TaskStatus(
    val id: String,
    val kind: String,
    val status: String,
    val currentStep: String?,
    val errorMessage: String?,
)
