package com.suilearn.api.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.application.LearningAgentPort.Difficulty;
import com.suilearn.api.agent.memory.AgentSemanticMemory;
import com.suilearn.api.agent.memory.EmbeddingResult;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.memory.MemoryPromotionPolicy;
import com.suilearn.api.agent.memory.MemoryType;
import com.suilearn.api.agent.memory.SemanticMemoryQuery;
import com.suilearn.api.agent.memory.SemanticMemoryStore;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.agent.runtime.TurnContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AgentDeclarativeToolsTest {
    @Test
    void searchAndReadReuseScopeAndDeletedValidation() {
        var search = new SearchKnowledgeTool(request -> {
            assertThat(request.scope().knowledgeBaseId()).isEqualTo("kb-1");
            return List.of(new EvidencePointer("stable-1", "ref-1", "kb-1", "mat-1", 0.9d));
        });
        var searchResult = search.execute(context(), Map.of("query", "java"));
        assertThat(searchResult.success()).isTrue();
        assertThat(searchResult.sources()).singleElement().satisfies(source ->
            assertThat(source.stableId()).isEqualTo("stable-1"));
        assertThat(searchResult.metadata()).containsEntry("count", 1);

        var read = new ReadEvidenceTool(request -> Optional.of(
            new EvidenceRecord("stable-1", "ref-1", "kb-1", "mat-1", "verified content", false,
                "rev-1", 3, "block-1", "excerpt")));
        var readResult = read.execute(context(), Map.of("stableId", "stable-1", "sourceRef", "ref-1"));
        assertThat(readResult.success()).isTrue();
        assertThat(readResult.content()).isEqualTo("verified content");

        var deletedRead = new ReadEvidenceTool(request -> Optional.of(
            new EvidenceRecord("stable-1", "ref-1", "kb-1", "mat-1", "gone", true)));
        var deletedResult = deletedRead.execute(context(), Map.of("stableId", "stable-1"));
        assertThat(deletedResult.success()).isFalse();
        assertThat(deletedResult.metadata()).containsEntry("code", "EVIDENCE_NOT_FOUND");
    }

    @Test
    void generatePracticeIsTemporaryAndDegradesWithoutModel() {
        var unavailable = new GeneratePracticeTool(null);
        var unavailableResult = unavailable.execute(context(), Map.of("learningGoal", "java",
            "evidence", List.of(evidence())));
        assertThat(unavailableResult.success()).isFalse();
        assertThat(unavailableResult.metadata()).containsEntry("code", "AGENT_MODEL_UNAVAILABLE");

        var model = (PracticeModelPort) request -> new PracticeModelPort.Draft(
            "explanation", List.of(new TemporaryExercise("q", "a", "why", List.of("ref-1"))),
            List.of("ref-1"), "next", AgentAction.NONE);
        var coach = new PracticeCoachSubAgent(model, AgentToolCatalog.fixedMvp(), com.suilearn.api.agent.metrics.AgentMetrics.noop());
        var tool = new GeneratePracticeTool(coach);

        var result = tool.execute(context(), Map.of("learningGoal", "java", "difficulty", "EASY",
            "practiceCount", 1, "evidence", List.of(evidence())));

        assertThat(result.success()).isTrue();
        assertThat(result.metadata()).containsEntry("uncertain", false);
        assertThat(result.content()).isEqualTo("explanation");
        assertThat(result.sources()).extracting(ToolCitation::sourceRef).containsExactly("ref-1");
    }

    @Test
    void memoryToolsUsePromotionPolicyAndAskUserPauses() {
        var memory = memoryManager();
        var recall = new RecallMemoryTool(memory);
        var recallResult = recall.execute(context(), Map.of("query", "goal"));
        assertThat(recallResult.success()).isTrue();
        assertThat(recallResult.metadata()).containsEntry("status", "AVAILABLE");

        var persist = new PersistMemoryTool(memory);
        var persistResult = persist.execute(context(), Map.of(
            "memoryType", "WEAKNESS", "content", "lambda", "confidence", 0.9, "sourceRef", "ref-1"));
        assertThat(persistResult.success()).isTrue();
        assertThat(persistResult.metadata()).containsEntry("status", "PERSISTED");

        var ask = new AskUserTool();
        var askResult = ask.execute(context(), Map.of("questionId", "q1", "prompt", "Which level?",
            "options", List.of(Map.of("id", "easy", "label", "Easy"))));
        assertThat(askResult.success()).isFalse();
        assertThat(askResult.pauseForUser().questionId()).isEqualTo("q1");
        assertThat(askResult.pauseForUser().options()).hasSize(1);
    }

    private MemoryManager memoryManager() {
        var semanticStore = new SemanticMemoryStore() {
            private final java.util.Map<String, AgentSemanticMemory> memories = new java.util.concurrent.ConcurrentHashMap<>();

            @Override public java.util.List<AgentSemanticMemory> findByLearnerAndTypes(String learnerId, java.util.Set<com.suilearn.api.agent.memory.MemoryType> types) {
                return memories.values().stream().filter(memory -> memory.learnerId().equals(learnerId)
                    && types.contains(memory.memoryType())).toList();
            }
            @Override public java.util.List<com.suilearn.api.agent.memory.ScoredSemanticMemory> recall(SemanticMemoryQuery query, java.util.List<Double> embedding, int topK) {
                return java.util.List.of();
            }
            @Override public AgentSemanticMemory save(AgentSemanticMemory memory) {
                memories.put(memory.id(), memory);
                return memory;
            }
            @Override public long deleteByLearner(String learnerId) { return 0; }
        };
        return new MemoryManager(null, semanticStore,
            content -> EmbeddingResult.available(java.util.List.of(1.0d)),
            new MemoryPromotionPolicy(0.8d, 1, 500), 5, java.time.Instant::now);
    }

    private TurnContext context() {
        return new TurnContext("turn-1", "sess-1", "learner-1", "study_agent",
            new StudyScope("kb-1", null), List.of(), "question", List.of(), List.of(), Map.of());
    }

    private Map<String, Object> evidence() {
        return Map.of("stableId", "stable-1", "sourceRef", "ref-1", "content", "evidence", "relevance", 0.8d);
    }
}
