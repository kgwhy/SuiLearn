package com.suilearn.api.task.application;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
final class RabbitProcessingMessageEndpoint {
    private final DurableMessageConsumer consumer;
    private final ProcessingMessageHandler handler;
    private final ProcessingFailureRouter failures;

    RabbitProcessingMessageEndpoint(
        InboundMessageStore messages, TransactionBoundary transactions, ProcessingMessageHandler handler, ProcessingFailureRouter failures
    ) {
        this.consumer = new DurableMessageConsumer(messages, transactions);
        this.handler = handler;
        this.failures = failures;
    }

    @RabbitListener(
        id = "suilearnProcessingMessageEndpoint",
        queues = {"document.processing", "knowledge-point.generation", "question.generation"},
        containerFactory = "processingRabbitListenerContainerFactory"
    )
    void consume(Message message, Channel channel) {
        try {
            String messageId = message.getMessageProperties().getMessageId();
            if (!ProcessingFailureRouter.hasValidMessageId(messageId)) {
                throw new IllegalArgumentException("Processing message is missing or invalid messageId");
            }
            consumer.consume(messageId, () -> {
                handler.handle(message);
                return null;
            }, ignored -> acknowledge(channel, message.getMessageProperties().getDeliveryTag()));
        } catch (RuntimeException exception) {
            failures.route(message, exception);
            acknowledge(channel, message.getMessageProperties().getDeliveryTag());
        }
    }

    private void acknowledge(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException exception) {
            throw new IllegalStateException("RabbitMQ acknowledgement failed", exception);
        }
    }
}
