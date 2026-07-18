package com.suilearn.api.agent.tool;

import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.suilearn.api.agent.metrics.AgentMetrics;

public final class KnowledgeResearchSubAgent {
    private final EvidenceSearchPort searchPort;
    private final EvidenceReadPort readPort;
    private final AgentToolCatalog catalog;
    private final AgentMetrics metrics;

    public KnowledgeResearchSubAgent(EvidenceSearchPort searchPort, EvidenceReadPort readPort,
                                     AgentToolCatalog catalog) {
        this(searchPort, readPort, catalog, AgentMetrics.noop());
    }

    public KnowledgeResearchSubAgent(EvidenceSearchPort searchPort, EvidenceReadPort readPort,
                                     AgentToolCatalog catalog, AgentMetrics metrics) {
        this.searchPort = searchPort;
        this.readPort = readPort;
        this.catalog = catalog;
        this.metrics = metrics;
    }

    public EvidenceBundle research(Request request, SharedAgentBudget budget) {
        budget.consumeTool(AgentRole.SUPERVISOR, AgentAction.KNOWLEDGE_RESEARCH);
        budget.consumeStep(AgentRole.KNOWLEDGE_RESEARCH);
        catalog.requireAllowed(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.SEARCH_KNOWLEDGE);
        budget.consumeTool(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.SEARCH_KNOWLEDGE);
        List<EvidencePointer> pointers;
        try {
            pointers = searchPort.search(
                new EvidenceSearchPort.SearchRequest(request.researchGoal(), request.scope(), request.evidenceLimit()));
            metrics.recordTool(AgentMetrics.Tool.KNOWLEDGE_SEARCH, AgentMetrics.Outcome.SUCCESS);
        } catch (RuntimeException exception) {
            metrics.recordTool(AgentMetrics.Tool.KNOWLEDGE_SEARCH, AgentMetrics.Outcome.FAILED);
            metrics.recordSubAgent(AgentMetrics.Agent.KNOWLEDGE_RESEARCH, AgentMetrics.Outcome.FAILED);
            throw exception;
        }

        var items = new ArrayList<EvidenceBundle.Item>();
        var seen = new HashSet<String>();
        for (EvidencePointer pointer : pointers) {
            if (items.size() >= request.evidenceLimit()) {
                break;
            }
            if (!request.scope().matches(pointer.knowledgeBaseId(), pointer.materialId())
                || !seen.add(pointer.stableId())) {
                continue;
            }
            catalog.requireAllowed(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.READ_EVIDENCE);
            budget.consumeTool(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.READ_EVIDENCE);
            try {
                readPort.read(new EvidenceReadPort.ReadRequest(request.researchGoal(), pointer, request.scope()))
                    .filter(record -> valid(record, pointer, request.scope()))
                    .ifPresent(record -> items.add(new EvidenceBundle.Item(
                        record.stableId(), record.sourceRef(), record.content(), pointer.relevance(), true, true,
                        record.materialId(), record.revisionId(), record.pageNumber(), record.blockId(),
                        record.excerpt())));
                metrics.recordTool(AgentMetrics.Tool.EVIDENCE_READ, AgentMetrics.Outcome.SUCCESS);
            } catch (RuntimeException exception) {
                metrics.recordTool(AgentMetrics.Tool.EVIDENCE_READ, AgentMetrics.Outcome.FAILED);
                metrics.recordSubAgent(AgentMetrics.Agent.KNOWLEDGE_RESEARCH, AgentMetrics.Outcome.FAILED);
                throw exception;
            }
        }
        metrics.recordSubAgent(AgentMetrics.Agent.KNOWLEDGE_RESEARCH, AgentMetrics.Outcome.SUCCESS);
        return new EvidenceBundle(items);
    }

    private boolean valid(EvidenceRecord record, EvidencePointer pointer, AgentScope scope) {
        return !record.deleted()
            && record.stableId().equals(pointer.stableId())
            && record.sourceRef().equals(pointer.sourceRef())
            && scope.matches(record.knowledgeBaseId(), record.materialId());
    }

    public record Request(String researchGoal, AgentScope scope, int evidenceLimit) {
        public Request {
            researchGoal = RequiredText.value(researchGoal, "researchGoal");
            if (scope == null) {
                throw new IllegalArgumentException("scope is required");
            }
            if (evidenceLimit < 1) {
                throw new IllegalArgumentException("evidenceLimit must be positive");
            }
        }
    }
}
