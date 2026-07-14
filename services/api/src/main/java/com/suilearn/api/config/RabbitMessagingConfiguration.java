package com.suilearn.api.config;

import com.suilearn.api.task.application.MessagingTopology;
import com.suilearn.api.task.application.OutboxBrokerPublisher;
import com.suilearn.api.task.application.PersistentInboundMessageStore;
import com.suilearn.api.task.application.PersistentOutboxDispatcher;
import com.suilearn.api.task.application.PersistentProcessingOperationClaims;
import com.suilearn.api.task.application.RabbitOutboxBrokerPublisher;
import com.suilearn.api.task.application.RetryPolicy;
import com.suilearn.api.persistence.repository.InboundMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import com.suilearn.api.persistence.repository.ProcessingOperationJpaRepository;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableScheduling
public class RabbitMessagingConfiguration {
    static final String PROCESSING_EXCHANGE = "suilearn.processing";
    private static final String RETRY_EXCHANGE = "suilearn.processing.retry";
    private static final String DEAD_LETTER_EXCHANGE = "suilearn.processing.dlx";

    @Bean
    TopicExchange processingExchange() { return new TopicExchange(PROCESSING_EXCHANGE, true, false); }

    @Bean
    TopicExchange retryExchange() { return new TopicExchange(RETRY_EXCHANGE, true, false); }

    @Bean
    TopicExchange deadLetterExchange() { return new TopicExchange(DEAD_LETTER_EXCHANGE, true, false); }

    @Bean
    Declarables processingTopology(SuiLearnProcessingProperties properties) {
        List<Declarable> declarables = new ArrayList<>();
        for (String route : MessagingTopology.queueNames()) {
            var main = QueueBuilder.durable(route)
                .deadLetterExchange(RETRY_EXCHANGE).deadLetterRoutingKey(route + ".short").build();
            var shortRetry = QueueBuilder.durable(route + ".retry.short").ttl((int) properties.rabbitRetryShortDelayMs())
                .deadLetterExchange(PROCESSING_EXCHANGE).deadLetterRoutingKey(route).build();
            var longRetry = QueueBuilder.durable(route + ".retry.long").ttl((int) properties.rabbitRetryLongDelayMs())
                .deadLetterExchange(PROCESSING_EXCHANGE).deadLetterRoutingKey(route).build();
            var deadLetter = QueueBuilder.durable(route + ".dlq").build();
            declarables.add(main);
            declarables.add(shortRetry);
            declarables.add(longRetry);
            declarables.add(deadLetter);
            declarables.add(BindingBuilder.bind(main).to(processingExchange()).with(route));
            declarables.add(BindingBuilder.bind(shortRetry).to(retryExchange()).with(route + ".short"));
            declarables.add(BindingBuilder.bind(longRetry).to(retryExchange()).with(route + ".long"));
            declarables.add(BindingBuilder.bind(deadLetter).to(deadLetterExchange()).with(route));
        }
        return new Declarables(declarables);
    }

    @Bean
    RabbitTemplate rabbitTemplate(CachingConnectionFactory connectionFactory) {
        connectionFactory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        var template = new RabbitTemplate(connectionFactory);
        template.setExchange(PROCESSING_EXCHANGE);
        return template;
    }

    @Bean
    SimpleRabbitListenerContainerFactory processingRabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        @Qualifier("processingConsumerTaskExecutor") ThreadPoolTaskExecutor executor,
        @Value("${suilearn.rabbitmq.listener-auto-startup:true}") boolean autoStartup,
        SuiLearnProcessingProperties properties
    ) {
        var factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setTaskExecutor(executor);
        factory.setConcurrentConsumers(properties.processingConcurrency());
        factory.setMaxConcurrentConsumers(properties.processingConcurrency());
        factory.setDefaultRequeueRejected(false);
        factory.setAutoStartup(autoStartup);
        return factory;
    }

    @Bean
    PersistentInboundMessageStore persistentInboundMessageStore(InboundMessageJpaRepository messages, Clock clock) {
        return new PersistentInboundMessageStore(messages, clock);
    }

    @Bean
    PersistentProcessingOperationClaims persistentProcessingOperationClaims(ProcessingOperationJpaRepository operations, Clock clock) {
        return new PersistentProcessingOperationClaims(operations, clock);
    }

    @Bean
    OutboxBrokerPublisher outboxBrokerPublisher(RabbitTemplate rabbitTemplate) {
        return new RabbitOutboxBrokerPublisher(rabbitTemplate);
    }

    @Bean
    RetryPolicy outboxRetryPolicy(SuiLearnProcessingProperties properties) {
        return new RetryPolicy(properties.maxAttempts());
    }

    @Bean
    PersistentOutboxDispatcher persistentOutboxDispatcher(
        OutboxEventJpaRepository events, OutboxBrokerPublisher publisher, RetryPolicy retryPolicy, Clock clock
    ) {
        return new PersistentOutboxDispatcher(events, publisher, retryPolicy, clock);
    }
}
