package com.suilearn.api.agent.compatibility;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.application.LearningAgentPort;
import com.suilearn.api.agent.config.AgentConfiguration;
import com.suilearn.api.agent.infrastructure.springai.AgentFrameworkConfiguration;
import com.suilearn.api.agent.memory.*;
import com.suilearn.api.agent.prompt.PromptRegistry;
import com.suilearn.api.ai.application.RetrievalPort;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;

class AgentFrameworkWiringTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AgentConfiguration.class, AgentFrameworkConfiguration.class)
        .withPropertyValues(properties());

    @Test
    void disabledDoesNotCreateAgentModelPromptOrMemoryBeans() {
        runner.withPropertyValues("suilearn.agent.enabled=false").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(LearningAgentPort.class);
            assertThat(context).doesNotHaveBean(ChatModel.class);
            assertThat(context).doesNotHaveBean(PromptRegistry.class);
            assertThat(context).doesNotHaveBean(MemoryManager.class);
        });
    }

    @Test
    void enabledWithoutModelExposesStableUnavailablePortWithoutFailingContext() {
        runner.withPropertyValues("suilearn.agent.enabled=true").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(LearningAgentPort.class);
            assertThatThrownBy(() -> context.getBean(LearningAgentPort.class).run(request()))
                .hasMessage("AGENT_MODEL_UNAVAILABLE");
        });
    }

    @Test
    void enabledWithoutSessionStoreReportsSessionDependencyUnavailable() {
        runner.withUserConfiguration(ModelAndRetrieval.class)
            .withPropertyValues("suilearn.agent.enabled=true").run(context -> {
                assertThat(context).hasNotFailed();
                assertThatThrownBy(() -> context.getBean(LearningAgentPort.class).run(request()))
                    .hasMessage("AGENT_SESSION_MEMORY_UNAVAILABLE");
            });
    }

    @Test
    void enabledWithRequiredDependenciesCreatesRealAdapterAndMemoryManager() {
        runner.withUserConfiguration(CompleteDependencies.class)
            .withPropertyValues("suilearn.agent.enabled=true").run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(SessionMemoryService.class);
                assertThat(context).hasSingleBean(MemoryManager.class);
                assertThat(context).hasSingleBean(com.suilearn.api.agent.tool.KnowledgeResearchSubAgent.class);
                assertThat(context).hasSingleBean(com.suilearn.api.agent.tool.PracticeCoachSubAgent.class);
                assertThat(context).hasSingleBean(LearningAgentPort.class);
                assertThat(context.getBean(LearningAgentPort.class).getClass().getName())
                    .contains("SpringAiAlibabaLearningAgentAdapter");
                assertThat(context).hasSingleBean(PromptRegistry.class);
            });
    }

    private LearningAgentPort.StudyRunRequest request() {
        return new LearningAgentPort.StudyRunRequest("learner", "session", "question",
            new LearningAgentPort.AgentScope("kb", null), 1, LearningAgentPort.Difficulty.MEDIUM);
    }

    private String[] properties() {
        return new String[]{"suilearn.agent.enabled=false", "suilearn.agent.max-steps=4",
            "suilearn.agent.subagent-max-steps=3", "suilearn.agent.max-tool-calls=8",
            "suilearn.agent.run-timeout=90s", "suilearn.agent.context-max-tokens=12000",
            "suilearn.agent.practice-default-count=3", "suilearn.agent.session.ttl=24h",
            "suilearn.agent.session.max-turns=20", "suilearn.agent.memory.top-k=5",
            "suilearn.agent.memory.min-confidence=0.8"};
    }

    @Configuration(proxyBeanMethods = false)
    static class ModelAndRetrieval {
        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }
        @Bean ChatModel chatModel() { return prompt -> { throw new AssertionError("not invoked"); }; }
        @Bean RetrievalPort retrievalPort() { return new RetrievalPort() {
            public List<com.suilearn.api.model.SearchResult> search(RetrievalRequest request) { return List.of(); }
            public List<com.suilearn.api.model.MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) {
                return List.of();
            }
        }; }
    }

    @Configuration(proxyBeanMethods = false)
    static class CompleteDependencies extends ModelAndRetrieval {
        @Bean SessionMemoryStore sessionStore() { return new SessionMemoryStore() {
            public Optional<SessionMemory> read(String key, Duration ttl) { return Optional.empty(); }
            public void write(String key, SessionMemory memory, Duration ttl) { }
            public long deleteByPrefix(String prefix) { return 0; }
        }; }
        @Bean SemanticMemoryStore semanticStore() { return new SemanticMemoryStore() {
            public List<AgentSemanticMemory> findByLearnerAndTypes(String learner, Set<MemoryType> types) {
                return List.of();
            }
            public List<ScoredSemanticMemory> recall(SemanticMemoryQuery query, List<Double> vector, int topK) {
                return List.of();
            }
            public AgentSemanticMemory save(AgentSemanticMemory memory) { return memory; }
            public long deleteByLearner(String learner) { return 0; }
        }; }
    }
}
