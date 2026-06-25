package com.suilearn.api.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class AppConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    SuiLearnAiProperties suiLearnAiProperties(Environment environment) {
        return new SuiLearnAiProperties(
            environment.getProperty("suilearn.ai.provider", "openai-compatible"),
            environment.getProperty("suilearn.ai.base-url", ""),
            environment.getProperty("suilearn.ai.api-key", ""),
            environment.getProperty("suilearn.ai.chat-base-url", ""),
            environment.getProperty("suilearn.ai.chat-api-key", ""),
            environment.getProperty("suilearn.ai.embedding-base-url", ""),
            environment.getProperty("suilearn.ai.embedding-api-key", ""),
            environment.getProperty("suilearn.ai.chat-model", ""),
            environment.getProperty("suilearn.ai.embedding-model", ""),
            environment.getProperty("suilearn.ai.timeout-ms", Integer.class, 30000),
            environment.getProperty("suilearn.ai.max-retries", Integer.class, 2)
        );
    }
}
