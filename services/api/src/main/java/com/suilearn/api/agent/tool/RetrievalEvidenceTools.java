package com.suilearn.api.agent.tool;

import com.suilearn.api.ai.application.RetrievalPort;
import com.suilearn.api.ai.application.RetrievalPort.RetrievalRequest;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SourceRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class RetrievalEvidenceTools implements EvidenceSearchPort, EvidenceReadPort {
    private final RetrievalPort retrievalPort;

    public RetrievalEvidenceTools(RetrievalPort retrievalPort) {
        this.retrievalPort = retrievalPort;
    }

    @Override
    public List<EvidencePointer> search(SearchRequest request) {
        var retrievalRequest = new RetrievalRequest(request.query(), request.scope().knowledgeBaseId(),
            request.scope().materialId());
        var unique = new LinkedHashMap<String, EvidencePointer>();
        retrievalPort.search(retrievalRequest).stream()
            .flatMap(result -> result.sourceRefs().stream().map(source -> pointer(source, result.score())))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(pointer -> request.scope().matches(pointer.knowledgeBaseId(), pointer.materialId()))
            .limit(request.limit())
            .forEach(pointer -> unique.putIfAbsent(pointer.stableId(), pointer));
        return List.copyOf(unique.values());
    }

    @Override
    public Optional<EvidenceRecord> read(ReadRequest request) {
        var retrievalRequest = new RetrievalRequest(request.query(), request.scope().knowledgeBaseId(),
            request.scope().materialId());
        return retrievalPort.retrieveEvidence(retrievalRequest, 20).stream()
            .filter(chunk -> matches(chunk, request.pointer(), request.scope()))
            .findFirst()
            .map(chunk -> new EvidenceRecord(
                request.pointer().stableId(), request.pointer().sourceRef(), chunk.knowledgeBaseId(),
                chunk.materialId(), chunk.content(), chunk.sourceRef() != null && chunk.sourceRef().deleted(),
                chunk.sourceRef().revisionId(), chunk.sourceRef().pageNumber(), chunk.sourceRef().blockId(),
                chunk.sourceRef().excerpt()));
    }

    private Optional<EvidencePointer> pointer(SourceRef source, double score) {
        if (source == null || source.deleted() || source.id() == null || source.id().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new EvidencePointer(source.id(), source.id(), source.knowledgeBaseId(),
            source.materialId(), Math.max(0.0d, Math.min(1.0d, score)), source.revisionId(),
            source.pageNumber(), source.blockId(), source.excerpt()));
    }

    private boolean matches(MaterialChunk chunk, EvidencePointer pointer,
                            com.suilearn.api.agent.application.LearningAgentPort.AgentScope scope) {
        SourceRef source = chunk.sourceRef();
        return source != null && !source.deleted()
            && pointer.sourceRef().equals(source.id())
            && scope.matches(chunk.knowledgeBaseId(), chunk.materialId());
    }
}
