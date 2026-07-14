package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

class SuiLearnProcessingPropertiesTest {
    @Test
    void exposesRabbitRetryDelayEnvironmentOverridesToProcessingTopology() throws Exception {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("application.properties"));

        assertThat(properties.getProperty("suilearn.rabbitmq.retry-short-delay-ms"))
            .isEqualTo("${SUILEARN_RABBITMQ_RETRY_SHORT_DELAY_MS:30000}");
        assertThat(properties.getProperty("suilearn.rabbitmq.retry-long-delay-ms"))
            .isEqualTo("${SUILEARN_RABBITMQ_RETRY_LONG_DELAY_MS:300000}");
    }
}
