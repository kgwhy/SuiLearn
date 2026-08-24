package com.suilearn.api.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.capability.CapabilityManifest;
import com.suilearn.api.agent.infrastructure.turn.JpaTurnStore;
import com.suilearn.api.agent.llm.LlmClient;
import com.suilearn.api.agent.llm.UsageTracker;
import com.suilearn.api.agent.llm.OpenAiCompatibleLlmClient;
import com.suilearn.api.agent.context.ContextBuilder;
import com.suilearn.api.agent.context.PromptBlockAssembler;
import com.suilearn.api.agent.context.RollingSessionSummary;
import com.suilearn.api.agent.context.SessionMessageHistory;
import com.suilearn.api.agent.context.TokenEstimator;
import com.suilearn.api.agent.loop.AgentLoop;
import com.suilearn.api.agent.learner.LearnerProfileService;
import com.suilearn.api.agent.loop.ToolDispatcher;
import com.suilearn.api.agent.memory.MemoryL2DocRepository;
import com.suilearn.api.agent.memory.MemoryL3DocRepository;
import com.suilearn.api.agent.memory.MemoryManager;
import com.suilearn.api.agent.memory.MemoryTurnRecorder;
import com.suilearn.api.agent.tool.AskUserTool;
import com.suilearn.api.agent.tool.EvidenceReadPort;
import com.suilearn.api.agent.tool.EvidenceSearchPort;
import com.suilearn.api.agent.tool.GeneratePracticeTool;
import com.suilearn.api.agent.tool.PersistMemoryTool;
import com.suilearn.api.agent.tool.PracticeCoachSubAgent;
import com.suilearn.api.agent.tool.ReadEvidenceTool;
import com.suilearn.api.agent.tool.RecallMemoryTool;
import com.suilearn.api.agent.tool.SearchKnowledgeTool;
import com.suilearn.api.agent.tool.Tool;
import com.suilearn.api.agent.infrastructure.turn.SessionMessageJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.SessionSummaryJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.TurnEventJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.TurnJpaRepository;
import com.suilearn.api.config.SuiLearnAiProperties;
import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentWebSocketProperties.class)
public class AgentTurnRuntimeConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(AgentTurnRuntimeConfiguration.class);

    @Bean
    TurnStore turnStore(TurnJpaRepository turns, TurnEventJpaRepository events,
                        SessionMessageJpaRepository messages, ObjectMapper objectMapper) {
        return new JpaTurnStore(turns, events, messages, objectMapper);
    }

    @Bean
    CapabilityRegistry capabilityRegistry() {
        return CapabilityRegistry.builtin();
    }

    @Bean
    LlmClient llmClient(SuiLearnAiProperties aiProperties, ObjectMapper objectMapper) {
        return new OpenAiCompatibleLlmClient(aiProperties, objectMapper);
    }

    @Bean
    ToolDispatcher toolDispatcher(ToolRegistry tools, ObjectMapper objectMapper) {
        return new ToolDispatcher(tools, objectMapper);
    }

    @Bean
    PromptBlockAssembler promptBlockAssembler() {
        return new PromptBlockAssembler(TokenEstimator.conservativeCharacters());
    }

    @Bean
    ContextBuilder contextBuilder(PromptBlockAssembler prompts,
                                  com.suilearn.api.agent.config.AgentConfigurationProperties properties) {
        return new ContextBuilder(TokenEstimator.conservativeCharacters(), prompts, properties.contextMaxTokens());
    }

    @Bean
    SessionMessageHistory sessionMessageHistory(SessionMessageJpaRepository messages) {
        return new SessionMessageHistory(messages);
    }

    @Bean
    RollingSessionSummary rollingSessionSummary(SessionMessageJpaRepository messages,
                                                SessionSummaryJpaRepository summaries,
                                                LlmClient client, Clock clock,
                                                SuiLearnAiProperties aiProperties,
                                                com.suilearn.api.agent.config.AgentConfigurationProperties properties) {
        return new RollingSessionSummary(messages, summaries, client, clock, aiProperties.chatModel(),
            properties.session().maxTurns());
    }

    @Bean
    UsageTracker usageTracker() {
        return UsageTracker.defaults();
    }

    @Bean
    AgentLoop agentLoop(LlmClient client, ToolDispatcher dispatcher, ToolRegistry tools,
                        com.suilearn.api.agent.config.AgentConfigurationProperties properties,
                        Clock clock, SuiLearnAiProperties aiProperties,
                        ContextBuilder contextBuilder, SessionMessageHistory history,
                        RollingSessionSummary summaries, UsageTracker usageTracker,
                        LearnerProfileService learnerProfiles) {
        return new AgentLoop(client, dispatcher, tools, properties, clock, aiProperties.chatModel(),
            contextBuilder, history, summaries, usageTracker, learnerProfiles);
    }

    @Bean
    TurnOrchestrator turnOrchestrator(CapabilityRegistry capabilities, AgentLoop loop,
                                      MemoryTurnRecorder memory) {
        return new TurnOrchestrator(capabilities, loop, memory);
    }

    @Bean
    SearchKnowledgeTool searchKnowledgeTool(ObjectProvider<EvidenceSearchPort> searchPort) {
        return new SearchKnowledgeTool(searchPort.getIfAvailable());
    }

    @Bean
    ReadEvidenceTool readEvidenceTool(ObjectProvider<EvidenceReadPort> readPort) {
        return new ReadEvidenceTool(readPort.getIfAvailable());
    }

    @Bean
    GeneratePracticeTool generatePracticeTool(ObjectProvider<PracticeCoachSubAgent> coach) {
        return new GeneratePracticeTool(coach.getIfAvailable());
    }

    @Bean
    RecallMemoryTool recallMemoryTool(ObjectProvider<MemoryManager> memory,
                                     MemoryL2DocRepository l2, MemoryL3DocRepository l3) {
        return new RecallMemoryTool(memory.getIfAvailable(), l2, l3);
    }

    @Bean
    PersistMemoryTool persistMemoryTool(ObjectProvider<MemoryManager> memory) {
        return new PersistMemoryTool(memory.getIfAvailable());
    }

    @Bean
    AskUserTool askUserTool() {
        return new AskUserTool();
    }

    @Bean
    ToolRegistry toolRegistry(Map<String, Tool> tools) {
        return new ToolRegistry(tools);
    }

    @Bean
    TurnRuntimeService turnRuntimeService(TurnStore store, TurnOrchestrator orchestrator,
                                          ObjectMapper objectMapper, Clock clock,
                                          CapabilityRegistry capabilities) {
        Set<String> capabilityNames = capabilities.manifests().stream()
            .map(CapabilityManifest::name).collect(Collectors.toUnmodifiableSet());
        return new TurnRuntimeService(store, orchestrator, objectMapper, clock, capabilityNames);
    }

    @Bean
    @Order(20)
    ApplicationRunner orphanedTurnRecoveryRunner(TurnRuntimeService runtime) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                try {
                    var recovered = runtime.recoverOrphans();
                    if (!recovered.isEmpty()) {
                        LOG.info("Marked {} running turn(s) as FAILED_ORPHANED", recovered.size());
                    }
                } catch (RuntimeException exception) {
                    LOG.error("Orphaned turn recovery failed", exception);
                }
            }
        };
    }
}
