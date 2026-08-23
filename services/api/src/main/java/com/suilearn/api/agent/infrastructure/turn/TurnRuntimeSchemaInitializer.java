package com.suilearn.api.agent.infrastructure.turn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

/**
 * Creates Hibernate-managed indexes that {@code ddl-auto=update} may not add for
 * pre-existing tables. All statements are idempotent and PostgreSQL-only.
 */
@Component
@Order(15)
public class TurnRuntimeSchemaInitializer implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TurnRuntimeSchemaInitializer.class);
    private static final String TURN_INDEX_SQL =
        "create index if not exists idx_turn_session_created on turn(session_id, created_at)";
    private static final String EVENT_INDEX_SQL =
        "create index if not exists idx_turn_events_session_created on turn_events(session_id, created_at)";

    private final JdbcOperations jdbc;

    public TurnRuntimeSchemaInitializer(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrate(databaseProductName());
    }

    void migrate(String databaseProductName) {
        if (databaseProductName == null || !databaseProductName.toLowerCase().contains("postgres")) {
            LOG.debug("Turn runtime schema initializer skipped for database {}", databaseProductName);
            return;
        }
        jdbc.execute(TURN_INDEX_SQL);
        jdbc.execute(EVENT_INDEX_SQL);
    }

    private String databaseProductName() {
        return jdbc.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName());
    }
}
