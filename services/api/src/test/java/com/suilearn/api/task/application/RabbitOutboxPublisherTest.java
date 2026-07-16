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
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
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

    @Test
    void propagatesPersistedReplayRetryCountIntoTheBrokerMessageHeader() {
        var template = mock(RabbitTemplate.class);
        var confirm = new CorrelationData.Confirm(true, null);
        var publisher = new RabbitOutboxBrokerPublisher(template, () -> confirm);
        var event = new DurableOutboxEvent("outbox_2", "task_1", "PARSING", "{}", OutboxDeliveryState.PENDING,
            0, Instant.EPOCH, null, null, 3);

        assertThat(publisher.publish(event)).isTrue();

        var processor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(template).convertAndSend(eq("suilearn.processing"), eq("document.processing"), eq("{}"), processor.capture(), any());
        var message = processor.getValue().postProcessMessage(new org.springframework.amqp.core.Message(new byte[0], new MessageProperties()));
        assertThat(message.getMessageProperties().getHeaders())
            .containsEntry(ProcessingFailureRouter.RETRY_COUNT_HEADER, 3);
    }
}
