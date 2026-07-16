package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

class ProcessingFailureRouterTest {
    @Test
    void routesTransientFailuresFromShortToLongRetryThenDeadLetterWithIncrementingHeader() {
        var template = mock(RabbitTemplate.class);
        var router = confirmedRouter(template);

        router.route(message("document.processing", null), new IllegalStateException("temporary"));
        router.route(message("document.processing", 1), new IllegalStateException("temporary"));
        router.route(message("document.processing", 2), new IllegalStateException("temporary"));

        var messages = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("suilearn.processing.retry"), eq("document.processing.short"), messages.capture(), any(CorrelationData.class));
        Object retryCount = messages.getValue().getMessageProperties().getHeader("x-suilearn-retry-count");
        assertThat(retryCount).isEqualTo(1);
        verify(template).send(eq("suilearn.processing.retry"), eq("document.processing.long"), any(Message.class), any(CorrelationData.class));
        verify(template).send(eq("suilearn.processing.dlx"), eq("document.processing"), any(Message.class), any(CorrelationData.class));
    }

    @Test
    void routesPermanentFailureDirectlyToDeadLetterWithoutRetry() {
        var template = mock(RabbitTemplate.class);
        var router = confirmedRouter(template);

        router.route(message("document.processing", null), new IllegalArgumentException("bad payload"));

        verify(template).send(eq("suilearn.processing.dlx"), eq("document.processing"), any(Message.class), any(CorrelationData.class));
        org.mockito.Mockito.verifyNoMoreInteractions(template);
    }

    @Test
    void advancesTheDurableTaskOnlyAfterAConfirmedRetryAndLeavesFinalDeadLetterFailed() {
        var template = mock(RabbitTemplate.class);
        var retryState = mock(TaskRetryRoutingState.class);
        var router = new ProcessingFailureRouter(template, new RetryPolicy(3), mock(DeadLetterReplayService.class),
            () -> new CorrelationData.Confirm(true, null), retryState);

        router.route(message("document.processing", null, "task_1"), new IllegalStateException("temporary"));
        router.route(message("document.processing", 2, "task_1"), new IllegalStateException("temporary"));

        verify(retryState).retryAccepted("task_1", 1);
        org.mockito.Mockito.verifyNoMoreInteractions(retryState);
    }

    @Test
    void continuesAnAcceptedDlqReplayRetryCountWhenTheRedeliveredMessageFails() {
        var template = mock(RabbitTemplate.class);
        var retryState = mock(TaskRetryRoutingState.class);
        var router = new ProcessingFailureRouter(template, new RetryPolicy(5), mock(DeadLetterReplayService.class),
            () -> new CorrelationData.Confirm(true, null), retryState);

        router.route(message("document.processing", 3, "task_1"), new IllegalStateException("temporary"));

        var routed = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("suilearn.processing.retry"), eq("document.processing.long"), routed.capture(), any(CorrelationData.class));
        assertThat((Object) routed.getValue().getMessageProperties().getHeader(ProcessingFailureRouter.RETRY_COUNT_HEADER)).isEqualTo(4);
        verify(retryState).retryAccepted("task_1", 4);
    }

    @Test
    void rejectsARejectedBrokerConfirmationSoTheOriginalMessageIsNotAcknowledged() {
        var template = mock(RabbitTemplate.class);
        var router = new ProcessingFailureRouter(template, new RetryPolicy(3), mock(DeadLetterReplayService.class),
            () -> new CorrelationData.Confirm(false, "broker rejected message"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> router.route(message("document.processing", null), new IllegalStateException("temporary")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not confirmed");
    }

    @Test
    void rejectsAConfirmationTimeoutOrBrokerFailure() {
        var template = mock(RabbitTemplate.class);
        var router = new ProcessingFailureRouter(template, new RetryPolicy(3), mock(DeadLetterReplayService.class),
            () -> { throw new java.util.concurrent.TimeoutException("confirm timed out"); });

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> router.route(message("document.processing", null), new IllegalStateException("temporary")))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not confirmed");
    }

    @Test
    void assignsAStableTraceIdAndRoutesMissingMessageIdDirectlyToTheDlq() {
        var template = mock(RabbitTemplate.class);
        var deadLetters = mock(DeadLetterReplayService.class);
        var router = new ProcessingFailureRouter(template, new RetryPolicy(3), deadLetters,
            () -> new CorrelationData.Confirm(true, null));

        router.route(message("document.processing", null), new IllegalArgumentException("Processing message is missing messageId"));

        var routed = ArgumentCaptor.forClass(Message.class);
        verify(template).send(eq("suilearn.processing.dlx"), eq("document.processing"), routed.capture(), any(CorrelationData.class));
        org.assertj.core.api.Assertions.assertThat(routed.getValue().getMessageProperties().getMessageId())
            .startsWith("invalid-message-");
        verify(deadLetters).record(eq(routed.getValue()), eq(FailureKind.PERMANENT), any(IllegalArgumentException.class));
    }

    private ProcessingFailureRouter confirmedRouter(RabbitTemplate template) {
        return new ProcessingFailureRouter(template, new RetryPolicy(3), mock(DeadLetterReplayService.class),
            () -> new CorrelationData.Confirm(true, null));
    }

    private Message message(String queue, Integer retryCount) {
        return message(queue, retryCount, null);
    }

    private Message message(String queue, Integer retryCount, String taskId) {
        var properties = new MessageProperties();
        properties.setConsumerQueue(queue);
        if (retryCount != null) properties.setHeader("x-suilearn-retry-count", retryCount);
        if (taskId != null) properties.setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, taskId);
        return new Message("{}".getBytes(), properties);
    }
}
