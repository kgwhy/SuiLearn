package com.suilearn.api.agent.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.infrastructure.turn.JpaTurnStore;
import com.suilearn.api.agent.infrastructure.turn.SessionMessageJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.TurnEventJpaRepository;
import com.suilearn.api.agent.infrastructure.turn.TurnJpaRepository;
import java.time.Clock;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
    TurnExecutor turnExecutor() {
        return new UnavailableTurnExecutor();
    }

    @Bean
    TurnRuntimeService turnRuntimeService(TurnStore store, TurnExecutor executor,
                                          ObjectMapper objectMapper, Clock clock) {
        return new TurnRuntimeService(store, executor, objectMapper, clock, Set.of("study_agent"));
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
