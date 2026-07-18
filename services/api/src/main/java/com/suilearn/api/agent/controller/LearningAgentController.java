package com.suilearn.api.agent.controller;

import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import com.suilearn.api.agent.application.LearningAgentPort.Difficulty;
import com.suilearn.api.agent.application.LearningAgentPort.StudyRunRequest;
import com.suilearn.api.agent.application.LearningAgentPort.StudyRunResult;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.controller.StudyAgentDtos.ActionTraceEntry;
import com.suilearn.api.agent.controller.StudyAgentDtos.BudgetUsage;
import com.suilearn.api.agent.controller.StudyAgentDtos.MemoryDeletionResponse;
import com.suilearn.api.agent.controller.StudyAgentDtos.MemoryLayerDeletion;
import com.suilearn.api.agent.controller.StudyAgentDtos.RunRequest;
import com.suilearn.api.agent.controller.StudyAgentDtos.RunResponse;
import com.suilearn.api.agent.controller.StudyAgentDtos.SourceCitation;
import com.suilearn.api.agent.memory.DeletionStatus;
import com.suilearn.api.agent.memory.LayerDeletion;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.metrics.AgentMetrics;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class LearningAgentController {
    private final LearningAgentPort agent;
    private final MemoryManager memoryManager;
    private final boolean enabled;
    private final AgentConfigurationProperties properties;
    private final AgentMetrics metrics;

    @Autowired
    public LearningAgentController(ObjectProvider<LearningAgentPort> agent,
                                   ObjectProvider<MemoryManager> memoryManager,
                                   AgentConfigurationProperties properties,
                                   AgentMetrics metrics) {
        this(agent.getIfAvailable(), memoryManager.getIfAvailable(), properties.enabled(), properties, metrics);
    }

    LearningAgentController(LearningAgentPort agent, MemoryManager memoryManager, boolean enabled,
                            AgentConfigurationProperties properties, AgentMetrics metrics) {
        this.agent = agent;
        this.memoryManager = memoryManager;
        this.enabled = enabled;
        this.properties = properties;
        this.metrics = metrics;
    }

    @PostMapping("/api/v2/agents/study/runs")
    public RunResponse run(@Valid @RequestBody RunRequest request) {
        requireEnabled();
        if (!hasText(request.knowledgeBaseId()) && !hasText(request.materialId())) {
            throw new AgentApiException(AgentErrorCode.AGENT_SCOPE_REQUIRED);
        }
        if (agent == null) {
            throw new AgentApiException(AgentErrorCode.AGENT_MODEL_UNAVAILABLE);
        }
        long started = System.nanoTime();
        try {
            Difficulty difficulty = Difficulty.valueOf(request.effectiveDifficulty().name());
            StudyRunResult result = agent.run(new StudyRunRequest(
                request.learnerId(), request.sessionId(), request.question(),
                new AgentScope(request.knowledgeBaseId(), request.materialId()),
                request.effectivePracticeCount(properties.practiceDefaultCount()), difficulty));
            if (result.status() == LearningAgentPort.RunStatus.INVALID_MODEL_OUTPUT) {
                throw new AgentApiException(AgentErrorCode.INVALID_MODEL_OUTPUT);
            }
            RunResponse response = map(result);
            metrics.recordRun(result.status() == LearningAgentPort.RunStatus.BUDGET_EXHAUSTED
                ? AgentMetrics.Outcome.BUDGET_EXHAUSTED : AgentMetrics.Outcome.SUCCESS, elapsedMillis(started));
            return response;
        } catch (AgentApiException exception) {
            metrics.recordRun(AgentMetrics.Outcome.REJECTED, elapsedMillis(started));
            throw exception;
        } catch (RuntimeException exception) {
            metrics.recordRun(AgentMetrics.Outcome.FAILED, elapsedMillis(started));
            throw translate(exception);
        }
    }

    @DeleteMapping("/api/v2/agents/study/learners/{learnerId}/memories")
    public MemoryDeletionResponse deleteMemories(
        @PathVariable @NotBlank @Size(max = 128) String learnerId
    ) {
        requireEnabled();
        if (memoryManager == null) {
            throw new AgentApiException(AgentErrorCode.AGENT_SESSION_MEMORY_UNAVAILABLE);
        }
        var deletion = memoryManager.deleteLearnerMemory(learnerId);
        metrics.recordMemory(AgentMetrics.MemoryLayer.SESSION, deletion.session().status() == DeletionStatus.FAILED
            ? AgentMetrics.Outcome.FAILED : AgentMetrics.Outcome.SUCCESS);
        metrics.recordMemory(AgentMetrics.MemoryLayer.SEMANTIC, deletion.semantic().status() == DeletionStatus.FAILED
            ? AgentMetrics.Outcome.FAILED : AgentMetrics.Outcome.SUCCESS);
        return new MemoryDeletionResponse(learnerId, map(deletion.session()), map(deletion.semantic()));
    }

    private RunResponse map(StudyRunResult result) {
        List<SourceCitation> citations = result.citations().stream().map(this::map).toList();
        List<StudyAgentDtos.PracticeItem> practice = result.exercises().stream()
            .map(item -> new StudyAgentDtos.PracticeItem(
                item.id(), StudyAgentDtos.PracticeType.valueOf(item.type().name()), item.question(), item.options(),
                item.answer(), item.explanation(), item.citations().stream().map(this::map).toList()))
            .toList();
        List<ActionTraceEntry> trace = result.actionTrace().stream().map(this::map).toList();
        var memory = new StudyAgentDtos.MemoryStatus(
            StudyAgentDtos.SessionStatus.valueOf(result.memory().session().name()),
            StudyAgentDtos.SemanticRecallStatus.valueOf(result.memory().semanticRecall().name()),
            StudyAgentDtos.SemanticPersistenceStatus.valueOf(result.memory().semanticPersistence().name()));
        var usage = result.budgetUsage();
        BudgetUsage budget = new BudgetUsage(
            usage.supervisorSteps(), usage.maxSupervisorSteps(), usage.subagentSteps(),
            usage.maxSubagentStepsPerCall(), usage.toolCalls(), usage.maxToolCalls(),
            usage.elapsedMillis(), usage.timeoutMillis());
        List<StudyAgentDtos.DegradationStatus> degradation = result.degradationStatuses().stream()
            .map(value -> StudyAgentDtos.DegradationStatus.valueOf(value.name())).toList();
        return new RunResponse(
            result.runId(), result.sessionId(), map(result.status()), result.answer(), result.uncertain(), citations,
            practice, result.nextStep().isBlank() ? List.of() : List.of(result.nextStep()), memory, budget, trace,
            degradation);
    }

    private SourceCitation map(LearningAgentPort.Citation citation) {
        return new SourceCitation(citation.materialId(), citation.revisionId(), citation.pageNumber(),
            citation.blockId(), citation.excerpt(), citation.deleted());
    }

    private ActionTraceEntry map(LearningAgentPort.ActionTrace trace) {
        StudyAgentDtos.ActorType actorType = trace.actor() == LearningAgentPort.TraceActor.SUPERVISOR
            ? StudyAgentDtos.ActorType.SUPERVISOR : StudyAgentDtos.ActorType.SUBAGENT;
        StudyAgentDtos.TraceActor actor = switch (trace.actor()) {
            case SUPERVISOR -> StudyAgentDtos.TraceActor.STUDY_SUPERVISOR;
            case KNOWLEDGE_RESEARCH -> StudyAgentDtos.TraceActor.KNOWLEDGE_RESEARCH;
            case PRACTICE_COACH -> StudyAgentDtos.TraceActor.PRACTICE_COACH;
        };
        StudyAgentDtos.TraceAction action = switch (trace.action()) {
            case KNOWLEDGE_RESEARCH, PRACTICE_COACH -> StudyAgentDtos.TraceAction.DELEGATE;
            case SEARCH_KNOWLEDGE -> StudyAgentDtos.TraceAction.SEARCH;
            case READ_EVIDENCE -> StudyAgentDtos.TraceAction.READ;
            case SCHEMA_REPAIR -> StudyAgentDtos.TraceAction.SCHEMA_REPAIR;
            case STOP -> StudyAgentDtos.TraceAction.STOP;
        };
        StudyAgentDtos.TraceStatus status = switch (trace.status()) {
            case COMPLETED -> StudyAgentDtos.TraceStatus.SUCCEEDED;
            case REJECTED -> StudyAgentDtos.TraceStatus.REJECTED;
            case EXHAUSTED -> StudyAgentDtos.TraceStatus.BUDGET_EXHAUSTED;
            case FAILED -> StudyAgentDtos.TraceStatus.FAILED;
        };
        return new ActionTraceEntry(trace.step(), actorType, actor, action, status, trace.durationMillis());
    }

    private StudyAgentDtos.RunStatus map(LearningAgentPort.RunStatus status) {
        return switch (status) {
            case COMPLETED -> StudyAgentDtos.RunStatus.COMPLETED;
            case NO_EVIDENCE -> StudyAgentDtos.RunStatus.UNCERTAIN;
            case BUDGET_EXHAUSTED -> StudyAgentDtos.RunStatus.BUDGET_EXHAUSTED;
            case INVALID_MODEL_OUTPUT -> throw new AgentApiException(AgentErrorCode.INVALID_MODEL_OUTPUT);
        };
    }

    private MemoryLayerDeletion map(LayerDeletion deletion) {
        if (deletion.status() == DeletionStatus.FAILED) {
            return new MemoryLayerDeletion(StudyAgentDtos.DeletionResult.FAILED, deletion.deletedCount());
        }
        return new MemoryLayerDeletion(deletion.deletedCount() > 0
            ? StudyAgentDtos.DeletionResult.DELETED : StudyAgentDtos.DeletionResult.NOT_FOUND,
            deletion.deletedCount());
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new AgentApiException(AgentErrorCode.AGENT_FEATURE_DISABLED);
        }
    }

    private AgentApiException translate(RuntimeException exception) {
        String code = exception.getMessage();
        if (code != null) {
            for (AgentErrorCode candidate : AgentErrorCode.values()) {
                if (code.startsWith(candidate.name())) {
                    return new AgentApiException(candidate);
                }
            }
        }
        return exception instanceof IllegalArgumentException
            ? new AgentApiException(AgentErrorCode.INVALID_AGENT_REQUEST)
            : new AgentApiException(AgentErrorCode.AGENT_DEPENDENCY_UNAVAILABLE);
    }

    private long elapsedMillis(long started) {
        return Math.max(0, (System.nanoTime() - started) / 1_000_000);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
