package com.suilearn.core.repository

import com.suilearn.BuildConfig
import com.suilearn.core.remote.AiProviderStatus
import com.suilearn.core.remote.AiRemoteApiClient
import com.suilearn.core.remote.GeneratedQuestionDraft
import com.suilearn.core.remote.TaskStatus

class AiKnowledgeRemoteRepository(
    private val client: AiRemoteApiClient = AiRemoteApiClient(baseUrl = BuildConfig.SUILEARN_API_BASE_URL),
) {
    suspend fun getProviderStatus(): Result<AiProviderStatus> = runCatching {
        client.getProviderStatus()
    }

    suspend fun listReviewContents(): Result<List<GeneratedQuestionDraft>> = runCatching {
        val pending = client.listGeneratedContents(status = "PENDING_REVIEW")
        val saved = client.listGeneratedContents(status = "SAVED")
        pending + saved
    }

    suspend fun getTaskStatus(taskId: String): Result<TaskStatus> = runCatching {
        client.getTaskStatus(taskId)
    }

    suspend fun saveGeneratedContent(id: String): Result<GeneratedQuestionDraft> = runCatching {
        client.reviewGeneratedContent(id = id, status = "SAVED")
    }

    suspend fun discardGeneratedContent(id: String): Result<GeneratedQuestionDraft> = runCatching {
        client.reviewGeneratedContent(id = id, status = "DISCARDED")
    }
}
