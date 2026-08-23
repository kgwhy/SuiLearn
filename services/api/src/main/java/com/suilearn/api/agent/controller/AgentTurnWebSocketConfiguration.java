package com.suilearn.api.agent.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.config.AgentConfigurationProperties;
import com.suilearn.api.agent.runtime.AgentWebSocketProperties;
import com.suilearn.api.agent.runtime.TurnRuntimeService;
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
        ObjectMapper objectMapper
    ) {
        return new AgentTurnWebSocketHandler(runtime, agentProperties, websocketProperties, objectMapper);
    }

    @Bean
    WebSocketConfigurer agentTurnWebSocketConfigurer(AgentTurnWebSocketHandler handler) {
        return new WebSocketConfigurer() {
            @Override
            public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
                registry.addHandler(handler, "/api/v2/ws").setAllowedOriginPatterns("*");
            }
        };
    }
}
