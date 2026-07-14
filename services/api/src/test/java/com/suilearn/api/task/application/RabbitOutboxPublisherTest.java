package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class RabbitOutboxPublisherTest {
    @Test
    void publishesPersistentMessageAndWaitsForPositiveBrokerConfirm() {
        var template = mock(RabbitTemplate.class);
        when(template.getExchange()).thenReturn("suilearn.processing");
        var confirm = new CorrelationData.Confirm(true, null);
        var future = CompletableFuture.completedFuture(confirm);
        var publisher = new RabbitOutboxBrokerPublisher(template, future::get);
        var event = new DurableOutboxEvent("outbox_1", "task_1", "PARSING", "{}", OutboxDeliveryState.PENDING,
            0, Instant.EPOCH, null, null);

        var confirmed = publisher.publish(event);

        assertThat(confirmed).isTrue();
        verify(template).convertAndSend(eq("suilearn.processing"), eq("document.processing"), eq("{}"), any(), any());
    }
}
