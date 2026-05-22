package com.suilearn.feature.practice

import com.suilearn.core.model.PracticeQuestionState

data class PracticeUiState(
    val practiceState: PracticeQuestionState? = null,
    val message: String? = null,
)

