package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(RabbitListenerEndpointRegistrationTest.ListenerContext.class)
class RabbitListenerEndpointRegistrationTest {
    @Autowired RabbitListenerEndpointRegistry registry;

    @Test
    void registersTheProcessingEndpointWithTheManualAckListenerFactory() {
        assertThat(registry.getListenerContainer("suilearnProcessingMessageEndpoint")).isNotNull();
    }

    @Configuration
    @EnableRabbit
    static class ListenerContext {
        @Bean ConnectionFactory connectionFactory() { return org.mockito.Mockito.mock(ConnectionFactory.class); }
        @Bean(name = "processingRabbitListenerContainerFactory")
        SimpleRabbitListenerContainerFactory factory(ConnectionFactory connectionFactory) {
            var factory = new SimpleRabbitListenerContainerFactory();
            factory.setConnectionFactory(connectionFactory);
            factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
            factory.setAutoStartup(false);
            return factory;
        }
        @Bean InboundMessageStore inboundMessageStore() { return org.mockito.Mockito.mock(InboundMessageStore.class); }
        @Bean TransactionBoundary transactionBoundary() { return new TransactionBoundary() { @Override public <T> T execute(Work<T> work) { return work.run(); } }; }
        @Bean ProcessingMessageHandler processingMessageHandler() { return org.mockito.Mockito.mock(ProcessingMessageHandler.class); }
        @Bean ProcessingFailureRouter processingFailureRouter() { return org.mockito.Mockito.mock(ProcessingFailureRouter.class); }
        @Bean RabbitProcessingMessageEndpoint endpoint(InboundMessageStore messages, TransactionBoundary transactions, ProcessingMessageHandler handler, ProcessingFailureRouter failures) {
            return new RabbitProcessingMessageEndpoint(messages, transactions, handler, failures);
        }
    }
}
