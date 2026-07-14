package com.suilearn.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.EnableScheduling;

class RabbitMessagingConfigurationTest {
    @Test
    void declaresIsolatedDurableWorkRetryAndDeadLetterQueues() {
        var topology = new RabbitMessagingConfiguration().processingTopology(SuiLearnProcessingProperties.from(new org.springframework.mock.env.MockEnvironment()));

        assertThat(topology.getDeclarables()).filteredOn(declarable -> declarable instanceof org.springframework.amqp.core.Queue)
            .extracting(declarable -> ((org.springframework.amqp.core.Queue) declarable).getName())
            .contains(
                "document.processing", "document.processing.retry.short", "document.processing.retry.long", "document.processing.dlq",
                "knowledge-point.generation", "knowledge-point.generation.retry.short", "knowledge-point.generation.retry.long", "knowledge-point.generation.dlq",
                "question.generation", "question.generation.retry.short", "question.generation.retry.long", "question.generation.dlq"
            );
    }

    @Test
    void usesConfiguredRetryDelays() {
        var properties = SuiLearnProcessingProperties.from(new org.springframework.mock.env.MockEnvironment()
            .withProperty("suilearn.rabbitmq.retry-short-delay-ms", "30000")
            .withProperty("suilearn.rabbitmq.retry-long-delay-ms", "300000"));
        var queues = new RabbitMessagingConfiguration().processingTopology(properties).getDeclarables().stream()
            .filter(org.springframework.amqp.core.Queue.class::isInstance).map(org.springframework.amqp.core.Queue.class::cast).toList();
        assertThat(queues).anySatisfy(queue -> assertThat(queue.getArguments().get("x-message-ttl")).isEqualTo(30000));
        assertThat(queues).anySatisfy(queue -> assertThat(queue.getArguments().get("x-message-ttl")).isEqualTo(300000));
    }

    @Test
    void enablesScheduledRecoveryAndDeclaresManualAckRuntimeListenerFactory() {
        assertThat(RabbitMessagingConfiguration.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
        assertThat(RabbitMessagingConfiguration.class.getDeclaredMethods())
            .extracting(java.lang.reflect.Method::getName)
            .contains("processingRabbitListenerContainerFactory", "persistentOutboxDispatcher", "persistentInboundMessageStore");
    }

    @Test
    void sizesListenerConcurrencyFromProcessingConfiguration() {
        var properties = SuiLearnProcessingProperties.from(new org.springframework.mock.env.MockEnvironment()
            .withProperty("suilearn.processing.concurrency", "4"));
        var executor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
        executor.initialize();
        var factory = new RabbitMessagingConfiguration().processingRabbitListenerContainerFactory(
            org.mockito.Mockito.mock(org.springframework.amqp.rabbit.connection.ConnectionFactory.class), executor, true, properties
        );

        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(factory, "concurrentConsumers")).isEqualTo(4);
        assertThat(org.springframework.test.util.ReflectionTestUtils.getField(factory, "maxConcurrentConsumers")).isEqualTo(4);
    }
}
