package com.suilearn.api.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.AgentWebSocketProperties;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
import com.suilearn.api.security.AgentAuthProperties;
import com.suilearn.api.security.LearnerTokenHandshakeInterceptor;
import com.suilearn.api.security.LearnerTokenRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableWebSocket
public class AgentTurnWebSocketConfiguration {
    @Bean
    AgentTurnWebSocketHandler agentTurnWebSocketHandler(
        TurnRuntimeService runtime,
        AgentConfigurationProperties agentProperties,
        AgentWebSocketProperties websocketProperties,
        ObjectMapper objectMapper,
        AgentAuthProperties authProperties
    ) {
        return new AgentTurnWebSocketHandler(runtime, agentProperties, websocketProperties, objectMapper, authProperties);
    }

    @Bean
    LearnerTokenHandshakeInterceptor learnerTokenHandshakeInterceptor(LearnerTokenRegistry registry,
                                                                     AgentAuthProperties authProperties) {
        return new LearnerTokenHandshakeInterceptor(registry, authProperties);
    }

    @Bean
    WebSocketConfigurer agentTurnWebSocketConfigurer(AgentTurnWebSocketHandler handler,
                                                     LearnerTokenHandshakeInterceptor handshakeInterceptor) {
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
                registry.addHandler(handler, "/api/v2/ws")
                    .addInterceptors(handshakeInterceptor)
                    .setAllowedOriginPatterns("*");
            }
        };
    }
}
