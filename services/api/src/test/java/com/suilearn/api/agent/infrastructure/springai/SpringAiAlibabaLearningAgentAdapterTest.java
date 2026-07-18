package com.suilearn.api.agent.infrastructure.springai;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import com.suilearn.api.agent.application.LearningAgentPort.Difficulty;
import com.suilearn.api.agent.application.LearningAgentPort.RunStatus;
import com.suilearn.api.agent.application.LearningAgentPort.StudyRunRequest;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.context.*;
import com.suilearn.api.agent.memory.*;
import com.suilearn.api.agent.prompt.PromptRegistry;
import com.suilearn.api.agent.tool.AgentAction;
import com.suilearn.api.agent.tool.AgentToolCatalog;
import com.suilearn.api.agent.tool.EvidencePointer;
import com.suilearn.api.agent.tool.EvidenceRecord;
import com.suilearn.api.agent.tool.KnowledgeResearchSubAgent;
import com.suilearn.api.agent.tool.PracticeCoachSubAgent;
import com.suilearn.api.agent.tool.PracticeModelPort;
import com.suilearn.api.agent.tool.TemporaryExercise;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;
import com.fasterxml.jackson.databind.ObjectMapper;

class SpringAiAlibabaLearningAgentAdapterTest {
    @Test
    void noEvidenceStopsBeforeModelAndReturnsNoUnverifiedBody() {
        ScriptedAgentModel model = new ScriptedAgentModel(false);
        var adapter = adapter(model, request -> List.of(), request -> Optional.empty(), request -> {
            throw new AssertionError("practice must not run without evidence");
        });

        var result = adapter.run(request());

        assertThat(result.status()).isEqualTo(RunStatus.NO_EVIDENCE);
        assertThat(result.answer()).isEmpty();
        assertThat(result.citations()).isEmpty();
        assertThat(result.exercises()).isEmpty();
        assertThat(result.budgetUsage().toolCalls()).isEqualTo(2);
        assertThat(model.calls()).isGreaterThan(0);
    }

    @Test
    void realAdapterUsesScopedDomainAgentsRegistryAndActualBudgetUsage() {
        ScriptedAgentModel model = new ScriptedAgentModel(true);
        var pointer = new EvidencePointer("e-1", "source-1", "kb-1", "material-1", 1.0,
            "revision-1", 2, null, "excerpt");
        var adapter = adapter(model, request -> List.of(pointer), request -> Optional.of(new EvidenceRecord(
            "e-1", "source-1", "kb-1", "material-1", "body", false,
            "revision-1", 2, null, "excerpt")), request -> new PracticeModelPort.Draft(
                "explanation", List.of(new TemporaryExercise("question", "answer", "why", List.of("source-1"))),
                List.of("source-1"), "next", AgentAction.NONE));

        var result = adapter.run(request());

        assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.answer()).isEqualTo("grounded answer");
        assertThat(result.citations()).singleElement().satisfies(citation -> {
            assertThat(citation.materialId()).isEqualTo("material-1");
            assertThat(citation.revisionId()).isEqualTo("revision-1");
            assertThat(citation.pageNumber()).isEqualTo(2);
        });
        assertThat(result.budgetUsage().supervisorSteps()).isEqualTo(1);
        assertThat(result.budgetUsage().subagentSteps()).isEqualTo(2);
        assertThat(result.budgetUsage().toolCalls()).isEqualTo(4);
        assertThat(result.actionTrace()).isNotEmpty().allMatch(trace -> trace.durationMillis() >= 0);
        assertThat(model.calls()).isGreaterThanOrEqualTo(5);
        assertThat(result.memory().session()).isEqualTo(com.suilearn.api.agent.application.LearningAgentPort.SessionMemoryStatus.UPDATED);
    }

    @Test
    void supervisorCanFinishAfterResearchWithoutForcingPractice() {
        ScriptedAgentModel model = new ScriptedAgentModel(false, true, false);
        var pointer = new EvidencePointer("e-1", "source-1", "kb-1", "material-1", 1.0,
            "revision-1", 2, null, "excerpt");
        var adapter = adapter(model, request -> List.of(pointer), request -> Optional.of(new EvidenceRecord(
            "e-1", "source-1", "kb-1", "material-1", "body", false,
            "revision-1", 2, null, "excerpt")), request -> {
                throw new AssertionError("practice was not delegated");
            });

        var result = adapter.run(request());

        assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.exercises()).isEmpty();
        assertThat(result.budgetUsage().subagentSteps()).isEqualTo(1);
        assertThat(result.budgetUsage().toolCalls()).isEqualTo(3);
    }

    @Test
    void recordsOneSchemaRepairAndDoesNotResetSharedBudget() {
        ScriptedAgentModel model = new ScriptedAgentModel(false, true, true);
        var pointer = new EvidencePointer("e-1", "source-1", "kb-1", "material-1", 1.0,
            "revision-1", 2, null, "excerpt");
        var adapter = adapter(model, request -> List.of(pointer), request -> Optional.of(new EvidenceRecord(
            "e-1", "source-1", "kb-1", "material-1", "body", false,
            "revision-1", 2, null, "excerpt")), request -> { throw new AssertionError(); });

        var result = adapter.run(request());

        assertThat(result.status()).isEqualTo(RunStatus.COMPLETED);
        assertThat(result.actionTrace()).anyMatch(trace -> trace.action()
            == com.suilearn.api.agent.application.LearningAgentPort.TraceAction.SCHEMA_REPAIR);
        assertThat(result.budgetUsage().supervisorSteps()).isEqualTo(2);
    }

    @Test
    void perRunAgentsDoNotShareMutableScopePromptsAcrossConcurrentRequests() throws Exception {
        var seen = new ConcurrentLinkedQueue<String>();
        ChatModel model = new ChatModel() {
            public ChatResponse call(Prompt prompt) {
                seen.add(prompt.getInstructions().toString());
                return response("{\"action\":\"UNCERTAIN\",\"answer\":\"uncertain\",\"citations\":[]}");
            }
            public Flux<ChatResponse> stream(Prompt prompt) { return Flux.just(call(prompt)); }
        };
        var adapter = adapter(model, request -> List.of(), request -> Optional.empty(), request -> { throw new AssertionError(); });
        var first = new StudyRunRequest("learner-1", "s1", "q1", new AgentScope("kb-A", null), 1, Difficulty.MEDIUM);
        var second = new StudyRunRequest("learner-2", "s2", "q2", new AgentScope("kb-B", null), 1, Difficulty.MEDIUM);

        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var a = executor.submit(() -> adapter.run(first));
            var b = executor.submit(() -> adapter.run(second));
            assertThat(a.get().status()).isEqualTo(RunStatus.NO_EVIDENCE);
            assertThat(b.get().status()).isEqualTo(RunStatus.NO_EVIDENCE);
        }
        assertThat(seen).anyMatch(value -> value.contains("kb-A"));
        assertThat(seen).anyMatch(value -> value.contains("kb-B"));
        assertThat(seen).noneMatch(value -> value.contains("kb-A") && value.contains("kb-B"));
    }

    @Test
    void repairUsesRemainingDeadlineAndCancelsBlockingSecondCall() {
        MutableClock clock = new MutableClock(Instant.parse("2026-07-18T00:00:00Z"));
        AtomicInteger calls = new AtomicInteger();
        ChatModel model = prompt -> {
            if (calls.getAndIncrement() == 0) {
                clock.advance(Duration.ofMillis(9_900));
                return response("invalid-json");
            }
            try { Thread.sleep(1_000); } catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
            return response("{\"action\":\"UNCERTAIN\",\"answer\":\"uncertain\",\"citations\":[]}");
        };
        var adapter = adapter(model, request -> List.of(), request -> Optional.empty(),
            request -> { throw new AssertionError(); }, properties(8, Duration.ofSeconds(10)), clock);

        var result = adapter.run(request());

        assertThat(result.status()).isEqualTo(RunStatus.BUDGET_EXHAUSTED);
        assertThat(result.budgetUsage().timedOut()).isTrue();
        assertThat(result.budgetUsage().supervisorSteps()).isEqualTo(2);
    }

    @Test
    void frameworkDelegationAndDomainToolsShareOneToolCallBudget() {
        ScriptedAgentModel model = new ScriptedAgentModel(true);
        var pointer = new EvidencePointer("e-1", "source-1", "kb-1", "material-1", 1.0,
            "revision-1", 2, null, "excerpt");
        var adapter = adapter(model, request -> List.of(pointer), request -> Optional.empty(),
            request -> { throw new AssertionError(); }, properties(1, Duration.ofSeconds(10)), Clock.systemUTC());

        var result = adapter.run(request());

        assertThat(result.status()).isEqualTo(RunStatus.BUDGET_EXHAUSTED);
        assertThat(result.budgetUsage().toolCalls()).isEqualTo(1);
    }

    private SpringAiAlibabaLearningAgentAdapter adapter(
            ChatModel model,
            com.suilearn.api.agent.tool.EvidenceSearchPort search,
            com.suilearn.api.agent.tool.EvidenceReadPort read,
            PracticeModelPort practice) {
        return adapter(model, search, read, practice, properties(), Clock.systemUTC());
    }

    private SpringAiAlibabaLearningAgentAdapter adapter(
            ChatModel model,
            com.suilearn.api.agent.tool.EvidenceSearchPort search,
            com.suilearn.api.agent.tool.EvidenceReadPort read,
            PracticeModelPort practice,
            AgentConfigurationProperties properties,
            Clock clock) {
        var catalog = AgentToolCatalog.fixedMvp();
        SessionMemoryStore sessionStore = new SessionMemoryStore() {
            private SessionMemory value;
            public Optional<SessionMemory> read(String key, Duration ttl) { return Optional.ofNullable(value); }
            public void write(String key, SessionMemory memory, Duration ttl) { value = memory; }
            public long deleteByPrefix(String prefix) { value = null; return 0; }
        };
        var sessions = new SessionMemoryService(sessionStore,
            new SessionMemoryKeyFactory("suilearn:agent:session:v1"), Duration.ofHours(24), 20);
        SemanticMemoryStore semantic = new SemanticMemoryStore() {
            public List<AgentSemanticMemory> findByLearnerAndTypes(String learner, java.util.Set<MemoryType> types) { return List.of(); }
            public List<ScoredSemanticMemory> recall(SemanticMemoryQuery query, List<Double> vector, int topK) { return List.of(); }
            public AgentSemanticMemory save(AgentSemanticMemory memory) { return memory; }
            public long deleteByLearner(String learner) { return 0; }
        };
        var memory = new MemoryManager(sessions, semantic, content -> EmbeddingResult.available(List.of(1.0)),
            new MemoryPromotionPolicy(0.8, 1, 2000), 5, clock::instant);
        var context = new ContextManager(new ContextAssembler(TokenEstimator.conservativeCharacters(),
            new ContextBudgetPolicy()), 12000);
        return new SpringAiAlibabaLearningAgentAdapter(
            model, new KnowledgeResearchSubAgent(search, read, catalog),
            new PracticeCoachSubAgent(practice, catalog),
            new PromptRegistry(new SpringAiPromptTemplateRenderer()), properties, clock,
            AgentRuntimeReadiness.noOp(), context, memory, request -> Optional.empty(), new ObjectMapper());
    }

    private StudyRunRequest request() {
        return new StudyRunRequest("learner-1", "session-1", "learn hooks",
            new AgentScope("kb-1", null), 1, Difficulty.MEDIUM);
    }

    private AgentConfigurationProperties properties() {
        return properties(8, Duration.ofSeconds(90));
    }

    private AgentConfigurationProperties properties(int maxToolCalls, Duration timeout) {
        return new AgentConfigurationProperties(true, 4, 3, maxToolCalls, timeout, 12000, 3,
            new AgentConfigurationProperties.Session(Duration.ofHours(24), 20),
            new AgentConfigurationProperties.Memory(5, 0.8));
    }

    private static final class MutableClock extends Clock {
        private Instant instant;
        private MutableClock(Instant instant) { this.instant = instant; }
        void advance(Duration duration) { instant = instant.plus(duration); }
        public ZoneId getZone() { return ZoneId.of("UTC"); }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return instant; }
    }

    private ChatResponse response(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    private ChatResponse toolCall(String name) {
        String arguments = name.equals("researchScopedEvidence") || name.equals("createGroundedPractice")
            ? "\"run\"" : "{\"input\":\"run\"}";
        AssistantMessage message = AssistantMessage.builder().content("")
            .toolCalls(List.of(new AssistantMessage.ToolCall(UUID.randomUUID().toString(), "function", name, arguments)))
            .build();
        return new ChatResponse(List.of(new Generation(message)));
    }

    private final class ScriptedAgentModel implements ChatModel {
        private final boolean includePractice;
        private final boolean answerWithoutPractice;
        private final boolean repairFirst;
        private final AtomicInteger calls = new AtomicInteger();
        private boolean researchDelegated;
        private boolean practiceDelegated;
        private int finalResponses;

        private ScriptedAgentModel(boolean includePractice) { this(includePractice, false, false); }
        private ScriptedAgentModel(boolean includePractice, boolean answerWithoutPractice, boolean repairFirst) {
            this.includePractice = includePractice;
            this.answerWithoutPractice = answerWithoutPractice;
            this.repairFirst = repairFirst;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            calls.incrementAndGet();
            List<ToolCallback> callbacks = prompt.getOptions() instanceof ToolCallingChatOptions options
                ? options.getToolCallbacks() : List.of();
            List<String> names = callbacks.stream().map(callback -> callback.getToolDefinition().name()).toList();
            boolean hasToolResponse = prompt.getInstructions().stream()
                .anyMatch(message -> message.getClass().getSimpleName().equals("ToolResponseMessage"));
            if (names.contains("researchScopedEvidence")) {
                return hasToolResponse ? response("research complete") : toolCall("researchScopedEvidence");
            }
            if (names.contains("createGroundedPractice")) {
                return hasToolResponse ? response("practice complete") : toolCall("createGroundedPractice");
            }
            String researchName = names.stream().filter(name -> name.contains("knowledge")).findFirst().orElse(null);
            String practiceName = names.stream().filter(name -> name.contains("practice")).findFirst().orElse(null);
            if (!researchDelegated && researchName != null) {
                researchDelegated = true;
                return toolCall(researchName);
            }
            if (includePractice && !practiceDelegated && practiceName != null) {
                practiceDelegated = true;
                return toolCall(practiceName);
            }
            if (repairFirst && finalResponses++ == 0) return response("invalid-json");
            return includePractice || answerWithoutPractice
                ? response("{\"action\":\"ANSWER\",\"answer\":\"grounded answer\",\"citations\":[\"source-1\"]}")
                : response("{\"action\":\"UNCERTAIN\",\"answer\":\"uncertain\",\"citations\":[]}");
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }

        int calls() { return calls.get(); }
    }
}
