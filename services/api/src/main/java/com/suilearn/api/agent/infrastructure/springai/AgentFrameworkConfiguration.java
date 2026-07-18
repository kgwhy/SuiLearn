package com.suilearn.api.agent.infrastructure.springai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.application.MemoryCandidateExtractor;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.context.ContextAssembler;
import com.suilearn.api.agent.context.ContextBudgetPolicy;
import com.suilearn.api.agent.context.ContextManager;
import com.suilearn.api.agent.context.TokenEstimator;
import com.suilearn.api.agent.memory.EmbeddingProvider;
import com.suilearn.api.agent.memory.EmbeddingResult;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.memory.MemoryPromotionPolicy;
import com.suilearn.api.agent.memory.SemanticMemoryStore;
import com.suilearn.api.agent.memory.SessionMemoryKeyFactory;
import com.suilearn.api.agent.memory.SessionMemoryService;
import com.suilearn.api.agent.memory.SessionMemoryStore;
import com.suilearn.api.agent.metrics.AgentMetrics;
import com.suilearn.api.agent.prompt.PromptRegistry;
import com.suilearn.api.agent.prompt.PromptTemplateRenderer;
import com.suilearn.api.agent.tool.AgentToolCatalog;
import com.suilearn.api.agent.tool.KnowledgeResearchSubAgent;
import com.suilearn.api.agent.tool.PracticeCoachSubAgent;
import com.suilearn.api.agent.tool.PracticeModelPort;
import com.suilearn.api.agent.tool.RetrievalEvidenceTools;
import com.suilearn.api.ai.application.RetrievalPort;
import com.suilearn.api.retrieval.Retriever;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import javax.sql.DataSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "suilearn.agent", name = "enabled", havingValue = "true")
public class AgentFrameworkConfiguration {
    @Bean
    @ConditionalOnMissingBean
    PromptTemplateRenderer agentPromptTemplateRenderer() {
        return new SpringAiPromptTemplateRenderer();
    }

    @Bean
    @ConditionalOnMissingBean
    PromptRegistry agentPromptRegistry(PromptTemplateRenderer renderer) {
        return new PromptRegistry(renderer);
    }

    @Bean
    @ConditionalOnMissingBean
    AgentMetrics agentMetrics(ObjectProvider<MeterRegistry> registry) {
        MeterRegistry available = registry.getIfAvailable();
        return available == null ? AgentMetrics.noop() : new AgentMetrics(available);
    }

    @Bean
    @ConditionalOnMissingBean
    Clock agentClock() {
        return Clock.systemUTC();
    }

    @Bean
    @ConditionalOnMissingBean
    AgentToolCatalog agentToolCatalog() {
        return AgentToolCatalog.fixedMvp();
    }

    @Bean
    @ConditionalOnMissingBean(RetrievalPort.class)
    @ConditionalOnBean(Retriever.class)
    RetrievalPort agentRetrievalPort(Retriever retriever) {
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
    KnowledgeResearchSubAgent knowledgeResearchSubAgent(ObjectProvider<RetrievalEvidenceTools> toolsProvider,
                                                         AgentToolCatalog catalog, AgentMetrics metrics) {
        RetrievalEvidenceTools tools = toolsProvider.getIfAvailable();
        return tools == null ? null : new KnowledgeResearchSubAgent(tools, tools, catalog, metrics);
    }

    @Bean
    PracticeModelPort practiceModelPort(ObjectProvider<ChatModel> modelProvider, PromptRegistry prompts,
                                        ObjectProvider<ObjectMapper> objectMapperProvider) {
        ChatModel model = modelProvider.getIfAvailable();
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        return model == null || objectMapper == null ? null : new SpringAiPracticeModelPort(model, prompts, objectMapper);
    }

    @Bean
    PracticeCoachSubAgent practiceCoachSubAgent(ObjectProvider<PracticeModelPort> modelProvider,
                                                 AgentToolCatalog catalog, AgentMetrics metrics) {
        PracticeModelPort model = modelProvider.getIfAvailable();
        return model == null ? null : new PracticeCoachSubAgent(model, catalog, metrics);
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

    @Bean
    ContextManager agentContextManager(AgentConfigurationProperties properties, AgentMetrics metrics) {
        return new ContextManager(new ContextAssembler(TokenEstimator.conservativeCharacters(),
            new ContextBudgetPolicy(), metrics), properties.contextMaxTokens());
    }

    @Bean
    AgentRuntimeReadiness agentRuntimeReadiness(ObjectProvider<StringRedisTemplate> redis) {
        StringRedisTemplate available = redis.getIfAvailable();
        return available == null ? AgentRuntimeReadiness.noOp() : new AgentRuntimeReadiness(available);
    }

    @Bean
    MemoryCandidateExtractor agentMemoryCandidateExtractor(ObjectProvider<ChatModel> modelProvider,
                                                           PromptRegistry prompts,
                                                           ObjectProvider<ObjectMapper> objectMapperProvider) {
        ChatModel model = modelProvider.getIfAvailable();
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        return model == null || objectMapper == null
            ? request -> java.util.Optional.empty()
            : new SpringAiMemoryCandidateExtractor(model, prompts, objectMapper);
    }

    @Bean(name = "studyAgentHealthIndicator")
    AgentHealthIndicator studyAgentHealthIndicator(ObjectProvider<ChatModel> model,
                                                   AgentRuntimeReadiness readiness,
                                                   ObjectProvider<SemanticMemoryStore> semantic,
                                                   ObjectProvider<DataSource> dataSource) {
        return new AgentHealthIndicator(model.getIfAvailable(), readiness,
            semantic.getIfAvailable(), dataSource.getIfAvailable());
    }

    @Bean
    LearningAgentPort learningAgentPort(
            ObjectProvider<ChatModel> model,
            ObjectProvider<KnowledgeResearchSubAgent> research,
            ObjectProvider<PracticeCoachSubAgent> practice,
            ObjectProvider<MemoryManager> memory,
            PromptRegistry prompts,
            AgentConfigurationProperties properties,
            Clock clock,
            AgentRuntimeReadiness readiness,
            ContextManager contextManager,
            MemoryCandidateExtractor candidateExtractor,
            ObjectProvider<ObjectMapper> objectMapperProvider) {
        ChatModel chatModel = model.getIfAvailable();
        if (chatModel == null) {
            return unavailable("AGENT_MODEL_UNAVAILABLE");
        }
        MemoryManager memoryManager = memory.getIfAvailable();
        if (memoryManager == null) {
            return unavailable("AGENT_SESSION_MEMORY_UNAVAILABLE");
        }
        KnowledgeResearchSubAgent researchAgent = research.getIfAvailable();
        PracticeCoachSubAgent practiceAgent = practice.getIfAvailable();
        if (researchAgent == null || practiceAgent == null) {
            return unavailable("AGENT_DEPENDENCY_UNAVAILABLE");
        }
        ObjectMapper objectMapper = objectMapperProvider.getIfAvailable();
        if (objectMapper == null) {
            return unavailable("AGENT_DEPENDENCY_UNAVAILABLE");
        }
        return new SpringAiAlibabaLearningAgentAdapter(chatModel, researchAgent, practiceAgent, prompts,
            properties, clock, readiness, contextManager, memoryManager, candidateExtractor, objectMapper);
    }

    private LearningAgentPort unavailable(String code) {
        return request -> { throw new IllegalStateException(code); };
    }
}
