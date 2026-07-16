package com.suilearn.api.task.application;

import java.util.concurrent.TimeUnit;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

public final class RabbitOutboxBrokerPublisher implements OutboxBrokerPublisher {
    private static final String EXCHANGE = "suilearn.processing";
    private final RabbitTemplate rabbitTemplate;
    private final ConfirmationAwaiter confirmationAwaiter;

    public RabbitOutboxBrokerPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmationAwaiter = null;
    }

    RabbitOutboxBrokerPublisher(RabbitTemplate rabbitTemplate, ConfirmationAwaiter confirmationAwaiter) {
        this.rabbitTemplate = rabbitTemplate;
        this.confirmationAwaiter = confirmationAwaiter;
    }

    @Override
    public boolean publish(DurableOutboxEvent event) {
        var correlation = new CorrelationData(event.id());
        rabbitTemplate.convertAndSend(EXCHANGE, routingKey(event.stage()), event.payload(), message -> {
            message.getMessageProperties().setMessageId(event.id());
            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
            message.getMessageProperties().setHeader(TaskDispatchMessageHandler.TASK_ID_HEADER, event.taskId());
            message.getMessageProperties().setHeader(TaskDispatchMessageHandler.STAGE_HEADER, event.stage());
            message.getMessageProperties().setHeader(ProcessingFailureRouter.RETRY_COUNT_HEADER, event.retryCount());
            return message;
        }, correlation);
        try {
            var confirmation = confirmationAwaiter == null
                ? correlation.getFuture().get(10, TimeUnit.SECONDS)
                : confirmationAwaiter.await();
            return confirmation.isAck();
        } catch (Exception exception) {
            return false;
        }
    }

    private String routingKey(String stage) {
        return switch (stage) {
            case "GENERATING_KNOWLEDGE_POINTS" -> "knowledge-point.generation";
            case "GENERATING_QUESTIONS" -> "question.generation";
            default -> "document.processing";
        };
    }

    @FunctionalInterface
    interface ConfirmationAwaiter {
        CorrelationData.Confirm await() throws Exception;
    }
}
