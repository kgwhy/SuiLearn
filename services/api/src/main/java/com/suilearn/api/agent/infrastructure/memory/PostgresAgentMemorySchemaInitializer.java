package com.suilearn.api.agent.infrastructure.memory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

@Component
@Order(30)
@ConditionalOnProperty(prefix = "suilearn.agent", name = "enabled", havingValue = "true")
public class PostgresAgentMemorySchemaInitializer implements ApplicationRunner {
    private final JdbcOperations jdbc;

    public PostgresAgentMemorySchemaInitializer(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (!"PostgreSQL".equalsIgnoreCase(databaseProductName())) {
            return;
        }
        jdbc.execute("create extension if not exists vector");
        jdbc.execute("""
            create table if not exists agent_semantic_memories (
              id varchar(255) primary key,
              learner_id varchar(255) not null,
              memory_type varchar(32) not null,
              content text not null,
              content_fingerprint varchar(64) not null,
              embedding_json text not null,
              confidence double precision not null,
              source_run_id varchar(255) not null,
              source_ref varchar(255) not null,
              created_at timestamptz not null,
              updated_at timestamptz not null
            )
            """);
        jdbc.execute("alter table agent_semantic_memories add column if not exists embedding vector");
        jdbc.execute("create index if not exists idx_agent_memory_learner_type "
            + "on agent_semantic_memories (learner_id, memory_type)");
        jdbc.execute("create unique index if not exists uk_agent_memory_learner_type_fingerprint "
            + "on agent_semantic_memories (learner_id, memory_type, content_fingerprint)");
    }

    private String databaseProductName() {
        return jdbc.execute((ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
    }
}
