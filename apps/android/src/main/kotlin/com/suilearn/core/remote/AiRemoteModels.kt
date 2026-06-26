package com.suilearn.core.remote

data class AiProviderStatus(
    val providerType: String,
    val configured: Boolean,
    val available: Boolean,
    val chatModel: String?,
    val embeddingModel: String?,
    val embeddingDimensions: Int?,
    val message: String?,
    val baseUrl: String? = null,
    val apiKeyEnvName: String? = null,
    val timeoutMs: Int? = null,
    val maxRetries: Int? = null,
)

data class GeneratedQuestionDraft(
    val id: String,
    val knowledgeBaseId: String,
    val generationTaskId: String?,
    val status: String,
    val questionType: String,
    val categoryId: String,
    val categoryName: String,
    val knowledgePointIds: List<String>,
    val stem: String,
    val options: List<String>,
    val answer: List<String>,
    val explanation: String,
    val savedQuestionId: String?,
)

data class TaskStatus(
    val id: String,
    val kind: String,
    val status: String,
    val currentStep: String?,
    val errorMessage: String?,
    val progressPercent: Int? = null,
    val errorCode: String? = null,
)
