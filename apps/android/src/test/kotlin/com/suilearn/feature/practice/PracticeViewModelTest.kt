package com.suilearn.feature.practice

import com.suilearn.core.importer.QuestionPackSource
import com.suilearn.core.model.PracticeMode
import com.suilearn.di.AppDependencies
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PracticeViewModelTest {
    @Test
    fun `previous question restores submitted state without rolling back learning records`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val dependencies = AppDependencies(
                object : QuestionPackSource {
                    override fun loadQuestionPackJson(): String = assetJson()
                }
            )
            val viewModel = PracticeViewModel(dependencies)

            viewModel.onEvent(PracticeEvent.StartPractice(PracticeMode.SEQUENTIAL))
            waitForState { viewModel.uiState.value.practiceState != null }

            val firstQuestionState = assertNotNull(viewModel.uiState.value.practiceState)
            val firstQuestionId = firstQuestionState.question.questionId
            val firstAnswer = firstQuestionState.question.answer

            viewModel.onEvent(PracticeEvent.PreviousQuestion)
            waitForState { viewModel.uiState.value.practiceState?.index == 0 }

            viewModel.onEvent(PracticeEvent.SubmitAnswer(firstAnswer))
            waitForState { viewModel.uiState.value.practiceState?.submitted == true }
            assertEquals(1, dependencies.answerRecordRepository.listAll().size)

            viewModel.onEvent(PracticeEvent.NextQuestion)
            waitForState { viewModel.uiState.value.practiceState?.index == 1 }

            viewModel.onEvent(PracticeEvent.PreviousQuestion)
            waitForState {
                viewModel.uiState.value.practiceState?.question?.questionId == firstQuestionId
            }

            val restoredState = assertNotNull(viewModel.uiState.value.practiceState)
            assertEquals(0, restoredState.index)
            assertTrue(restoredState.submitted)
            assertEquals(firstAnswer.toSet(), restoredState.selectedAnswers)
            assertEquals(true, restoredState.isCorrect)
            assertEquals(1, dependencies.answerRecordRepository.listAll().size)

            viewModel.onEvent(PracticeEvent.NextQuestion)
            waitForState { viewModel.uiState.value.practiceState?.index == 1 }
            assertEquals(1, dependencies.answerRecordRepository.listAll().size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private suspend fun TestScope.waitForState(predicate: () -> Boolean) {
        repeat(200) {
            if (predicate()) return
            advanceUntilIdle()
            Thread.sleep(10)
        }
        throw AssertionError("Timed out waiting for practice state.")
    }

    private fun assetJson(): String {
        val candidates = listOf(
            File("src/main/assets/question_pack_java_interview.json"),
            File("apps/android/src/main/assets/question_pack_java_interview.json"),
        )
        val file = candidates.firstOrNull { it.isFile }
            ?: error("Cannot find bundled question pack asset.")
        return file.readText(Charsets.UTF_8)
    }
}
