package com.suilearn.api.agent.context;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.context.AgentContextRequest.Candidate;
import com.suilearn.api.agent.context.AgentContextSnapshot.TrimReason;
import com.suilearn.api.agent.context.AgentContextSnapshot.Trust;
import java.util.List;
import org.junit.jupiter.api.Test;

class AgentContextManagerTest {
    private static final TokenEstimator TEST_ESTIMATOR = String::length;

    @Test
    void trimsInFixedPriorityWhileKeepingImmutableSystemTaskAndScope() {
        var assembler = new ContextAssembler(TEST_ESTIMATOR, new ContextBudgetPolicy());
        var request = new AgentContextRequest(
            "S", "T", "P",
            List.of(new EvidenceItem("e-1", "source-1", "EEEE", 0.1, true)),
            List.of(new Candidate("s-1", "SSSS", 0.1, 1)),
            List.of(new Candidate("m-1", "MMMM", 0.1, 1)),
            List.of(new Candidate("o-1", "OOOO", 0.1, 1)));

        AgentContextSnapshot snapshot = assembler.assemble(request, 7);

        assertThat(snapshot.systemContract()).isEqualTo("S");
        assertThat(snapshot.currentTask()).isEqualTo("T");
        assertThat(snapshot.scope()).isEqualTo("P");
        assertThat(snapshot.supplemental()).extracting(AgentContextSnapshot.Entry::source)
            .containsExactly(ContextSource.EVIDENCE);
        assertThat(snapshot.trimming()).extracting(AgentContextSnapshot.TrimEvent::source)
            .containsExactly(ContextSource.OBSERVATION, ContextSource.SEMANTIC_MEMORY,
                ContextSource.SESSION_SUMMARY);
        assertThat(snapshot.estimatedTokens()).isEqualTo(7);
    }

    @Test
    void deduplicatesBySourceAndStableIdAndKeepsExternalContentUntrusted() {
        String discardedSecret = "discarded-sensitive-body";
        var request = new AgentContextRequest(
            "system", "task", "scope",
            List.of(
                new EvidenceItem("same", "old-ref", discardedSecret, 0.2, true),
                new EvidenceItem("same", "new-ref", "retained", 0.9, true)),
            List.of(new Candidate("same", "session-data", 1.0, 1)),
            List.of(), List.of());

        AgentContextSnapshot snapshot = new ContextAssembler(TEST_ESTIMATOR, new ContextBudgetPolicy())
            .assemble(request, 1000);

        assertThat(snapshot.supplemental()).hasSize(2);
        assertThat(snapshot.supplemental()).allMatch(entry -> entry.trust() == Trust.UNTRUSTED_DATA);
        assertThat(snapshot.supplemental()).filteredOn(entry -> entry.source() == ContextSource.EVIDENCE)
            .singleElement().extracting(AgentContextSnapshot.Entry::sourceRef).isEqualTo("new-ref");
        assertThat(snapshot.trimming()).singleElement()
            .extracting(AgentContextSnapshot.TrimEvent::reason).isEqualTo(TrimReason.DUPLICATE_STABLE_ID);
        assertThat(snapshot.trimming().toString()).doesNotContain(discardedSecret, "old-ref", "same");
    }

    @Test
    void failsRatherThanTrimmingMandatoryContext() {
        var request = new AgentContextRequest("system", "task", "scope", List.of(), List.of(), List.of(), List.of());

        assertThatThrownBy(() -> new ContextAssembler(TEST_ESTIMATOR, new ContextBudgetPolicy())
            .assemble(request, 5))
            .isInstanceOf(ContextBudgetPolicy.ContextBudgetExceededException.class)
            .hasMessageNotContaining("system")
            .hasMessageNotContaining("task")
            .hasMessageNotContaining("scope");
    }

    @Test
    void excludesUnverifiedEvidenceFromTheAssembledContext() {
        var request = new AgentContextRequest("system", "task", "scope",
            List.of(new EvidenceItem("unverified", "source", "must-not-enter", 1.0, false)),
            List.of(), List.of(), List.of());

        AgentContextSnapshot snapshot = new ContextAssembler(TEST_ESTIMATOR, new ContextBudgetPolicy())
            .assemble(request, 100);

        assertThat(snapshot.supplemental()).isEmpty();
        assertThat(snapshot.toString()).doesNotContain("must-not-enter");
    }

    @Test
    void createsOnlyMinimumSubagentSnapshotsAndRejectsUnverifiedPracticeEvidence() {
        var manager = new ContextManager(new ContextAssembler(TEST_ESTIMATOR, new ContextBudgetPolicy()), 1000);
        var research = manager.forKnowledgeResearch("goal", "kb:1", List.of("weakness-summary"), 3, 4);

        assertThat(research.researchGoal()).isEqualTo("goal");
        assertThat(research.scope()).isEqualTo("kb:1");
        assertThat(research.evidenceIsUntrustedData()).isTrue();
        assertThat(research.toString()).doesNotContain("credential", "fullSession");

        var verified = new EvidenceItem("e-1", "source-1", "data", 1.0, true);
        assertThat(manager.forPracticeCoach("goal", List.of(verified), "MEDIUM", 3, "schema")
            .verifiedEvidence()).containsExactly(verified);
        assertThatThrownBy(() -> manager.forPracticeCoach("goal",
            List.of(new EvidenceItem("e-2", "source-2", "data", 1.0, false)), "MEDIUM", 3, "schema"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("practice context accepts verified evidence only");
    }
}
