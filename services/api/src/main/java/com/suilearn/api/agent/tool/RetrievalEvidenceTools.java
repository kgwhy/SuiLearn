package com.suilearn.api.agent.tool;

import com.suilearn.api.ai.application.RetrievalPort;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.ai.application.RetrievalPort.RetrievalRequest;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SourceRef;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;

public final class RetrievalEvidenceTools implements EvidenceSearchPort, EvidenceReadPort {
    private final RetrievalPort retrievalPort;
    private final MaterialChunkStore chunkStore;

    public RetrievalEvidenceTools(RetrievalPort retrievalPort) {
        this(retrievalPort, null);
    }

    public RetrievalEvidenceTools(RetrievalPort retrievalPort, MaterialChunkStore chunkStore) {
        this.retrievalPort = retrievalPort;
        this.chunkStore = chunkStore;
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
        Optional<MaterialChunk> byId = readById(request.pointer(), request.scope());
        if (byId.isPresent()) {
            return byId.map(chunk -> record(request.pointer(), chunk));
        }
        var retrievalRequest = new RetrievalRequest(request.query(), request.scope().knowledgeBaseId(),
            request.scope().materialId());
        return retrievalPort.retrieveEvidence(retrievalRequest, 20).stream()
            .filter(chunk -> matches(chunk, request.pointer(), request.scope()))
            .findFirst()
            .map(chunk -> record(request.pointer(), chunk));
    }

    private Optional<MaterialChunk> readById(EvidencePointer pointer, StudyScope scope) {
        if (chunkStore == null) {
            return Optional.empty();
        }
        String chunkId = firstNonBlank(pointer.blockId(), pointer.stableId(), pointer.sourceRef());
        if (chunkId == null) {
            return Optional.empty();
        }
        return chunkStore.find(chunkId)
            .filter(chunk -> matches(chunk, pointer, scope));
    }

    private static EvidenceRecord record(EvidencePointer pointer, MaterialChunk chunk) {
        SourceRef source = chunk.sourceRef();
        return new EvidenceRecord(
            pointer.stableId(),
            pointer.sourceRef(),
            chunk.knowledgeBaseId(),
            chunk.materialId(),
            chunk.content(),
            source != null && source.deleted(),
            source != null ? source.revisionId() : pointer.revisionId(),
            source != null ? source.pageNumber() : pointer.pageNumber(),
            source != null ? source.blockId() : pointer.blockId(),
            source != null ? source.excerpt() : pointer.excerpt());
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private Optional<EvidencePointer> pointer(SourceRef source, double score) {
        if (source == null || source.deleted() || source.id() == null || source.id().isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new EvidencePointer(source.id(), source.id(), source.knowledgeBaseId(),
            source.materialId(), Math.max(0.0d, Math.min(1.0d, score)), source.revisionId(),
            source.pageNumber(), source.blockId(), source.excerpt()));
    }

    private boolean matches(MaterialChunk chunk, EvidencePointer pointer, StudyScope scope) {
        SourceRef source = chunk.sourceRef();
        return source != null && !source.deleted()
            && pointer.sourceRef().equals(source.id())
            && scope.matches(chunk.knowledgeBaseId(), chunk.materialId());
    }
}
