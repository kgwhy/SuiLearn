package com.suilearn.api.agent.infrastructure.springai;

import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.application.MemoryCandidateExtractor;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.context.AgentContextRequest;
import com.suilearn.api.agent.context.ContextManager;
import com.suilearn.api.agent.context.EvidenceItem;
import com.suilearn.api.agent.memory.*;
import com.suilearn.api.agent.prompt.*;
import com.suilearn.api.agent.tool.*;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

/** Fixed Supervisor + two request-bound Agent-as-Tool specialists. */
public final class SpringAiAlibabaLearningAgentAdapter implements LearningAgentPort {
    private static final String OUTPUT_SCHEMA =
        "action is ANSWER, UNCERTAIN, or PARTIAL; answer is text; citations is a list of source references";

    private final KnowledgeResearchSubAgent knowledgeResearch;
    private final PracticeCoachSubAgent practiceCoach;
    private final PromptRegistry prompts;
    private final AgentConfigurationProperties properties;
    private final Clock clock;
    private final AgentRuntimeReadiness readiness;
    private final ContextManager contextManager;
    private final MemoryManager memoryManager;
    private final MemoryCandidateExtractor candidateExtractor;
    private final ObjectMapper objectMapper;
    private final ChatModel chatModel;

    public SpringAiAlibabaLearningAgentAdapter(
            ChatModel chatModel, KnowledgeResearchSubAgent knowledgeResearch,
            PracticeCoachSubAgent practiceCoach, PromptRegistry prompts,
            AgentConfigurationProperties properties, Clock clock,
            AgentRuntimeReadiness readiness, ContextManager contextManager,
            MemoryManager memoryManager, MemoryCandidateExtractor candidateExtractor,
            ObjectMapper objectMapper) {
        this.knowledgeResearch = Objects.requireNonNull(knowledgeResearch);
        this.practiceCoach = Objects.requireNonNull(practiceCoach);
        this.prompts = Objects.requireNonNull(prompts);
        this.properties = Objects.requireNonNull(properties);
        this.clock = Objects.requireNonNull(clock);
        this.readiness = Objects.requireNonNull(readiness);
        this.contextManager = Objects.requireNonNull(contextManager);
        this.memoryManager = Objects.requireNonNull(memoryManager);
        this.candidateExtractor = Objects.requireNonNull(candidateExtractor);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.chatModel = Objects.requireNonNull(chatModel);
    }

    @Override
    public StudyRunResult run(StudyRunRequest request) {
        readiness.requireAvailable();
        Execution execution = new Execution(request, properties, clock);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<StudyRunResult> future = executor.submit(() -> execute(execution));
        try {
            return future.get(properties.runTimeout().toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            execution.budget.markTimedOut();
            execution.trace.add(execution.trace(TraceActor.SUPERVISOR, TraceAction.STOP,
                TraceStatus.EXHAUSTED, execution.startedNanos));
            return execution.result("", true, List.of(), List.of(), "", RunStatus.BUDGET_EXHAUSTED,
                MemoryStatus.notAttempted(), List.of());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AGENT_DEPENDENCY_UNAVAILABLE");
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            throw new IllegalStateException("AGENT_DEPENDENCY_UNAVAILABLE");
        } finally {
            executor.shutdownNow();
        }
    }

    private StudyRunResult execute(Execution execution) {
        try (WorkingMemory working = memoryManager.openWorkingMemory()) {
            working.put("runId", execution.runId);
            Optional<SessionMemory> session;
            try {
                session = memoryManager.readSession(execution.request.learnerId(), execution.sessionId);
            } catch (RuntimeException exception) {
                throw new IllegalStateException("AGENT_SESSION_MEMORY_UNAVAILABLE");
            }
            SemanticRecallResult semantic = memoryManager.recall(execution.request.learnerId(),
                MemoryType.allowed(), execution.request.question());
            var sessionCandidates = new ArrayList<AgentContextRequest.Candidate>();
            session.ifPresent(value -> {
                long sequence = 0;
                for (SessionTurn turn : value.turns()) {
                    sessionCandidates.add(new AgentContextRequest.Candidate("session-" + (++sequence),
                        turn.summary(), 1.0, sequence));
                }
            });
            var semanticCandidates = semantic.memories().stream().map(item -> new AgentContextRequest.Candidate(
                item.memory().id(), item.memory().content(), item.score(),
                Math.max(0, item.memory().updatedAt().toEpochMilli()))).toList();
            var snapshot = contextManager.assemble(new AgentContextRequest(
                "fixed-agent-safety-contract", execution.request.question(), scope(execution.request), List.of(),
                sessionCandidates, semanticCandidates, List.of()));
            execution.context = snapshot.supplemental().stream().map(entry -> entry.source() + ":" + entry.content())
                .collect(Collectors.joining("\n"));

            AgentTopology topology = topology(execution);
            StructuredOutputProcessor<StructuredAgentOutput> processor = new StructuredOutputProcessor<>(
                this::decodeOutput,
                output -> new StructuredAgentOutputValidator(execution.citationMap().keySet(), 20_000).validate(output),
                (invalid, reasons) -> callSupervisor(execution, topology.supervisor(),
                    "schema validation: " + String.join(",", reasons)));
            StructuredOutputProcessor.Result<StructuredAgentOutput> processed;
            try {
                processed = processor.process(callSupervisor(execution, topology.supervisor(),
                    execution.request.question()));
            } catch (SharedAgentBudget.BudgetExhaustedException exception) {
                return execution.result("", true, List.of(), List.of(), "", RunStatus.BUDGET_EXHAUSTED,
                    MemoryStatus.notAttempted(), List.of());
            } catch (StructuredOutputProcessor.InvalidModelOutputException exception) {
                return execution.result("", true, List.of(), List.of(), "", RunStatus.INVALID_MODEL_OUTPUT,
                    MemoryStatus.notAttempted(), List.of());
            }
            if (processed.repairCount() > 0) {
                execution.trace.add(execution.trace(TraceActor.SUPERVISOR, TraceAction.SCHEMA_REPAIR,
                    TraceStatus.COMPLETED, execution.startedNanos));
            }
            StructuredAgentOutput output = processed.value();
            Map<String, Citation> citationMap = execution.citationMap();
            if (execution.evidence.items().isEmpty() || output.action() == StructuredAgentOutput.Action.UNCERTAIN) {
                return finishMemory(execution, "", true, List.of(), List.of(), "", RunStatus.NO_EVIDENCE,
                    semantic);
            }
            if (citationMap.size() != execution.evidence.items().size()) {
                return execution.result("", true, List.of(), List.of(), "", RunStatus.INVALID_MODEL_OUTPUT,
                    MemoryStatus.notAttempted(), List.of());
            }
            List<Citation> citations = output.citations().stream().map(citationMap::get).toList();
            List<Exercise> exercises = exercises(execution, citationMap);
            String next = execution.practice == null ? "" : execution.practice.nextStep();
            return finishMemory(execution, output.answer(), false, citations, exercises, next,
                RunStatus.COMPLETED, semantic);
        }
    }

    private StudyRunResult finishMemory(Execution execution, String answer, boolean uncertain,
                                        List<Citation> citations, List<Exercise> exercises, String next,
                                        RunStatus status, SemanticRecallResult semantic) {
        try {
            memoryManager.appendSession(execution.request.learnerId(), execution.sessionId,
                new SessionTurn("study run " + status.name().toLowerCase(Locale.ROOT), next, clock.instant()));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("AGENT_SESSION_MEMORY_UNAVAILABLE");
        }
        MemoryPersistenceResult persistence;
        Optional<MemoryCandidate> candidate = Optional.empty();
        if (status == RunStatus.COMPLETED && !citations.isEmpty()) {
            candidate = candidateExtractor.extract(new MemoryCandidateExtractor.Request(
                execution.request.learnerId(), execution.runId, answer,
                citations.stream().map(Citation::sourceRef).toList(), execution.remaining()));
        }
        persistence = memoryManager.promote(execution.request.learnerId(), candidate.orElse(null));
        SemanticPersistenceStatus persistenceStatus = switch (persistence.status()) {
            case PERSISTED -> SemanticPersistenceStatus.PERSISTED;
            case NO_CANDIDATE, REJECTED -> SemanticPersistenceStatus.NO_CANDIDATE;
            case PERSIST_FAILED -> SemanticPersistenceStatus.PERSIST_FAILED;
        };
        var degradation = new ArrayList<DegradationStatus>();
        if (semantic.status() == RecallStatus.LONG_TERM_MEMORY_DEGRADED)
            degradation.add(DegradationStatus.LONG_TERM_MEMORY_DEGRADED);
        if (persistence.status() == PersistenceStatus.PERSIST_FAILED)
            degradation.add(DegradationStatus.MEMORY_PERSIST_FAILED);
        MemoryStatus memory = new MemoryStatus(SessionMemoryStatus.UPDATED,
            semantic.status() == RecallStatus.LONG_TERM_MEMORY_DEGRADED
                ? SemanticRecallStatus.LONG_TERM_MEMORY_DEGRADED
                : semantic.memories().isEmpty() ? SemanticRecallStatus.EMPTY : SemanticRecallStatus.AVAILABLE,
            persistenceStatus);
        return execution.result(answer, uncertain, citations, exercises, next, status, memory, degradation);
    }

    private String researchBound(Execution execution) {
        long started = System.nanoTime();
        try {
            execution.evidence = knowledgeResearch.research(new KnowledgeResearchSubAgent.Request(
                execution.request.question(), execution.request.scope(), 5), execution.budget);
        } catch (SharedAgentBudget.BudgetExhaustedException exception) {
            execution.budgetFailure = true;
            throw exception;
        }
        execution.trace.add(execution.trace(TraceActor.KNOWLEDGE_RESEARCH, TraceAction.KNOWLEDGE_RESEARCH,
            TraceStatus.COMPLETED, started));
        return json(execution.evidence);
    }

    private String practiceBound(Execution execution) {
        long started = System.nanoTime();
        try {
            execution.practice = practiceCoach.coach(new PracticeCoachSubAgent.Request(
                execution.request.question(), execution.evidence, execution.request.difficulty(),
                execution.request.practiceCount()), execution.budget);
        } catch (SharedAgentBudget.BudgetExhaustedException exception) {
            execution.budgetFailure = true;
            throw exception;
        }
        execution.trace.add(execution.trace(TraceActor.PRACTICE_COACH, TraceAction.PRACTICE_COACH,
            TraceStatus.COMPLETED, started));
        return json(execution.practice);
    }

    private String callSupervisor(Execution execution, ReactAgent supervisor, String input) {
        execution.budget.consumeStep(AgentRole.SUPERVISOR);
        Duration remaining = execution.remaining();
        if (remaining.isZero() || remaining.isNegative()) {
            execution.budget.markTimedOut();
            throw new SharedAgentBudget.BudgetExhaustedException();
        }
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<AssistantMessage> future = executor.submit(() -> supervisor.call(input));
        try {
            AssistantMessage response = future.get(remaining.toMillis(), TimeUnit.MILLISECONDS);
            if (execution.budgetFailure) throw new SharedAgentBudget.BudgetExhaustedException();
            execution.budget.checkTime();
            return response.getText() == null ? "" : response.getText();
        } catch (TimeoutException exception) {
            future.cancel(true);
            execution.budget.markTimedOut();
            throw new SharedAgentBudget.BudgetExhaustedException();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new SharedAgentBudget.BudgetExhaustedException();
        } catch (ExecutionException exception) {
            if (hasCause(exception, SharedAgentBudget.BudgetExhaustedException.class)) {
                throw new SharedAgentBudget.BudgetExhaustedException();
            }
            throw new IllegalStateException("AGENT_MODEL_UNAVAILABLE");
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (type.isInstance(current)) return true;
        }
        return false;
    }

    private AgentTopology topology(Execution execution) {
        String scope = scope(execution.request);
        String researchInstruction = prompts.render("knowledge-research", "v1",
            new PromptVariables.KnowledgeResearch(execution.request.question(), scope, execution.context)).content();
        String practiceInstruction = prompts.render("practice-coach", "v1",
            new PromptVariables.PracticeCoach(execution.request.question(), "request-bound verified evidence",
                execution.request.difficulty().name(), Integer.toString(execution.request.practiceCount()),
                OUTPUT_SCHEMA)).content();
        String supervisorInstruction = prompts.render("supervisor", "v1",
            new PromptVariables.Supervisor(execution.request.question(), scope, execution.context)).content();
        ToolCallback researchTool = FunctionToolCallback.builder("researchScopedEvidence",
            (String ignored) -> researchBound(execution)).description("Search bound scope")
            .inputType(String.class).build();
        ToolCallback practiceTool = FunctionToolCallback.builder("createGroundedPractice",
            (String ignored) -> practiceBound(execution)).description("Practice from bound evidence")
            .inputType(String.class).build();
        ReactAgent research = ReactAgent.builder().name("knowledge-research").model(chatModel)
            .instruction(researchInstruction).tools(researchTool).build();
        ReactAgent practice = ReactAgent.builder().name("practice-coach").model(chatModel)
            .instruction(practiceInstruction).tools(practiceTool).build();
        ReactAgent supervisor = ReactAgent.builder().name("study-supervisor").model(chatModel)
            .instruction(supervisorInstruction).tools(AgentTool.getFunctionToolCallback(research),
                AgentTool.getFunctionToolCallback(practice)).build();
        return new AgentTopology(supervisor);
    }

    private List<Exercise> exercises(Execution execution, Map<String, Citation> citations) {
        if (execution.practice == null) return List.of();
        var result = new ArrayList<Exercise>();
        for (int index = 0; index < execution.practice.exercises().size(); index++) {
            TemporaryExercise item = execution.practice.exercises().get(index);
            result.add(new Exercise(execution.runId + "-practice-" + (index + 1), ExerciseType.SHORT_ANSWER,
                item.question(), List.of(), item.answer(), item.explanation(),
                item.citations().stream().map(citations::get).toList()));
        }
        return result;
    }

    private StructuredAgentOutput decodeOutput(String raw) {
        try { return objectMapper.readValue(raw, StructuredAgentOutput.class); }
        catch (JsonProcessingException exception) { throw new IllegalArgumentException("INVALID_MODEL_OUTPUT"); }
    }

    private String json(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("INVALID_MODEL_OUTPUT"); }
    }

    private String scope(StudyRunRequest request) {
        return "knowledgeBaseId=" + request.scope().knowledgeBaseId() + ",materialId=" + request.scope().materialId();
    }

    private final class Execution {
        final StudyRunRequest request;
        final String runId = UUID.randomUUID().toString();
        final String sessionId;
        final Instant started;
        final long startedNanos = System.nanoTime();
        final Instant deadline;
        final SharedAgentBudget budget;
        final List<ActionTrace> trace = Collections.synchronizedList(new ArrayList<>());
        volatile EvidenceBundle evidence = new EvidenceBundle(List.of());
        volatile PracticeResult practice;
        volatile String context = "";
        volatile boolean budgetFailure;

        Execution(StudyRunRequest request, AgentConfigurationProperties properties, Clock clock) {
            this.request = request;
            this.sessionId = request.sessionId() == null ? UUID.randomUUID().toString() : request.sessionId();
            this.started = clock.instant();
            this.deadline = started.plus(properties.runTimeout());
            this.budget = new SharedAgentBudget(properties.maxSteps(), properties.subagentMaxSteps(),
                properties.maxToolCalls(), properties.runTimeout(), clock);
        }

        Duration remaining() {
            Duration remaining = Duration.between(clock.instant(), deadline);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }

        Map<String, Citation> citationMap() {
            return evidence.items().stream().filter(item -> item.verified() && item.materialId() != null
                && item.revisionId() != null && item.excerpt() != null
                && (item.pageNumber() != null || item.blockId() != null)).collect(Collectors.toUnmodifiableMap(
                    EvidenceBundle.Item::sourceRef,
                    item -> new Citation(item.stableId(), item.sourceRef(), item.materialId(), item.revisionId(),
                        item.pageNumber(), item.blockId(), item.excerpt(), false)));
        }

        ActionTrace trace(TraceActor actor, TraceAction action, TraceStatus status, long startNanos) {
            return new ActionTrace(trace.size() + 1, actor, action, status,
                Math.max(0, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos)));
        }

        StudyRunResult result(String answer, boolean uncertain, List<Citation> citations,
                              List<Exercise> exercises, String next, RunStatus status,
                              MemoryStatus memory, List<DegradationStatus> degradation) {
            SharedAgentBudget.Usage usage = budget.usage();
            long elapsed = Math.max(0, Duration.between(started, clock.instant()).toMillis());
            BudgetUsage budgetUsage = new BudgetUsage(usage.supervisorSteps(), properties.maxSteps(),
                usage.subagentSteps(), properties.subagentMaxSteps(), usage.toolCalls(), properties.maxToolCalls(),
                elapsed, properties.runTimeout().toMillis(), usage.timedOut());
            return new StudyRunResult(runId, sessionId, answer, uncertain, citations, exercises, next, status,
                budgetUsage, List.copyOf(trace), memory, degradation);
        }
    }

    private record AgentTopology(ReactAgent supervisor) { }
}
