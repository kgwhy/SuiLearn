package com.suilearn.feature.practice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.suilearn.core.model.QuestionType
import com.suilearn.core.model.ShortAnswerReview
import com.suilearn.ui.AppSectionCard
import com.suilearn.ui.EmptyState

@Composable
fun PracticeScreen(
    practiceViewModel: PracticeViewModel,
    onFinish: () -> Unit,
) {
    val uiState by practiceViewModel.uiState.collectAsStateWithLifecycle()
    val practiceState = uiState.practiceState
    var selectedAnswers by remember(practiceState?.question?.questionId) { mutableStateOf(setOf<String>()) }
    var shortAnswer by remember(practiceState?.question?.questionId) { mutableStateOf("") }
    var shortAnswerReview by remember(practiceState?.question?.questionId) { mutableStateOf<ShortAnswerReview?>(null) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(practiceState?.question?.questionId) {
        selectedAnswers = emptySet()
        shortAnswer = ""
        shortAnswerReview = null
    }

    if (showFinishDialog) {
        AlertDialog(
            onDismissRequest = { showFinishDialog = false },
            title = { Text("结束练习") },
            text = { Text("这将关闭当前练习会话。") },
            confirmButton = {
                TextButton(onClick = {
                    showFinishDialog = false
                    onFinish()
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showFinishDialog = false }) { Text("取消") }
            },
        )
    }

    if (practiceState == null) {
        EmptyState(
            title = "当前没有练习会话",
            subtitle = "可从首页、分类或搜索开始练习。",
            action = "返回",
            onActionClick = onFinish,
        )
        return
    }

    val submitted = practiceState.submitted
    val question = practiceState.question
    val displayedShortAnswer = if (submitted) practiceState.shortAnswerText else shortAnswer

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp),
    ) {
        item {
            AppSectionCard(
                title = "练习 ${practiceState.index + 1}/${practiceState.total}",
                subtitle = practiceState.question.questionId,
                action = "结束",
                onActionClick = { showFinishDialog = true },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(question.stem, fontWeight = FontWeight.SemiBold)
                    Text("题型：${question.type.label()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (submitted) {
                        Text(
                            text = when (practiceState.isCorrect) {
                                true -> "正确"
                                false -> "未答对"
                                null -> "待自评"
                            },
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
        item {
            AppSectionCard(title = "作答") {
                when (question.type) {
                    QuestionType.SINGLE_CHOICE, QuestionType.TRUE_FALSE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            question.options.forEach { option ->
                                FilterChip(
                                    selected = selectedAnswers.contains(option.optionKey),
                                    onClick = {
                                        if (!submitted) {
                                            selectedAnswers = setOf(option.optionKey)
                                        }
                                    },
                                    label = { Text("${option.optionKey}. ${option.content}") },
                                )
                            }
                        }
                    }
                    QuestionType.MULTIPLE_CHOICE -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            question.options.forEach { option ->
                                FilterChip(
                                    selected = selectedAnswers.contains(option.optionKey),
                                    onClick = {
                                        if (!submitted) {
                                            selectedAnswers = if (selectedAnswers.contains(option.optionKey)) {
                                                selectedAnswers - option.optionKey
                                            } else {
                                                selectedAnswers + option.optionKey
                                            }
                                        }
                                    },
                                    label = { Text("${option.optionKey}. ${option.content}") },
                                )
                            }
                        }
                    }
                    QuestionType.SHORT_ANSWER -> {
                        OutlinedTextField(
                            value = displayedShortAnswer,
                            onValueChange = {
                                if (!submitted) {
                                    shortAnswer = it
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("答案") },
                            enabled = !submitted,
                        )
                    }
                }
                if (submitted) {
                    Spacer(Modifier.height(12.dp))
                    AnswerResultBlock(
                        questionType = question.type,
                        isCorrect = practiceState.isCorrect,
                        explanation = question.explanation,
                        shortAnswerReview = shortAnswerReview,
                        answer = when (question.type) {
                            QuestionType.SHORT_ANSWER -> listOf(displayedShortAnswer)
                            else -> selectedAnswers.toList()
                        },
                        correctAnswer = question.answer,
                    )
                    if (question.type == QuestionType.SHORT_ANSWER && practiceState.isCorrect == null) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SelfReviewChip(
                                label = "通过",
                                selected = shortAnswerReview == ShortAnswerReview.PASSED,
                                enabled = !practiceState.loading,
                                onClick = {
                                    shortAnswerReview = ShortAnswerReview.PASSED
                                    practiceViewModel.onEvent(PracticeEvent.ReviewShortAnswer(ShortAnswerReview.PASSED))
                                },
                            )
                            SelfReviewChip(
                                label = "未通过",
                                selected = shortAnswerReview == ShortAnswerReview.NOT_PASSED,
                                enabled = !practiceState.loading,
                                onClick = {
                                    shortAnswerReview = ShortAnswerReview.NOT_PASSED
                                    practiceViewModel.onEvent(PracticeEvent.ReviewShortAnswer(ShortAnswerReview.NOT_PASSED))
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val answer = when (question.type) {
                                QuestionType.SHORT_ANSWER -> listOf(shortAnswer.trim())
                                else -> selectedAnswers.toList()
                            }
                            practiceViewModel.onEvent(
                                PracticeEvent.SubmitAnswer(
                                    answer = answer,
                                )
                            )
                        },
                        enabled = when (question.type) {
                            QuestionType.SHORT_ANSWER -> shortAnswer.isNotBlank() && !submitted
                            else -> selectedAnswers.isNotEmpty() && !submitted
                        },
                    ) {
                        Icon(Icons.Outlined.CheckCircleOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("提交")
                    }
                    OutlinedButton(
                        onClick = {
                            if (submitted) {
                                selectedAnswers = emptySet()
                                shortAnswer = ""
                                shortAnswerReview = null
                                practiceViewModel.onEvent(PracticeEvent.NextQuestion)
                            }
                        },
                        enabled = submitted && (question.type != QuestionType.SHORT_ANSWER || practiceState.isCorrect != null),
                    ) {
                        Text("下一题")
                    }
                }
            }
        }
        if (uiState.message != null) {
            item {
                AppSectionCard(title = "提示") {
                    Text(uiState.message.orEmpty())
                }
            }
        }
    }
}

@Composable
private fun AnswerResultBlock(
    questionType: QuestionType,
    isCorrect: Boolean?,
    explanation: String,
    shortAnswerReview: ShortAnswerReview?,
    answer: List<String>,
    correctAnswer: List<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (questionType == QuestionType.SHORT_ANSWER) {
            Text(
                text = when {
                    shortAnswerReview == ShortAnswerReview.PASSED || isCorrect == true -> "自评：通过"
                    shortAnswerReview == ShortAnswerReview.NOT_PASSED || isCorrect == false -> "自评：未通过"
                    else -> "请自评：通过 / 未通过"
                },
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            Text(
                text = when (isCorrect) {
                    true -> "正确"
                    false -> "错误"
                    null -> "待确认"
                },
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text("你的答案：${answer.joinToString(", ")}")
        Text("正确答案：${correctAnswer.joinToString(", ")}")
        Text("解析：$explanation")
    }
}

@Composable
private fun SelfReviewChip(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
    )
}

private fun QuestionType.label(): String = when (this) {
    QuestionType.SINGLE_CHOICE -> "单选题"
    QuestionType.MULTIPLE_CHOICE -> "多选题"
    QuestionType.TRUE_FALSE -> "判断题"
    QuestionType.SHORT_ANSWER -> "简答题"
}
