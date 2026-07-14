package com.suilearn.api.task.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
final class ProcessingFailureRouter {
    static final String RETRY_COUNT_HEADER = "x-suilearn-retry-count";
    private static final String RETRY_EXCHANGE = "suilearn.processing.retry";
    private static final String DEAD_LETTER_EXCHANGE = "suilearn.processing.dlx";
    private final RabbitTemplate rabbitTemplate;
    private final RetryPolicy retryPolicy;
    private final DeadLetterReplayService deadLetters;
    private final ConfirmationAwaiter confirmationAwaiter;

    @Autowired
    ProcessingFailureRouter(RabbitTemplate rabbitTemplate, RetryPolicy retryPolicy, DeadLetterReplayService deadLetters) {
        this(rabbitTemplate, retryPolicy, deadLetters, null);
    }

    ProcessingFailureRouter(
        RabbitTemplate rabbitTemplate, RetryPolicy retryPolicy, DeadLetterReplayService deadLetters, ConfirmationAwaiter confirmationAwaiter
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.retryPolicy = retryPolicy;
        this.deadLetters = deadLetters;
        this.confirmationAwaiter = confirmationAwaiter;
    }

    void route(Message message, RuntimeException failure) {
        Message traceable = withTraceableMessageId(message);
        String queue = traceable.getMessageProperties().getConsumerQueue();
        if (queue == null || queue.isBlank()) throw new IllegalArgumentException("Processing message has no consumer queue", failure);
        int retries = retryCount(traceable);
        FailureKind kind = failure instanceof IllegalArgumentException ? FailureKind.PERMANENT : FailureKind.TRANSIENT;
        if (retryPolicy.next(retries + 1, kind) == DeliveryDecision.DEAD_LETTER) {
            deadLetters.record(traceable, kind, failure);
            publishConfirmed(DEAD_LETTER_EXCHANGE, queue, MessageBuilder.fromMessage(traceable).build());
            return;
        }
        String suffix = retries == 0 ? ".short" : ".long";
        Message retry = MessageBuilder.fromMessage(traceable).setHeader(RETRY_COUNT_HEADER, retries + 1).build();
        publishConfirmed(RETRY_EXCHANGE, queue + suffix, retry);
    }

    static boolean hasValidMessageId(String messageId) {
        return messageId != null && messageId.matches("[A-Za-z0-9._:-]{1,200}");
    }

    private Message withTraceableMessageId(Message message) {
        if (hasValidMessageId(message.getMessageProperties().getMessageId())) return message;
        return MessageBuilder.fromMessage(message).setMessageId("invalid-message-" + fingerprint(message)).build();
    }

    private String fingerprint(Message message) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            String queue = String.valueOf(message.getMessageProperties().getConsumerQueue());
            digest.update(queue.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(message.getBody());
            return HexFormat.of().formatHex(digest.digest()).substring(0, 32);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to create dead-letter trace identifier", exception);
        }
    }

    private void publishConfirmed(String exchange, String routingKey, Message message) {
        var correlation = new CorrelationData(message.getMessageProperties().getMessageId());
        rabbitTemplate.send(exchange, routingKey, message, correlation);
        try {
            var confirmation = confirmationAwaiter == null
                ? correlation.getFuture().get(10, TimeUnit.SECONDS)
                : confirmationAwaiter.await();
            if (confirmation == null || !confirmation.isAck()) {
                throw new IllegalStateException("Failure route was not confirmed by RabbitMQ");
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("Failure route was not confirmed by RabbitMQ", exception);
        }
    }

    private int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeaders().get(RETRY_COUNT_HEADER);
        if (value instanceof Number number) return Math.max(number.intValue(), 0);
        if (value instanceof String text) {
            try { return Math.max(Integer.parseInt(text), 0); }
            catch (NumberFormatException ignored) { return 0; }
        }
        return 0;
    }

    @FunctionalInterface
    interface ConfirmationAwaiter {
        CorrelationData.Confirm await() throws Exception;
    }
}
