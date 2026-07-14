package com.suilearn.api.task.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

class RabbitProcessingMessageEndpointTest {
    @Test
    void runsTheMessageHandlerInsideTheDurableClaimTransactionBeforeAcking() throws Exception {
        var messages = mock(InboundMessageStore.class);
        when(messages.claim("message_1")).thenReturn(true);
        var handler = mock(ProcessingMessageHandler.class);
        var failures = mock(ProcessingFailureRouter.class);
        var channel = mock(Channel.class);
        var properties = new MessageProperties();
        properties.setMessageId("message_1");
        properties.setDeliveryTag(42L);
        var endpoint = new RabbitProcessingMessageEndpoint(messages, immediateTransactions(), handler, failures);

        endpoint.consume(new Message("{}".getBytes(), properties), channel);

        verify(handler).handle(org.mockito.ArgumentMatchers.any(Message.class));
        verify(messages).complete("message_1");
        verify(channel).basicAck(42L, false);
    }

    @Test
    void acknowledgesOnlyAfterTheFailureTransferIsConfirmed() throws Exception {
        var messages = mock(InboundMessageStore.class);
        when(messages.claim("message_1")).thenReturn(true);
        var handler = mock(ProcessingMessageHandler.class);
        var failures = mock(ProcessingFailureRouter.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("handler failed")).when(handler).handle(org.mockito.ArgumentMatchers.any());
        var channel = mock(Channel.class);
        var properties = new MessageProperties();
        properties.setMessageId("message_1");
        properties.setDeliveryTag(42L);
        var endpoint = new RabbitProcessingMessageEndpoint(messages, immediateTransactions(), handler, failures);

        endpoint.consume(new Message("{}".getBytes(), properties), channel);

        verify(failures).route(org.mockito.ArgumentMatchers.any(Message.class), org.mockito.ArgumentMatchers.any(IllegalStateException.class));
        verify(channel).basicAck(42L, false);
    }

    @Test
    void leavesTheOriginalMessageUnacknowledgedWhenFailureTransferCannotBeConfirmed() throws Exception {
        var messages = mock(InboundMessageStore.class);
        when(messages.claim("message_1")).thenReturn(true);
        var handler = mock(ProcessingMessageHandler.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("handler failed")).when(handler).handle(org.mockito.ArgumentMatchers.any());
        var failures = mock(ProcessingFailureRouter.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("failure route not confirmed"))
            .when(failures).route(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        var channel = mock(Channel.class);
        var properties = new MessageProperties();
        properties.setMessageId("message_1");
        properties.setDeliveryTag(42L);
        var endpoint = new RabbitProcessingMessageEndpoint(messages, immediateTransactions(), handler, failures);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> endpoint.consume(new Message("{}".getBytes(), properties), channel))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not confirmed");

        verify(channel, never()).basicAck(42L, false);
    }

    @Test
    void routesAMissingMessageIdAsPermanentFailureAndAcknowledgesOnlyAfterThatRouteSucceeds() throws Exception {
        var messages = mock(InboundMessageStore.class);
        var handler = mock(ProcessingMessageHandler.class);
        var failures = mock(ProcessingFailureRouter.class);
        var channel = mock(Channel.class);
        var properties = new MessageProperties();
        properties.setDeliveryTag(42L);
        var endpoint = new RabbitProcessingMessageEndpoint(messages, immediateTransactions(), handler, failures);

        endpoint.consume(new Message("{}".getBytes(), properties), channel);

        verify(failures).route(org.mockito.ArgumentMatchers.any(Message.class), org.mockito.ArgumentMatchers.argThat(error ->
            error instanceof IllegalArgumentException && error.getMessage().contains("missing or invalid messageId")));
        verify(channel).basicAck(42L, false);
        verify(messages, never()).claim(org.mockito.ArgumentMatchers.any());
    }

    private TransactionBoundary immediateTransactions() {
        return new TransactionBoundary() {
            @Override public <T> T execute(Work<T> work) { return work.run(); }
        };
    }
}
