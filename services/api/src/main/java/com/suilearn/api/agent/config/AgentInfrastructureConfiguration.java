package com.suilearn.api.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.memory.EmbeddingProvider;
import com.suilearn.api.agent.memory.EmbeddingResult;
import com.suilearn.api.agent.memory.MemoryConsolidationCommandRepository;
import com.suilearn.api.agent.memory.MemoryConsolidator;
import com.suilearn.api.agent.memory.MemoryL2DocRepository;
import com.suilearn.api.agent.memory.MemoryL3DocRepository;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.memory.MemoryTurnRecorder;
import com.suilearn.api.agent.memory.MemoryMetaRepository;
import com.suilearn.api.agent.memory.MemorySnapshotRecorder;
import com.suilearn.api.agent.memory.MemorySnapshotRepository;
import com.suilearn.api.agent.memory.MemoryTraceRecorder;
import com.suilearn.api.agent.memory.MemoryTraceRepository;
import com.suilearn.api.agent.memory.MemoryPromotionPolicy;
import com.suilearn.api.agent.memory.SemanticMemoryStore;
import com.suilearn.api.agent.memory.SessionMemoryKeyFactory;
import com.suilearn.api.agent.memory.SessionMemoryService;
import com.suilearn.api.agent.memory.SessionMemoryStore;
import com.suilearn.api.agent.metrics.AgentMetrics;
import com.suilearn.api.agent.tool.LlmPracticeModelPort;
import com.suilearn.api.agent.tool.PracticeCoachSubAgent;
import com.suilearn.api.agent.tool.PracticeModelPort;
import com.suilearn.api.agent.tool.RetrievalEvidenceTools;
import com.suilearn.api.ai.application.RetrievalPort;
import com.suilearn.api.config.SuiLearnAiProperties;
import com.suilearn.api.retrieval.Retriever;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Post-3b infrastructure beans for the new runtime tools. It intentionally does
 * not create PromptRegistry, ContextManager, health indicators, or any Spring AI
 * legacy agent-framework bean.
 */
@Configuration(proxyBeanMethods = false)
public class AgentInfrastructureConfiguration {
    @Bean
    @ConditionalOnMissingBean
    AgentMetrics agentMetrics(ObjectProvider<MeterRegistry> registry) {
        MeterRegistry available = registry.getIfAvailable();
        return available == null ? AgentMetrics.noop() : new AgentMetrics(available);
    }

    @Bean
    @ConditionalOnMissingBean(RetrievalPort.class)
    RetrievalPort agentRetrievalPort(ObjectProvider<Retriever> retrieverProvider) {
        Retriever retriever = retrieverProvider.getIfAvailable();
        if (retriever == null) {
            return null;
        }
        return new RetrievalPort() {
            @Override public java.util.List<com.suilearn.api.model.SearchResult> search(RetrievalRequest request) {
                return retriever.search(new Retriever.RetrievalRequest(
                    request.query(), request.knowledgeBaseId(), request.materialId()));
            }
            @Override public java.util.List<com.suilearn.api.model.MaterialChunk> retrieveEvidence(
                    RetrievalRequest request, int limit) {
                return retriever.retrieveEvidence(new Retriever.RetrievalRequest(
                    request.query(), request.knowledgeBaseId(), request.materialId()), limit);
            }
        };
    }

    @Bean
    RetrievalEvidenceTools retrievalEvidenceTools(ObjectProvider<RetrievalPort> retrievalPort) {
        RetrievalPort available = retrievalPort.getIfAvailable();
        return available == null ? null : new RetrievalEvidenceTools(available);
    }

    @Bean
    PracticeModelPort practiceModelPort(LlmClient client, ObjectMapper objectMapper, SuiLearnAiProperties properties) {
        return new LlmPracticeModelPort(client, objectMapper, properties.chatModel());
    }

    @Bean
    PracticeCoachSubAgent practiceCoachSubAgent(PracticeModelPort model, AgentMetrics metrics) {
        return new PracticeCoachSubAgent(model, metrics);
    }

    @Bean
    @ConditionalOnMissingBean
    EmbeddingProvider agentMemoryEmbeddingProvider(
            ObjectProvider<com.suilearn.api.retrieval.EmbeddingProvider> provider) {
        return content -> {
            var available = provider.getIfAvailable();
            if (available == null || !available.supportsEmbeddings()) {
                return EmbeddingResult.unavailable("embedding unavailable");
            }
            try {
                return EmbeddingResult.available(available.embed(content).values());
            } catch (RuntimeException exception) {
                return EmbeddingResult.unavailable("embedding unavailable");
            }
        };
    }

    @Bean
    SessionMemoryService sessionMemoryService(ObjectProvider<SessionMemoryStore> storeProvider,
                                               AgentConfigurationProperties properties) {
        SessionMemoryStore store = storeProvider.getIfAvailable();
        return store == null ? null : new SessionMemoryService(store,
            new SessionMemoryKeyFactory("suilearn:agent:session:v1"),
            properties.session().ttl(), properties.session().maxTurns());
    }

    @Bean
    MemoryTraceRecorder memoryTraceRecorder(MemoryTraceRepository traces, Clock clock) {
        return new MemoryTraceRecorder(traces, clock);
    }

    @Bean
    MemorySnapshotRecorder memorySnapshotRecorder(MemorySnapshotRepository snapshots, Clock clock) {
        return new MemorySnapshotRecorder(snapshots, clock);
    }

    @Bean
    MemoryConsolidator memoryConsolidator(MemoryConsolidationCommandRepository commands,
                                          MemorySnapshotRepository snapshots,
                                          MemoryL2DocRepository l2, MemoryL3DocRepository l3,
                                          MemoryMetaRepository meta, LlmClient client,
                                          ObjectMapper objectMapper, SuiLearnAiProperties properties,
                                          Clock clock) {
        return new MemoryConsolidator(commands, snapshots, l2, l3, meta, client, objectMapper,
            properties.chatModel(), clock);
    }

    @Bean
    MemoryTurnRecorder memoryTurnRecorder(MemoryTraceRecorder traces, MemorySnapshotRecorder snapshots,
                                          MemoryConsolidator consolidator, ObjectMapper objectMapper) {
        return new MemoryTurnRecorder(traces, snapshots, consolidator, objectMapper);
    }

    @Bean
    MemoryManager agentMemoryManager(ObjectProvider<SessionMemoryService> sessionsProvider,
                                     ObjectProvider<SemanticMemoryStore> semanticStoreProvider,
                                     EmbeddingProvider embeddings, AgentConfigurationProperties properties,
                                     Clock clock) {
        SessionMemoryService sessions = sessionsProvider.getIfAvailable();
        SemanticMemoryStore semanticStore = semanticStoreProvider.getIfAvailable();
        if (sessions == null || semanticStore == null) {
            return null;
        }
        return new MemoryManager(sessions, semanticStore, embeddings,
            new MemoryPromotionPolicy(properties.memory().minConfidence(), 1, 2_000),
            properties.memory().topK(), () -> Instant.now(clock));
    }
}
