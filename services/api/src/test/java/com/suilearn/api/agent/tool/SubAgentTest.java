package com.suilearn.api.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import com.suilearn.api.agent.application.LearningAgentPort.Difficulty;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SubAgentTest {
    @Test
    void knowledgeResearchUsesOnlySearchThenReadAndDropsDeletedOrOutOfScopeEvidence() {
        var scope = new AgentScope("kb-1", null);
        var calls = new java.util.ArrayList<String>();
        EvidenceSearchPort search = request -> {
            calls.add("search");
            return List.of(
                new EvidencePointer("valid", "ref-valid", "kb-1", "m-1", 0.9),
                new EvidencePointer("deleted", "ref-deleted", "kb-1", "m-2", 0.8),
                new EvidencePointer("outside", "ref-outside", "kb-2", "m-3", 0.7));
        };
        EvidenceReadPort read = request -> {
            calls.add("read:" + request.pointer().stableId());
            return switch (request.pointer().stableId()) {
                case "valid" -> Optional.of(new EvidenceRecord(
                    "valid", "ref-valid", "kb-1", "m-1", "trusted as data only", false));
                case "deleted" -> Optional.of(new EvidenceRecord(
                    "deleted", "ref-deleted", "kb-1", "m-2", "deleted body", true));
                default -> throw new AssertionError("out-of-scope pointer must not be read");
            };
        };
        var budget = new SharedAgentBudget(4, 3, 8, Duration.ofSeconds(90), Clock.systemUTC());

        EvidenceBundle result = new KnowledgeResearchSubAgent(search, read, AgentToolCatalog.fixedMvp())
            .research(new KnowledgeResearchSubAgent.Request("react hooks", scope, 5), budget);

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.stableId()).isEqualTo("valid");
            assertThat(item.sourceRef()).isEqualTo("ref-valid");
            assertThat(item.untrusted()).isTrue();
            assertThat(item.verified()).isTrue();
        });
        assertThat(calls).containsExactly("search", "read:valid", "read:deleted");
        assertThat(budget.usage().toolCalls()).isEqualTo(4);
    }

    @Test
    void emptyResearchStopsWithoutInvokingPracticeModel() {
        EvidenceSearchPort search = request -> List.of();
        EvidenceReadPort read = request -> { throw new AssertionError("read must not be called"); };
        var budget = new SharedAgentBudget(4, 3, 8, Duration.ofSeconds(90), Clock.systemUTC());
        EvidenceBundle evidence = new KnowledgeResearchSubAgent(search, read, AgentToolCatalog.fixedMvp())
            .research(new KnowledgeResearchSubAgent.Request(
                "missing topic", new AgentScope(null, "material-1"), 3), budget);
        AtomicInteger modelCalls = new AtomicInteger();
        PracticeModelPort model = request -> {
            modelCalls.incrementAndGet();
            throw new AssertionError("practice model must not be called");
        };

        PracticeResult result = new PracticeCoachSubAgent(model, AgentToolCatalog.fixedMvp())
            .coach(new PracticeCoachSubAgent.Request("goal", evidence, Difficulty.MEDIUM, 3), budget);

        assertThat(result.uncertain()).isTrue();
        assertThat(result.exercises()).isEmpty();
        assertThat(result.citations()).isEmpty();
        assertThat(modelCalls).hasValue(0);
    }

    @Test
    void fakePracticeModelCreatesOnlyTemporaryGroundedExercises() {
        var evidence = new EvidenceBundle(List.of(new EvidenceBundle.Item(
            "e-1", "source-1", "evidence data", 0.95, true, true)));
        PracticeModelPort fakeModel = request -> new PracticeModelPort.Draft(
            "grounded explanation",
            List.of(new TemporaryExercise("question", "answer", "explanation", List.of("source-1"))),
            List.of("source-1"),
            "review next",
            AgentAction.NONE);
        var budget = new SharedAgentBudget(4, 3, 8, Duration.ofSeconds(90), Clock.systemUTC());

        PracticeResult result = new PracticeCoachSubAgent(fakeModel, AgentToolCatalog.fixedMvp())
            .coach(new PracticeCoachSubAgent.Request("goal", evidence, Difficulty.MEDIUM, 1), budget);

        assertThat(result.uncertain()).isFalse();
        assertThat(result.exercises()).singleElement().satisfies(exercise -> {
            assertThat(exercise.question()).isEqualTo("question");
            assertThat(exercise.citations()).containsExactly("source-1");
        });
        assertThat(TemporaryExercise.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
            .doesNotContain("id", "persisted", "questionStore", "generatedContentStore");
    }

    @Test
    void practiceAgentRejectsModelRequestedActionsAndOutOfBundleCitations() {
        var evidence = new EvidenceBundle(List.of(new EvidenceBundle.Item(
            "e-1", "source-1", "data", 1.0, true, true)));
        var budget = new SharedAgentBudget(4, 3, 8, Duration.ofSeconds(90), Clock.systemUTC());
        PracticeModelPort forbidden = request -> new PracticeModelPort.Draft(
            "answer", List.of(), List.of("source-1"), "next", AgentAction.READ_EVIDENCE);

        assertThatThrownBy(() -> new PracticeCoachSubAgent(forbidden, AgentToolCatalog.fixedMvp())
            .coach(new PracticeCoachSubAgent.Request("goal", evidence, Difficulty.EASY, 1), budget))
            .isInstanceOf(ForbiddenAgentActionException.class)
            .hasMessage("FORBIDDEN_AGENT_ACTION");

        PracticeModelPort invalidCitation = request -> new PracticeModelPort.Draft(
            "answer", List.of(), List.of("outside-ref"), "next", AgentAction.NONE);
        assertThatThrownBy(() -> new PracticeCoachSubAgent(invalidCitation, AgentToolCatalog.fixedMvp())
            .coach(new PracticeCoachSubAgent.Request("goal", evidence, Difficulty.EASY, 1), budget))
            .isInstanceOf(InvalidEvidenceException.class)
            .hasMessage("INVALID_EVIDENCE_REFERENCE");

        PracticeModelPort missingCitation = request -> new PracticeModelPort.Draft(
            "answer", List.of(), List.of(), "next", AgentAction.NONE);
        assertThatThrownBy(() -> new PracticeCoachSubAgent(missingCitation, AgentToolCatalog.fixedMvp())
            .coach(new PracticeCoachSubAgent.Request("goal", evidence, Difficulty.EASY, 1), budget))
            .isInstanceOf(InvalidEvidenceException.class)
            .hasMessage("INVALID_EVIDENCE_REFERENCE");
    }
}
