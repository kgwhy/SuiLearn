package com.suilearn.api.agent.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.metrics.AgentMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.suilearn.api.agent.memory.LayerDeletion;
import com.suilearn.api.agent.memory.MemoryDeletionResult;
import com.suilearn.api.agent.memory.MemoryManager;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningAgentControllerTest {
    @Test
    void runsAgentAndReturnsOnlySanitizedContractFields() {
        var citation = new LearningAgentPort.Citation(
            "stable-1", "source-1", "material-1", "revision-1", 2, null, "excerpt", false);
        LearningAgentPort port = request -> {
            assertThat(request.difficulty()).isEqualTo(LearningAgentPort.Difficulty.HARD);
            return new LearningAgentPort.StudyRunResult(
            "run-1", request.sessionId(), "answer", false, List.of(citation),
            List.of(new LearningAgentPort.Exercise("practice-1", LearningAgentPort.ExerciseType.SHORT_ANSWER,
                "question", List.of(), "answer", "explanation", List.of(citation))), "review",
            LearningAgentPort.RunStatus.COMPLETED,
            new LearningAgentPort.BudgetUsage(1, 4, 2, 3, 3, 8, 12, 90_000, false),
            List.of(
                new LearningAgentPort.ActionTrace(1, LearningAgentPort.TraceActor.SUPERVISOR,
                    LearningAgentPort.TraceAction.SCHEMA_REPAIR, LearningAgentPort.TraceStatus.COMPLETED, 2),
                new LearningAgentPort.ActionTrace(2, LearningAgentPort.TraceActor.SUPERVISOR,
                    LearningAgentPort.TraceAction.STOP, LearningAgentPort.TraceStatus.COMPLETED, 4)),
            new LearningAgentPort.MemoryStatus(LearningAgentPort.SessionMemoryStatus.UPDATED,
                LearningAgentPort.SemanticRecallStatus.AVAILABLE,
                LearningAgentPort.SemanticPersistenceStatus.PERSISTED),
            List.of(LearningAgentPort.DegradationStatus.LONG_TERM_MEMORY_DEGRADED));
        };
        var controller = new LearningAgentController(port, null, true, properties(),
            new AgentMetrics(new SimpleMeterRegistry()));

        StudyAgentDtos.RunResponse response = controller.run(new StudyAgentDtos.RunRequest(
            "learner-1", "session-1", "question", "kb-1", null, 3, StudyAgentDtos.Difficulty.HARD));

        assertThat(response.runId()).isEqualTo("run-1");
        assertThat(response.status()).isEqualTo(StudyAgentDtos.RunStatus.COMPLETED);
        assertThat(response.nextSteps()).containsExactly("review");
        assertThat(response.citations()).singleElement().satisfies(mapped -> {
            assertThat(mapped.materialId()).isEqualTo("material-1");
            assertThat(mapped.revisionId()).isEqualTo("revision-1");
            assertThat(mapped.pageNumber()).isEqualTo(2);
            assertThat(mapped.excerpt()).isEqualTo("excerpt");
        });
        assertThat(response.practiceItems()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo("practice-1");
            assertThat(item.type()).isEqualTo(StudyAgentDtos.PracticeType.SHORT_ANSWER);
            assertThat(item.citations()).hasSize(1);
        });
        assertThat(response.memory().session()).isEqualTo(StudyAgentDtos.SessionStatus.UPDATED);
        assertThat(response.budgetUsage().maxToolCalls()).isEqualTo(8);
        assertThat(response.degradationStatuses())
            .containsExactly(StudyAgentDtos.DegradationStatus.LONG_TERM_MEMORY_DEGRADED);
        assertThat(response.actionTrace()).extracting(StudyAgentDtos.ActionTraceEntry::action)
            .containsExactly(StudyAgentDtos.TraceAction.SCHEMA_REPAIR, StudyAgentDtos.TraceAction.STOP);
        assertThat(response.actionTrace()).allSatisfy(trace ->
            assertThat(trace.actor()).isEqualTo(StudyAgentDtos.TraceActor.STUDY_SUPERVISOR));
        assertThat(StudyAgentDtos.RunResponse.class.getDeclaredFields())
            .extracting(java.lang.reflect.Field::getName)
            .doesNotContain("reasoning", "prompt", "transcript", "rawModelResponse", "learnerId");
    }

    @Test
    void mapsDisabledAndDependencyFailuresToStableSafeErrors() {
        var metrics = new AgentMetrics(new SimpleMeterRegistry());
        var disabled = new LearningAgentController(request -> null, null, false, properties(), metrics);
        assertThatThrownBy(() -> disabled.run(new StudyAgentDtos.RunRequest(
            "learner-secret", "session-secret", "question-secret", "kb-1", null, 3)))
            .isInstanceOf(AgentApiException.class)
            .hasMessage("AGENT_FEATURE_DISABLED")
            .hasMessageNotContaining("learner-secret");

        LearningAgentPort unavailable = request -> { throw new IllegalStateException("AGENT_MODEL_UNAVAILABLE: raw-secret"); };
        var enabled = new LearningAgentController(unavailable, null, true, properties(), metrics);
        assertThatThrownBy(() -> enabled.run(new StudyAgentDtos.RunRequest(
            "learner", "session", "question", "kb-1", null, 3)))
            .isInstanceOf(AgentApiException.class)
            .hasMessage("AGENT_MODEL_UNAVAILABLE")
            .hasMessageNotContaining("raw-secret");
    }

    @Test
    void defaultsMissingDifficultyToMedium() {
        LearningAgentPort port = request -> {
            assertThat(request.difficulty()).isEqualTo(LearningAgentPort.Difficulty.MEDIUM);
            assertThat(request.sessionId()).isNull();
            return LearningAgentPort.StudyRunResult.noEvidence("run", "generated-session",
                new LearningAgentPort.BudgetUsage(0, 1, 0, 1, 0, 1, 0, 90_000, false));
        };
        var controller = new LearningAgentController(port, null, true, properties(),
            new AgentMetrics(new SimpleMeterRegistry()));

        StudyAgentDtos.RunResponse response = controller.run(
            new StudyAgentDtos.RunRequest("learner", null, "question", "kb", null, null));
        assertThat(response.sessionId()).isEqualTo("generated-session");
    }

    @Test
    void preservesSuccessfulLayerCountWhenTheOtherMemoryDeletionLayerFails() {
        MemoryManager memory = mock(MemoryManager.class);
        when(memory.deleteLearnerMemory("learner"))
            .thenReturn(new MemoryDeletionResult(LayerDeletion.succeeded(2), LayerDeletion.failed()));
        var controller = new LearningAgentController(request -> null, memory, true, properties(),
            new AgentMetrics(new SimpleMeterRegistry()));

        StudyAgentDtos.MemoryDeletionResponse response = controller.deleteMemories("learner");

        assertThat(response.session().status()).isEqualTo(StudyAgentDtos.DeletionResult.DELETED);
        assertThat(response.session().deletedCount()).isEqualTo(2);
        assertThat(response.semantic().status()).isEqualTo(StudyAgentDtos.DeletionResult.FAILED);
        assertThat(response.semantic().deletedCount()).isZero();
    }

    static AgentConfigurationProperties properties() {
        return new AgentConfigurationProperties(false, 4, 3, 8, Duration.ofSeconds(90), 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }
}
