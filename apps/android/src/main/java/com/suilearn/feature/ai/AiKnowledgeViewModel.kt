package com.suilearn.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.di.AppDependencies
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiKnowledgeViewModel(
    dependencies: AppDependencies,
) : ViewModel() {
    private val repository = dependencies.aiKnowledgeRemoteRepository
    private val _uiState = MutableStateFlow(AiKnowledgeUiState())
    val uiState: StateFlow<AiKnowledgeUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    serviceMessage = null,
                    actionMessage = null,
                )
            }
            val providerResult = repository.getProviderStatus()
            val contentsResult = repository.listReviewContents()

            _uiState.update { current ->
                val provider = providerResult.getOrNull()
                val contents = contentsResult.getOrNull().orEmpty()
                val failure = providerResult.exceptionOrNull() ?: contentsResult.exceptionOrNull()
                current.copy(
                    isLoading = false,
                    providerStatus = provider,
                    generatedContents = contents,
                    serviceMessage = failure?.let { "服务未连接，仍可离线刷题。" },
                    actionMessage = null,
                )
            }
        }
    }

    fun checkTask(taskId: String?) {
        if (taskId.isNullOrBlank()) {
            _uiState.update { it.copy(actionMessage = "该生成内容未返回任务 ID。") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(checkingTaskId = taskId, actionMessage = null) }
            val result = repository.getTaskStatus(taskId)
            _uiState.update {
                it.copy(
                    selectedTaskStatus = result.getOrNull(),
                    checkingTaskId = null,
                    actionMessage = result.exceptionOrNull()?.let { "任务状态读取失败。" },
                )
            }
        }
    }

    fun saveDraft(id: String) {
        reviewDraft(id = id, save = true)
    }

    fun discardDraft(id: String) {
        reviewDraft(id = id, save = false)
    }

    private fun reviewDraft(id: String, save: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(reviewingContentId = id, actionMessage = null) }
            val result = if (save) {
                repository.saveGeneratedContent(id)
            } else {
                repository.discardGeneratedContent(id)
            }
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        reviewingContentId = null,
                        actionMessage = if (save) "已提交保存确认。" else "已丢弃生成题。",
                    )
                }
                refresh()
            } else {
                _uiState.update {
                    it.copy(
                        reviewingContentId = null,
                        actionMessage = if (save) "保存确认失败。" else "丢弃失败。",
                    )
                }
            }
        }
    }
}
