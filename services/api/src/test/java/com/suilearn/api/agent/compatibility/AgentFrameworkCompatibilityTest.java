package com.suilearn.api.agent.compatibility;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class AgentFrameworkCompatibilityTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(MinimalAgentConfiguration.class);

    @Test
    void disabledContextStartsWithoutCreatingAgentFrameworkBeans() {
        contextRunner
                .withPropertyValues("suilearn.agent.enabled=false")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(ReactAgent.class);
                    assertThat(context).doesNotHaveBean(ChatModel.class);
                });
    }

    @Test
    void enabledContextCreatesARealReactAgentWithAnOfflineChatModel() {
        contextRunner
                .withPropertyValues("suilearn.agent.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatModel.class);
                    assertThat(context).hasSingleBean(ReactAgent.class);
                    assertThat(context.getBean(ReactAgent.class)).isNotNull();
                });
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnProperty(name = "suilearn.agent.enabled", havingValue = "true")
    static class MinimalAgentConfiguration {

        @Bean
        ChatModel offlineChatModel() {
            return new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    throw new AssertionError("Compatibility context must not invoke a model");
                }
            };
        }

        @Bean
        ReactAgent compatibilityReactAgent(ChatModel chatModel) {
            return ReactAgent.builder()
                    .name("compatibility-agent")
                    .model(chatModel)
                    .build();
        }
    }
}
