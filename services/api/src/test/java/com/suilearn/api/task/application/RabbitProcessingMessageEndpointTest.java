package com.suilearn.api.task.application;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rabbitmq.client.Channel;
import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

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

    @Test
    void routesParserRejectionFromTheMaterialWorkerDirectlyToTheDlqWithoutRetry() throws Exception {
        var messages = mock(InboundMessageStore.class);
        when(messages.claim("message_1")).thenReturn(true);
        var tasks = mock(TaskService.class);
        var imports = mock(MaterialImportService.class);
        var task = new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT, TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null,
            null, null, 0, "UPLOADED", null, null, 0, null, Instant.EPOCH, null, null, Instant.EPOCH);
        when(tasks.getTaskStatus("task_1")).thenReturn(task);
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Original asset could not be parsed"))
            .when(imports).consumeQueuedMaterialImport("mat_1", "task_1");
        var template = mock(RabbitTemplate.class);
        var deadLetters = mock(DeadLetterReplayService.class);
        var router = new ProcessingFailureRouter(template, new RetryPolicy(3), deadLetters,
            () -> new CorrelationData.Confirm(true, null));
        var channel = mock(Channel.class);
        var properties = new MessageProperties();
        properties.setMessageId("message_1");
        properties.setConsumerQueue("document.processing");
        properties.setDeliveryTag(42L);
        properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, "task_1");
        properties.setHeader(TaskDispatchMessageHandler.STAGE_HEADER, "UPLOADED");
        var endpoint = new RabbitProcessingMessageEndpoint(messages, immediateTransactions(),
            new TaskDispatchMessageHandler(tasks, imports), router);

        endpoint.consume(new Message("{}".getBytes(), properties), channel);

        verify(template).send(org.mockito.ArgumentMatchers.eq("suilearn.processing.dlx"),
            org.mockito.ArgumentMatchers.eq("document.processing"), org.mockito.ArgumentMatchers.any(Message.class),
            org.mockito.ArgumentMatchers.any(CorrelationData.class));
        verify(template, never()).send(org.mockito.ArgumentMatchers.eq("suilearn.processing.retry"),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Message.class),
            org.mockito.ArgumentMatchers.any(CorrelationData.class));
        verify(deadLetters).record(org.mockito.ArgumentMatchers.any(Message.class),
            org.mockito.ArgumentMatchers.eq(FailureKind.PERMANENT), org.mockito.ArgumentMatchers.any(IllegalArgumentException.class));
        verify(channel).basicAck(42L, false);
    }

    private TransactionBoundary immediateTransactions() {
        return new TransactionBoundary() {
            @Override public <T> T execute(Work<T> work) { return work.run(); }
        };
    }
}
