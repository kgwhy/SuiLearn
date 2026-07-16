package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.rabbitmq.client.ConnectionFactory;
import com.suilearn.api.material.storage.MinioObjectGateway;
import com.suilearn.api.material.storage.MinioSdkObjectGateway;
import com.suilearn.api.persistence.entity.OutboxEventEntity;
import com.suilearn.api.persistence.repository.InboundMessageJpaRepository;
import com.suilearn.api.persistence.repository.OutboxEventJpaRepository;
import com.suilearn.api.persistence.repository.ProcessingOperationJpaRepository;
import io.minio.MinioClient;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.MessageListenerContainer;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
    "suilearn.rabbitmq.listener-auto-startup=false",
    "spring.task.scheduling.enabled=false",
    "suilearn.async-processing.enabled=true"
})
class DurablePipelineTestcontainersIntegrationTest {
    private static final String MINIO_ACCESS_KEY = "minioadmin";
    private static final String MINIO_SECRET_KEY = "minioadmin";

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("suilearn")
        .withUsername("suilearn")
        .withPassword("suilearn");

    @Container
    static final RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @Container
    static final GenericContainer<?> minio = new GenericContainer<>("minio/minio:RELEASE.2025-04-22T22-12-26Z")
        .withExposedPorts(9000)
        .withEnv("MINIO_ROOT_USER", MINIO_ACCESS_KEY)
        .withEnv("MINIO_ROOT_PASSWORD", MINIO_SECRET_KEY)
        .withCommand("server", "/data");

    @Autowired private OutboxEventJpaRepository outboxEvents;
    @Autowired private InboundMessageJpaRepository inboundMessages;
    @Autowired private ProcessingOperationJpaRepository operations;
    @Autowired private PersistentProcessingOperationClaims operationClaims;
    @Autowired private RabbitTemplate rabbitTemplate;
    @Autowired private RabbitListenerEndpointRegistry listenerRegistry;
    @MockBean private OutboxRecoveryScheduler outboxRecoveryScheduler;
    @MockBean private ProcessingMessageHandler processingMessageHandler;

    @DynamicPropertySource
    static void configureContainers(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("suilearn.minio.endpoint", () -> "http://" + minio.getHost() + ":" + minio.getMappedPort(9000));
        registry.add("suilearn.minio.access-key", () -> MINIO_ACCESS_KEY);
        registry.add("suilearn.minio.secret-key", () -> MINIO_SECRET_KEY);
        registry.add("suilearn.minio.bucket", () -> "test-assets");
    }

    @BeforeAll
    static void prepareTopology() throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        try (var connection = factory.newConnection(); var channel = connection.createChannel()) {
            channel.exchangeDeclare("test.pipeline", "direct", true);
            channel.exchangeDeclare("test.pipeline.dlx", "direct", true);
            channel.queueDeclare("test.pipeline.main", true, false, false, java.util.Map.of(
                "x-dead-letter-exchange", "test.pipeline.dlx",
                "x-dead-letter-routing-key", "dead"
            ));
            channel.queueDeclare("test.pipeline.dlq", true, false, false, java.util.Map.of());
            channel.queueBind("test.pipeline.main", "test.pipeline", "main");
            channel.queueBind("test.pipeline.dlq", "test.pipeline.dlx", "dead");
        }
    }

    @AfterAll
    static void cleanupTopology() throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        try (var connection = factory.newConnection(); var channel = connection.createChannel()) {
            channel.queueDelete("test.pipeline.main");
            channel.queueDelete("test.pipeline.dlq");
            channel.exchangeDelete("test.pipeline");
            channel.exchangeDelete("test.pipeline.dlx");
        }
    }

    @Test
    void persistsOutboxAndReusesCompletedOperationAcrossFreshServiceInstances() {
        var now = Instant.parse("2026-07-16T00:00:00Z");
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var eventId = "outbox_" + UUID.randomUUID();
        outboxEvents.save(OutboxEventEntity.pending(eventId, "task_1", "PARSING", "idempotency_" + eventId, "{}", now));

        var dispatcher = new PersistentOutboxDispatcher(outboxEvents, ignored -> false, new RetryPolicy(2), clock);
        dispatcher.dispatchDue();
        assertThat(outboxEvents.findById(eventId)).hasValueSatisfying(event -> assertThat(event.state()).isEqualTo("RETRY_WAIT"));

        var operationKey = "ocr:revision_1:page_1:" + UUID.randomUUID();
        var first = operationClaims.claim(operationKey, "task_1", "OCR", "tesseract-v1");
        operationClaims.complete(first.operationId(), "ocr-asset:page_1");

        var replay = operationClaims.claim(operationKey, "task_1", "OCR", "tesseract-v1");

        assertThat(replay.disposition()).isEqualTo(OperationClaimDisposition.REUSE_COMPLETED);
        assertThat(replay.resultReference()).isEqualTo("ocr-asset:page_1");
    }

    @Test
    void redeliversUnacknowledgedMessagesAndRoutesRejectedMessagesToDlq() throws Exception {
        var factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        try (var connection = factory.newConnection(); var channel = connection.createChannel()) {
            channel.basicPublish("test.pipeline", "main", null, "duplicate".getBytes(StandardCharsets.UTF_8));
            var firstDelivery = channel.basicGet("test.pipeline.main", false);
            assertThat(firstDelivery).isNotNull();
            channel.basicNack(firstDelivery.getEnvelope().getDeliveryTag(), false, true);

            var redelivery = channel.basicGet("test.pipeline.main", false);
            assertThat(redelivery).isNotNull();
            assertThat(redelivery.getEnvelope().isRedeliver()).isTrue();
            channel.basicNack(redelivery.getEnvelope().getDeliveryTag(), false, false);

            var deadLetter = waitForDelivery(channel, "test.pipeline.dlq");
            assertThat(deadLetter).isNotNull();
            assertThat(new String(deadLetter.getBody(), StandardCharsets.UTF_8)).isEqualTo("duplicate");
            channel.basicAck(deadLetter.getEnvelope().getDeliveryTag(), false);
        }
    }

    @Test
    void processesDuplicateMessageOnlyOnceWhenTheActualSpringConsumerIsRestarted() throws Exception {
        String messageId = "consumer_restart_" + UUID.randomUUID();
        MessageListenerContainer listener = listenerRegistry.getListenerContainer("suilearnProcessingMessageEndpoint");
        assertThat(listener).isNotNull();

        try {
            listener.start();
            waitUntil("Spring Rabbit consumer to start", listener::isRunning);
            publishProcessingMessage(messageId);
            org.mockito.Mockito.verify(processingMessageHandler, org.mockito.Mockito.timeout(5_000).times(1))
                .handle(org.mockito.ArgumentMatchers.argThat(message -> messageId.equals(message.getMessageProperties().getMessageId())));
            assertThat(inboundMessages.findByMessageId(messageId)).isPresent();

            listener.stop();
            waitUntil("Spring Rabbit consumer to stop", () -> !listener.isRunning());
            publishProcessingMessage(messageId);

            listener.start();
            waitUntil("restarted Spring Rabbit consumer to start", listener::isRunning);
            org.mockito.Mockito.verify(processingMessageHandler, org.mockito.Mockito.after(1_000).times(1))
                .handle(org.mockito.ArgumentMatchers.argThat(message -> messageId.equals(message.getMessageProperties().getMessageId())));
            assertThat(inboundMessages.findByMessageId(messageId)).isPresent();
        } finally {
            listener.stop();
            inboundMessages.findByMessageId(messageId).ifPresent(inboundMessages::delete);
            org.mockito.Mockito.clearInvocations(processingMessageHandler);
        }
    }

    @Test
    void recoversBrokerAndReusesCompletedOcrPageWhileClaimingOnlyInterruptedPage() throws Exception {
        var taskId = "task_" + UUID.randomUUID();
        var revisionId = "revision_" + UUID.randomUUID();
        var completedPageKey = "ocr:" + revisionId + ":page-1:tesseract-v1";
        var interruptedPageKey = "ocr:" + revisionId + ":page-2:tesseract-v1";
        var recoveryQueue = "test.pipeline.recovery." + UUID.randomUUID();
        var routingKey = "recovery." + UUID.randomUUID();
        var completed = operationClaims.claim(completedPageKey, taskId, "OCR", "tesseract-v1");
        operationClaims.complete(completed.operationId(), "ocr-asset:page-1");
        var interrupted = operationClaims.claim(interruptedPageKey, taskId, "OCR", "tesseract-v1");
        boolean brokerStopped = false;

        try {
            try (var connection = rabbitConnectionFactory().newConnection(); var channel = connection.createChannel()) {
                channel.queueDeclare(recoveryQueue, true, false, false, java.util.Map.of());
                channel.queueBind(recoveryQueue, "test.pipeline", routingKey);
                channel.basicPublish("test.pipeline", routingKey,
                    com.rabbitmq.client.MessageProperties.PERSISTENT_BASIC,
                    interruptedPageKey.getBytes(StandardCharsets.UTF_8));
            }

            assertThat(rabbit.execInContainer("rabbitmqctl", "stop_app").getExitCode()).isZero();
            brokerStopped = true;
            waitUntil("RabbitMQ to reject AMQP connections", () -> !brokerAcceptsConnections());

            assertThat(rabbit.execInContainer("rabbitmqctl", "start_app").getExitCode()).isZero();
            brokerStopped = false;
            waitUntil("RabbitMQ to accept AMQP connections", this::brokerAcceptsConnections);

            try (var connection = rabbitConnectionFactory().newConnection(); var channel = connection.createChannel()) {
                var redeliveredWork = waitForDelivery(channel, recoveryQueue);
                assertThat(new String(redeliveredWork.getBody(), StandardCharsets.UTF_8)).isEqualTo(interruptedPageKey);

                operationClaims.recoverInterrupted();
                var reused = operationClaims.claim(completedPageKey, taskId, "OCR", "tesseract-v1");
                var reclaimed = operationClaims.claim(interruptedPageKey, taskId, "OCR", "tesseract-v1");

                assertThat(reused.disposition()).isEqualTo(OperationClaimDisposition.REUSE_COMPLETED);
                assertThat(reused.operationId()).isEqualTo(completed.operationId());
                assertThat(reused.resultReference()).isEqualTo("ocr-asset:page-1");
                assertThat(reclaimed.disposition()).isEqualTo(OperationClaimDisposition.CLAIMED);
                assertThat(reclaimed.operationId()).isEqualTo(interrupted.operationId());
                channel.basicAck(redeliveredWork.getEnvelope().getDeliveryTag(), false);
            }
        } finally {
            if (brokerStopped) {
                rabbit.execInContainer("rabbitmqctl", "start_app");
                waitUntil("RabbitMQ to recover during cleanup", this::brokerAcceptsConnections);
            }
            if (brokerAcceptsConnections()) {
                try (var connection = rabbitConnectionFactory().newConnection(); var channel = connection.createChannel()) {
                    channel.queueDelete(recoveryQueue);
                }
            }
            operations.deleteById(completed.operationId());
            operations.deleteById(interrupted.operationId());
        }
    }

    @Test
    void storesPromotesAndCleansTemporaryObjectsInPrivateMinio() throws Exception {
        var client = MinioClient.builder()
            .endpoint("http://" + minio.getHost() + ":" + minio.getMappedPort(9000))
            .credentials(MINIO_ACCESS_KEY, MINIO_SECRET_KEY)
            .build();
        MinioObjectGateway gateway = new MinioSdkObjectGateway(client);
        var temporaryKey = "tmp/" + UUID.randomUUID();
        var promotedKey = "assets/" + UUID.randomUUID();
        if (!gateway.bucketExists("test-assets")) gateway.createPrivateBucket("test-assets");
        gateway.putPrivate("test-assets", temporaryKey,
            new ByteArrayInputStream("durable asset".getBytes(StandardCharsets.UTF_8)), "text/plain");
        assertThat(gateway.list("test-assets", "tmp/")).extracting(object -> object.key()).contains(temporaryKey);

        gateway.copy("test-assets", temporaryKey, promotedKey);
        assertThat(new String(gateway.getPrivate("test-assets", promotedKey).readAllBytes(), StandardCharsets.UTF_8))
            .isEqualTo("durable asset");
        gateway.delete("test-assets", temporaryKey);
        assertThat(gateway.list("test-assets", "tmp/")).isEmpty();
        gateway.delete("test-assets", promotedKey);
    }

    private com.rabbitmq.client.GetResponse waitForDelivery(com.rabbitmq.client.Channel channel, String queue) throws Exception {
        var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var delivery = channel.basicGet(queue, false);
            if (delivery != null) return delivery;
            Thread.sleep(25);
        }
        throw new AssertionError("Timed out waiting for RabbitMQ delivery in " + queue);
    }

    private void publishProcessingMessage(String messageId) {
        rabbitTemplate.convertAndSend("suilearn.processing", "document.processing", "{}", message -> {
            message.getMessageProperties().setMessageId(messageId);
            return message;
        });
    }

    private ConnectionFactory rabbitConnectionFactory() {
        var factory = new ConnectionFactory();
        factory.setHost(rabbit.getHost());
        factory.setPort(rabbit.getAmqpPort());
        factory.setConnectionTimeout(250);
        return factory;
    }

    private boolean brokerAcceptsConnections() {
        try (var connection = rabbitConnectionFactory().newConnection(); var channel = connection.createChannel()) {
            return channel.isOpen();
        } catch (Exception ignored) {
            return false;
        }
    }

    private void waitUntil(String condition, CheckedCondition probe) throws Exception {
        var deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (probe.isTrue()) return;
            Thread.sleep(50);
        }
        throw new AssertionError("Timed out waiting for " + condition);
    }

    @FunctionalInterface
    private interface CheckedCondition {
        boolean isTrue() throws Exception;
    }
}
