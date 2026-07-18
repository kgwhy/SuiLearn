package com.suilearn.api.agent.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import com.suilearn.api.agent.application.LearningAgentPort.Difficulty;
import com.suilearn.api.agent.application.LearningAgentPort.StudyRunRequest;
import com.suilearn.api.agent.application.LearningAgentPort.StudyRunResult;
import java.util.List;
import org.junit.jupiter.api.Test;

class LearningAgentContractTest {
    @Test
    void validatesScopeAndPracticeBoundsBeforeInfrastructureInvocation() {
        assertThatThrownBy(() -> new StudyRunRequest(
            "learner-1", null, "question", new AgentScope("kb-1", null), 0, Difficulty.MEDIUM))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("practiceCount must be between 1 and 5");
        assertThatThrownBy(() -> new StudyRunRequest(
            "learner-1", null, " ", new AgentScope("kb-1", null), 3, Difficulty.MEDIUM))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("question is required");
    }

    @Test
    void representsNoEvidenceAsUncertainWithoutCitationsOrPractice() {
        LearningAgentPort fakeFrameworkAdapter = request -> StudyRunResult.noEvidence(
            "run-1", request.sessionId() == null ? "generated-session" : request.sessionId(),
            new LearningAgentPort.BudgetUsage(1, 1, 0, false));

        StudyRunResult result = fakeFrameworkAdapter.run(new StudyRunRequest(
            "learner-1", null, "unknown topic", new AgentScope(null, "material-1"), 3, Difficulty.MEDIUM));

        assertThat(result.status()).isEqualTo(LearningAgentPort.RunStatus.NO_EVIDENCE);
        assertThat(result.uncertain()).isTrue();
        assertThat(result.citations()).isEmpty();
        assertThat(result.exercises()).isEmpty();
        assertThat(result.sessionId()).isEqualTo("generated-session");
    }

    @Test
    void publicResultContainsControlledTraceButNoReasoningOrRawModelFields() {
        assertThat(StudyRunResult.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
            .contains("actionTrace", "budgetUsage")
            .doesNotContain("reasoning", "chainOfThought", "prompt", "rawModelResponse", "transcript");
        var trace = new LearningAgentPort.ActionTrace(
            1, LearningAgentPort.TraceActor.KNOWLEDGE_RESEARCH, LearningAgentPort.TraceAction.SEARCH_KNOWLEDGE,
            LearningAgentPort.TraceStatus.COMPLETED, 12);
        assertThat(trace.toString()).doesNotContain("learner-1", "question-body", "evidence-body");
    }

    @Test
    void citationExerciseBudgetAndMemoryContractsMatchTheStableApiShape() {
        var citation = new LearningAgentPort.Citation("e-1", "source-1", "material-1", "revision-1",
            2, null, "excerpt", false);
        var exercise = new LearningAgentPort.Exercise("practice-1", LearningAgentPort.ExerciseType.SHORT_ANSWER,
            "question", List.of(), "answer", "explanation", List.of(citation));
        var budget = new LearningAgentPort.BudgetUsage(1, 4, 2, 3, 4, 8, 25, 90_000, false);

        assertThat(citation.materialId()).isEqualTo("material-1");
        assertThat(exercise.id()).isEqualTo("practice-1");
        assertThat(budget.maxToolCalls()).isEqualTo(8);
        assertThat(LearningAgentPort.MemoryStatus.notAttempted().semanticPersistence())
            .isEqualTo(LearningAgentPort.SemanticPersistenceStatus.NOT_ATTEMPTED);
        assertThatThrownBy(() -> new LearningAgentPort.Citation("e", "s", "m", "r", null, null,
            "excerpt", false)).isInstanceOf(IllegalArgumentException.class)
            .hasMessage("pageNumber or blockId is required");
    }
}
