package com.suilearn.api.agent.infrastructure.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.memory.AgentSemanticMemory;
import com.suilearn.api.agent.memory.MemoryType;
import com.suilearn.api.agent.memory.ScoredSemanticMemory;
import com.suilearn.api.agent.memory.SemanticMemoryQuery;
import com.suilearn.api.agent.memory.SemanticMemoryStore;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@ConditionalOnProperty(prefix = "suilearn.agent", name = "enabled", havingValue = "true")
public class JpaPgVectorSemanticMemoryStore implements SemanticMemoryStore {
    private static final TypeReference<List<Double>> VECTOR = new TypeReference<>() { };

    private final AgentSemanticMemoryJpaRepository repository;
    private final JdbcOperations jdbc;
    private final ObjectMapper objectMapper;

    public JpaPgVectorSemanticMemoryStore(AgentSemanticMemoryJpaRepository repository,
                                          JdbcOperations jdbc, ObjectMapper objectMapper) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AgentSemanticMemory> findByLearnerAndTypes(String learnerId, Set<MemoryType> types) {
        requireScope(learnerId, types);
        return repository.findByLearnerIdAndMemoryTypeIn(learnerId, typeNames(types)).stream()
            .map(this::toModel)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ScoredSemanticMemory> recall(SemanticMemoryQuery query, List<Double> embedding, int topK) {
        requireScope(query.learnerId(), query.types());
        if (embedding == null || embedding.isEmpty() || topK < 1) {
            throw new IllegalArgumentException("embedding and positive topK are required");
        }
        List<String> types = typeNames(query.types());
        String placeholders = String.join(",", types.stream().map(ignored -> "?").toList());
        String sql = "select id, learner_id, memory_type, content, content_fingerprint, embedding_json, "
            + "confidence, source_run_id, source_ref, created_at, updated_at, "
            + "1 - (embedding <=> cast(? as vector)) as similarity "
            + "from agent_semantic_memories "
            + "where learner_id = ? and memory_type in (" + placeholders + ") and embedding is not null "
            + "order by embedding <=> cast(? as vector), id limit ?";
        String vector = vectorLiteral(embedding);
        List<Object> parameters = new ArrayList<>();
        parameters.add(vector);
        parameters.add(query.learnerId());
        parameters.addAll(types);
        parameters.add(vector);
        parameters.add(Math.min(topK, query.topK()));
        return jdbc.query(sql, (resultSet, row) -> mapScored(resultSet), parameters.toArray());
    }

    @Override
    @Transactional
    public AgentSemanticMemory save(AgentSemanticMemory memory) {
        AgentSemanticMemoryEntity saved = repository.saveAndFlush(toEntity(memory));
        jdbc.update("update agent_semantic_memories set embedding = cast(? as vector) where id = ?",
            vectorLiteral(memory.embedding()), memory.id());
        return toModel(saved);
    }

    @Override
    @Transactional
    public long deleteByLearner(String learnerId) {
        if (learnerId == null || learnerId.isBlank()) {
            throw new IllegalArgumentException("learnerId is required");
        }
        return repository.deleteByLearnerId(learnerId);
    }

    private ScoredSemanticMemory mapScored(ResultSet resultSet) throws SQLException {
        AgentSemanticMemory memory = new AgentSemanticMemory(
            resultSet.getString("id"), resultSet.getString("learner_id"),
            MemoryType.valueOf(resultSet.getString("memory_type")), resultSet.getString("content"),
            resultSet.getString("content_fingerprint"), readVector(resultSet.getString("embedding_json")),
            resultSet.getDouble("confidence"), resultSet.getString("source_run_id"),
            resultSet.getString("source_ref"), resultSet.getTimestamp("created_at").toInstant(),
            resultSet.getTimestamp("updated_at").toInstant());
        return new ScoredSemanticMemory(memory, resultSet.getDouble("similarity"));
    }

    private AgentSemanticMemoryEntity toEntity(AgentSemanticMemory memory) {
        return new AgentSemanticMemoryEntity(memory.id(), memory.learnerId(), memory.memoryType().name(),
            memory.content(), memory.contentFingerprint(), writeVector(memory.embedding()), memory.confidence(),
            memory.sourceRunId(), memory.sourceRef(), memory.createdAt(), memory.updatedAt());
    }

    private AgentSemanticMemory toModel(AgentSemanticMemoryEntity entity) {
        return new AgentSemanticMemory(entity.getId(), entity.getLearnerId(),
            MemoryType.valueOf(entity.getMemoryType()), entity.getContent(), entity.getContentFingerprint(),
            readVector(entity.getEmbeddingJson()), entity.getConfidence(), entity.getSourceRunId(),
            entity.getSourceRef(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    private List<String> typeNames(Set<MemoryType> types) {
        return types.stream().sorted(Comparator.comparing(Enum::name)).map(Enum::name).toList();
    }

    private static void requireScope(String learnerId, Set<MemoryType> types) {
        if (learnerId == null || learnerId.isBlank() || types == null || types.isEmpty()
            || !MemoryType.allowed().containsAll(types)) {
            throw new IllegalArgumentException("learnerId and allowed memory types are required");
        }
    }

    private static String vectorLiteral(List<Double> embedding) {
        if (embedding == null || embedding.isEmpty() || embedding.stream().anyMatch(value -> value == null || !Double.isFinite(value))) {
            throw new IllegalArgumentException("finite embedding vector is required");
        }
        return embedding.toString();
    }

    private String writeVector(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (Exception failure) {
            throw new IllegalStateException("failed to serialize memory embedding", failure);
        }
    }

    private List<Double> readVector(String json) {
        try {
            return objectMapper.readValue(json, VECTOR);
        } catch (Exception failure) {
            throw new IllegalStateException("failed to deserialize memory embedding", failure);
        }
    }
}
