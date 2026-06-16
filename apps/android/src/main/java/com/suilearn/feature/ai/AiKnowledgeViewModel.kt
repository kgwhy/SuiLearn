package com.suilearn.feature.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.di.AppDependencies
import com.suilearn.core.model.Category
import com.suilearn.core.model.KnowledgePoint
import com.suilearn.core.model.Question
import com.suilearn.core.model.QuestionOption
import com.suilearn.core.model.QuestionType
import com.suilearn.core.remote.GeneratedQuestionDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AiKnowledgeViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val repository = dependencies.aiKnowledgeRemoteRepository
    private val questionRepository = dependencies.questionRepository
    private val studyPackRepository = dependencies.studyPackRepository
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
                val imported = if (save) {
                    result.getOrNull()?.let { importSavedDraft(it) } == true
                } else {
                    false
                }
                _uiState.update {
                    it.copy(
                        reviewingContentId = null,
                        actionMessage = when {
                            !save -> "已丢弃生成题。"
                            imported -> "已保存并写入本地题库。"
                            else -> "已保存到后端；本地题库导入失败，请刷新后重试。"
                        },
                    )
                }
                if (imported) {
                    dependencies.notifyDataChanged()
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

    private suspend fun importSavedDraft(draft: GeneratedQuestionDraft): Boolean {
        if (draft.status != "SAVED") return false
        val pack = studyPackRepository.getCurrentPack() ?: run {
            dependencies.ensureSeeded()
            studyPackRepository.getCurrentPack()
        } ?: return false
        val existingCategories = studyPackRepository.listCategories()
        val fallbackCategory = existingCategories.firstOrNull()
        val categoryId = draft.categoryId.ifBlank { fallbackCategory?.categoryId ?: "ai-generated" }
        if (existingCategories.none { it.categoryId == categoryId }) {
            studyPackRepository.upsertCategory(
                Category(
                    categoryId = categoryId,
                    packId = pack.packId,
                    name = draft.categoryName.ifBlank { "AI 生成题" },
                    description = "AI 生成题自动归类",
                    sortOrder = (existingCategories.maxOfOrNull { it.sortOrder } ?: 0) + 1,
                )
            )
        }
        val existingPoints = studyPackRepository.listKnowledgePoints()
        val pointIds = draft.knowledgePointIds.ifEmpty { listOf("ai-generated-${draft.id}") }
        pointIds.forEachIndexed { index, pointId ->
            if (existingPoints.none { it.knowledgePointId == pointId }) {
                studyPackRepository.upsertKnowledgePoint(
                    KnowledgePoint(
                        knowledgePointId = pointId,
                        packId = pack.packId,
                        categoryId = categoryId,
                        name = if (index == 0) draft.categoryName.ifBlank { "AI 生成知识点" } else "AI 生成知识点 ${index + 1}",
                        description = "从后端 AI 生成题沉淀的知识点",
                        sortOrder = (existingPoints.maxOfOrNull { it.sortOrder } ?: 0) + index + 1,
                    )
                )
            }
        }
        val questionId = draft.savedQuestionId ?: draft.id
        val nextSortOrder = (questionRepository.listQuestions().maxOfOrNull { it.sortOrder } ?: 0) + 1
        questionRepository.upsertQuestion(
            Question(
                questionId = questionId,
                packId = pack.packId,
                categoryId = categoryId,
                type = runCatching { QuestionType.valueOf(draft.questionType) }.getOrDefault(QuestionType.SINGLE_CHOICE),
                stem = draft.stem,
                answer = draft.answer,
                explanation = draft.explanation,
                difficulty = 3,
                isDeprecated = false,
                sortOrder = nextSortOrder,
                options = draft.options.mapIndexed { index, raw ->
                    val key = raw.substringBefore(".", "").trim().takeIf { it.length <= 3 && it.isNotBlank() }
                        ?: ('A' + index).toString()
                    val content = raw.substringAfter(".", raw).trim()
                    QuestionOption(
                        optionId = "${questionId}_ai_option_${index + 1}",
                        questionId = questionId,
                        optionKey = key,
                        content = content,
                        sortOrder = index + 1,
                    )
                },
                knowledgePointIds = pointIds,
            )
        )
        return true
    }
}
