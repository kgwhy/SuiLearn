package com.suilearn.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ProcessingExecutorConfig {
    @Bean(name = "processingConsumerTaskExecutor")
    ThreadPoolTaskExecutor processingConsumerTaskExecutor(SuiLearnProcessingProperties properties) {
        return executor("suilearn-consumer-", properties.processingConcurrency());
    }

    @Bean(name = "ocrTaskExecutor")
    ThreadPoolTaskExecutor ocrTaskExecutor(SuiLearnProcessingProperties properties) {
        return executor("suilearn-ocr-", properties.ocrConcurrency());
    }

    private ThreadPoolTaskExecutor executor(String prefix, int concurrency) {
        var executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix(prefix);
        executor.setCorePoolSize(concurrency);
        executor.setMaxPoolSize(concurrency);
        executor.setQueueCapacity(concurrency);
        executor.initialize();
        return executor;
    }
}
