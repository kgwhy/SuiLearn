package com.suilearn.api.agent.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.agent.infrastructure.memory.AgentSemanticMemoryJpaRepository;
import com.suilearn.api.agent.infrastructure.memory.JpaPgVectorSemanticMemoryStore;
import com.suilearn.api.agent.infrastructure.memory.PostgresAgentMemorySchemaInitializer;
import com.suilearn.api.agent.infrastructure.memory.RedisSessionMemoryStore;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcOperations;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest(properties = "suilearn.agent.enabled=true")
@Testcontainers
@Import({JpaPgVectorSemanticMemoryStore.class, AgentMemoryIntegrationTest.RedisFixture.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AgentMemoryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
        DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
        .withExposedPorts(6379);

    @Autowired
    JpaPgVectorSemanticMemoryStore semanticStore;

    @Autowired
    AgentSemanticMemoryJpaRepository repository;

    @Autowired
    JdbcOperations jdbc;

    @Autowired
    RedisSessionMemoryStore redisStore;

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
        properties.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeAll
    static void startRedisConnection() {
        // Container lifecycle is owned by Testcontainers; Spring fixture resolves mapped address lazily.
    }

    @AfterAll
    static void closeRedisConnection() {
        if (RedisFixture.connectionFactory != null) {
            RedisFixture.connectionFactory.destroy();
        }
    }

    @Test
    void redisUsesSlidingTtlBoundedTurnsAndControlledLearnerScanDeletion() throws Exception {
        SessionMemoryKeyFactory keys = new SessionMemoryKeyFactory("suilearn:agent:session:v1");
        SessionMemoryService service = new SessionMemoryService(redisStore, keys, Duration.ofSeconds(4), 20);
        for (int turn = 1; turn <= 23; turn++) {
            service.append("learner-a", "session-a", new SessionTurn("summary-" + turn, null, Instant.now()));
        }
        service.append("learner-b", "session-b", new SessionTurn("other", null, Instant.now()));

        String learnerAKey = keys.key("learner-a", "session-a");
        Long ttlBefore = redisStoreTemplate().getExpire(learnerAKey);
        Thread.sleep(1_100);
        assertThat(service.read("learner-a", "session-a").orElseThrow().turns())
            .hasSize(20).extracting(SessionTurn::summary).startsWith("summary-4");
        assertThat(redisStoreTemplate().getExpire(learnerAKey)).isGreaterThan(ttlBefore - 1);

        assertThat(service.deleteLearner("learner-a")).isEqualTo(1);
        assertThat(service.read("learner-a", "session-a")).isEmpty();
        assertThat(service.read("learner-b", "session-b")).isPresent();
    }

    @Test
    void postgresSchemaIsIdempotentAndVectorRecallFiltersBeforeTopK() {
        PostgresAgentMemorySchemaInitializer initializer = new PostgresAgentMemorySchemaInitializer(jdbc);
        initializer.run(null);
        initializer.run(null);
        save("a-goal", "learner-a", MemoryType.GOAL, List.of(1.0, 0.0));
        save("a-pref", "learner-a", MemoryType.PREFERENCE, List.of(0.99, 0.01));
        save("b-goal", "learner-b", MemoryType.GOAL, List.of(1.0, 0.0));

        List<ScoredSemanticMemory> recalled = semanticStore.recall(
            new SemanticMemoryQuery("learner-a", Set.of(MemoryType.GOAL), 5), List.of(1.0, 0.0), 5);

        assertThat(recalled).singleElement().satisfies(result -> {
            assertThat(result.memory().id()).isEqualTo("a-goal");
            assertThat(result.memory().createdAt()).isNotNull();
            assertThat(result.memory().updatedAt()).isNotNull();
        });
        assertThat(semanticStore.deleteByLearner("learner-a")).isEqualTo(2);
        assertThat(repository.findByLearnerIdAndMemoryTypeIn("learner-b", List.of("GOAL"))).hasSize(1);
    }

    @Test
    void realSemanticStoreIsNotQueriedWhenEmbeddingIsUnavailable() {
        MemoryManager manager = new MemoryManager(null, semanticStore,
            ignored -> EmbeddingResult.unavailable("offline"), new MemoryPromotionPolicy(0.8, 8, 500), 5, Instant::now);

        SemanticRecallResult result = manager.recall("learner-a", Set.of(MemoryType.GOAL), "react");

        assertThat(result.status()).isEqualTo(RecallStatus.LONG_TERM_MEMORY_DEGRADED);
        assertThat(result.memories()).isEmpty();
    }

    private void save(String id, String learner, MemoryType type, List<Double> embedding) {
        semanticStore.save(new AgentSemanticMemory(id, learner, type, id + " durable content", MemoryFingerprint.of(id),
            embedding, 0.9, "run", "topic:" + id, Instant.now(), Instant.now()));
    }

    private StringRedisTemplate redisStoreTemplate() {
        return RedisFixture.template;
    }

    @TestConfiguration
    static class RedisFixture {
        private static StringRedisTemplate template;
        private static LettuceConnectionFactory connectionFactory;

        @Bean
        ObjectMapper agentMemoryObjectMapper() {
            return JsonMapper.builder().findAndAddModules().build();
        }

        @Bean(destroyMethod = "destroy")
        LettuceConnectionFactory agentMemoryRedisConnectionFactory() {
            RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379));
            connectionFactory = new LettuceConnectionFactory(configuration);
            connectionFactory.afterPropertiesSet();
            return connectionFactory;
        }

        @Bean
        RedisSessionMemoryStore redisSessionMemoryStore(LettuceConnectionFactory connectionFactory,
                                                         ObjectMapper objectMapper) {
            template = new StringRedisTemplate(connectionFactory);
            template.afterPropertiesSet();
            return new RedisSessionMemoryStore(template, objectMapper);
        }
    }
}
