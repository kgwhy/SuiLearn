package com.suilearn.feature.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suilearn.core.common.AppResult
import com.suilearn.core.model.PracticeMode
import com.suilearn.core.model.PracticeQuestionState
import com.suilearn.core.model.PracticeSession
import com.suilearn.core.model.PracticeSessionStatus
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.ShortAnswerReview
import com.suilearn.core.usecase.BuildPracticeSessionUseCase
import com.suilearn.core.usecase.EvaluateShortAnswerUseCase
import com.suilearn.core.usecase.ResumePracticeSessionUseCase
import com.suilearn.core.usecase.StartPracticeFromQuestionUseCase
import com.suilearn.core.usecase.SubmitAnswerUseCase
import com.suilearn.core.usecase.ToggleFavoriteQuestionUseCase
import com.suilearn.di.AppDependencies
import com.suilearn.ui.model.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PracticeViewModel(
    private val dependencies: AppDependencies,
) : ViewModel() {
    private val buildPracticeSessionUseCase: BuildPracticeSessionUseCase = dependencies.buildPracticeSessionUseCase
    private val resumePracticeSessionUseCase: ResumePracticeSessionUseCase = dependencies.resumePracticeSessionUseCase
    private val submitAnswerUseCase: SubmitAnswerUseCase = dependencies.submitAnswerUseCase
    private val evaluateShortAnswerUseCase: EvaluateShortAnswerUseCase = dependencies.evaluateShortAnswerUseCase
    private val startPracticeFromQuestionUseCase: StartPracticeFromQuestionUseCase = dependencies.startPracticeFromQuestionUseCase
    private val toggleFavoriteQuestionUseCase: ToggleFavoriteQuestionUseCase = dependencies.toggleFavoriteQuestionUseCase

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    fun onEvent(event: PracticeEvent) {
        when (event) {
            is PracticeEvent.StartPractice -> startPractice(event.mode, event.targetId)
            is PracticeEvent.StartFromQuestion -> startPracticeFromQuestion(event.questionId)
            is PracticeEvent.Resume -> resumePractice(event.sessionId)
            is PracticeEvent.SubmitAnswer -> submitPracticeAnswer(
                answer = event.answer,
                durationMs = event.durationMs,
            )
            is PracticeEvent.ReviewShortAnswer -> reviewShortAnswer(event.review, event.durationMs)
            PracticeEvent.ToggleFavorite -> toggleFavorite()
            PracticeEvent.NextQuestion -> nextQuestion()
        }
    }

    private fun startPractice(mode: PracticeMode, targetId: String? = null) {
        viewModelScope.launch {
            val practiceState = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                buildPracticeSessionUseCase.execute(mode, targetId).dataOrNull()
                    ?.let { toPracticeState(it) }
            }
            if (practiceState == null) {
                updateMessage("无法开始练习。")
                return@launch
            }
            _uiState.update { it.copy(practiceState = practiceState, message = null) }
            dependencies.notifyDataChanged()
        }
    }

    private fun startPracticeFromQuestion(questionId: String) {
        viewModelScope.launch {
            val practiceState = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                startPracticeFromQuestionUseCase.execute(questionId).dataOrNull()
                    ?.let { toPracticeState(it) }
            }
            if (practiceState == null) {
                updateMessage("题目不存在或已弃用。")
                return@launch
            }
            _uiState.update { it.copy(practiceState = practiceState, message = null) }
            dependencies.notifyDataChanged()
        }
    }

    private fun resumePractice(sessionId: String? = null) {
        viewModelScope.launch {
            val session = withContext(Dispatchers.IO) {
                dependencies.ensureSeeded()
                when {
                    sessionId == null -> resumePracticeSessionUseCase.execute().dataOrNull()
                    else -> {
                        val found = dependencies.practiceSessionRepository.find(sessionId)
                        if (found != null &&
                            found.status == PracticeSessionStatus.IN_PROGRESS &&
                            hasAnyResumableQuestion(found)
                        ) {
                            found
                        } else {
                            null
                        }
                    }
                }
            }
            val practiceState = withContext(Dispatchers.IO) {
                session?.let { toPracticeState(it) }
            }
            _uiState.update { it.copy(practiceState = practiceState, message = null) }
        }
    }

    private fun submitPracticeAnswer(
        answer: List<String>,
        durationMs: Long = 0L,
    ) {
        val practiceState = _uiState.value.practiceState ?: return
        if (practiceState.submitted || practiceState.loading) return

        _uiState.update { current -> current.copy(practiceState = current.practiceState?.copy(loading = true), message = null) }

        viewModelScope.launch {
            if (practiceState.question.type == QuestionType.SHORT_ANSWER) {
                _uiState.update { current ->
                    current.copy(
                        practiceState = current.practiceState?.copy(
                            shortAnswerText = answer.joinToString("\n"),
                            submitted = true,
                            isCorrect = null,
                            showExplanation = true,
                            loading = false,
                        ),
                        message = null,
                    )
                }
                return@launch
            }

            val result = withContext(Dispatchers.IO) {
                submitAnswerUseCase.execute(
                    practiceMode = practiceState.session.practiceMode,
                    targetId = practiceState.session.targetId,
                    questionId = practiceState.question.questionId,
                    userAnswer = answer,
                    durationMs = durationMs,
                )
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            practiceState = current.practiceState?.copy(
                                submitted = true,
                                isCorrect = result.data.isCorrect,
                                isFavorite = result.data.isFavorite,
                                showExplanation = true,
                                loading = false,
                            ),
                            message = null,
                        )
                    }
                    dependencies.notifyDataChanged()
                }
                is AppResult.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            practiceState = current.practiceState?.copy(loading = false),
                            message = result.error.toString(),
                        )
                    }
                }
            }
        }
    }

    private fun reviewShortAnswer(
        review: ShortAnswerReview,
        durationMs: Long = 0L,
    ) {
        val practiceState = _uiState.value.practiceState ?: return
        if (practiceState.question.type != QuestionType.SHORT_ANSWER) return
        if (!practiceState.submitted) {
            updateMessage("请先提交答案。")
            return
        }
        if (practiceState.isCorrect != null || practiceState.loading) return

        _uiState.update { current -> current.copy(practiceState = current.practiceState?.copy(loading = true), message = null) }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                evaluateShortAnswerUseCase.execute(
                    practiceMode = practiceState.session.practiceMode,
                    targetId = practiceState.session.targetId,
                    questionId = practiceState.question.questionId,
                    userAnswer = listOf(practiceState.shortAnswerText),
                    review = review,
                    durationMs = durationMs,
                )
            }

            when (result) {
                is AppResult.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            practiceState = current.practiceState?.copy(
                                isCorrect = result.data.review == ShortAnswerReview.PASSED,
                                isFavorite = result.data.isFavorite,
                                loading = false,
                            ),
                            message = null,
                        )
                    }
                    dependencies.notifyDataChanged()
                }
                is AppResult.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            practiceState = current.practiceState?.copy(loading = false),
                            message = result.error.toString(),
                        )
                    }
                }
            }
        }
    }

    private fun nextQuestion() {
        val current = _uiState.value.practiceState ?: return
        if (!current.submitted) {
            updateMessage("请先提交答案。")
            return
        }
        if (current.question.type == QuestionType.SHORT_ANSWER && current.isCorrect == null) {
            updateMessage("请先选择自评结果。")
            return
        }

        viewModelScope.launch {
            val nextState = withContext(Dispatchers.IO) {
                val nextIndex = current.index + 1
                val nextQuestionId = current.session.questionIds.getOrNull(nextIndex)
                val nextQuestion = if (nextQuestionId == null) null else dependencies.questionRepository.getQuestion(nextQuestionId)
                if (nextQuestion == null) {
                    dependencies.practiceSessionRepository.markCompleted(current.session.sessionId)
                    null
                } else {
                    val nextSession = current.session.copy(currentIndex = nextIndex)
                    dependencies.practiceSessionRepository.update(nextSession)
                    PracticeQuestionState(
                        session = nextSession,
                        question = nextQuestion,
                        index = nextIndex,
                        total = current.total,
                        isFavorite = dependencies.favoriteRepository.isFavorite(nextQuestion.questionId),
                    )
                }
            }
            _uiState.update { it.copy(practiceState = nextState, message = null) }
            dependencies.notifyDataChanged()
        }
    }

    private fun toggleFavorite() {
        val practiceState = _uiState.value.practiceState ?: return
        viewModelScope.launch {
            val isFavorite = withContext(Dispatchers.IO) {
                toggleFavoriteQuestionUseCase.execute(practiceState.question.questionId).dataOrNull()
            } ?: return@launch
            _uiState.update { current ->
                current.copy(
                    practiceState = current.practiceState?.copy(isFavorite = isFavorite),
                    message = if (isFavorite) "已收藏" else "已取消收藏",
                )
            }
            dependencies.notifyDataChanged()
        }
    }

    private suspend fun toPracticeState(session: PracticeSession): PracticeQuestionState? {
        val questionId = session.questionIds.getOrNull(session.currentIndex) ?: return null
        val question = dependencies.questionRepository.getQuestion(questionId) ?: return null
        return PracticeQuestionState(
            session = session,
            question = question,
            index = session.currentIndex,
            total = session.questionIds.size,
            isFavorite = dependencies.favoriteRepository.isFavorite(question.questionId),
        )
    }

    private fun updateMessage(message: String?) {
        _uiState.update { it.copy(message = message) }
    }

    private suspend fun hasAnyResumableQuestion(session: PracticeSession): Boolean {
        for (questionId in session.questionIds) {
            if (dependencies.questionRepository.getQuestion(questionId)?.isDeprecated == false) {
                return true
            }
        }
        return false
    }
}
