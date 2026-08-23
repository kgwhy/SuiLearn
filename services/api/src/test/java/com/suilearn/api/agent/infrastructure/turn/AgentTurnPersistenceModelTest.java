package com.suilearn.api.agent.infrastructure.turn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.suilearn.api.agent.runtime.EventType;
import com.suilearn.api.agent.runtime.StreamEvent;
import com.suilearn.api.agent.runtime.TurnRecord;
import com.suilearn.api.agent.runtime.TurnStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcOperations;

class AgentTurnPersistenceModelTest {
    private final ObjectMapper mapper = JsonMapper.builder().findAndAddModules().build();

    @Test
    void eventIdAndEntityCarryCompositeKeyShape() {
        var id = new TurnEventId("turn_1", 3);
        assertThat(id).isEqualTo(new TurnEventId("turn_1", 3));
        assertThat(id.hashCode()).isEqualTo(new TurnEventId("turn_1", 3).hashCode());
        var entity = new TurnEventEntity(id, "sess_1", EventType.PROGRESS, "{}", Instant.EPOCH);
        assertThat(entity.getId().getTurnId()).isEqualTo("turn_1");
        assertThat(entity.getSessionId()).isEqualTo("sess_1");
    }

    @Test
    void jpaStoreAppendsTerminalEventAndMapsStatus() {
        TurnJpaRepository turns = mock(TurnJpaRepository.class);
        TurnEventJpaRepository events = mock(TurnEventJpaRepository.class);
        SessionMessageJpaRepository messages = mock(SessionMessageJpaRepository.class);
        var store = new JpaTurnStore(turns, events, messages, mapper);

        var turn = new TurnEntity("turn_1", "sess_1", "learner_1", "study_agent", "RUNNING",
            "{\"knowledgeBaseId\":\"kb_1\",\"materialId\":null}", "[]", "msg_1", 1,
            Instant.EPOCH, Instant.EPOCH, null, Instant.EPOCH);
        when(turns.findById("turn_1")).thenReturn(Optional.of(turn));
        when(turns.saveAndFlush(turn)).thenReturn(turn);
        var event = new StreamEvent("turn_1", "sess_1", 2, EventType.DONE, "test", null, "done",
            Map.of(), Instant.EPOCH.plusSeconds(1));
        var eventEntity = new TurnEventEntity(new TurnEventId("turn_1", 2), "sess_1", EventType.DONE,
            "{}", event.ts());
        when(events.saveAndFlush(org.mockito.ArgumentMatchers.any(TurnEventEntity.class))).thenReturn(eventEntity);

        TurnRecord record = store.appendEvent(event);

        assertThat(record.status()).isEqualTo(TurnStatus.COMPLETED);
        assertThat(record.lastSeq()).isEqualTo(2);
        assertThat(turn.getFinishedAt()).isEqualTo(event.ts());
    }

    @Test
    void schemaInitializerIsPostgresOnlyAndIdempotentSqlShape() {
        JdbcOperations jdbc = mock(JdbcOperations.class);
        var initializer = new TurnRuntimeSchemaInitializer(jdbc);

        initializer.migrate("H2");
        verifyNoInteractions(jdbc);

        initializer.migrate("PostgreSQL");
        verify(jdbc, org.mockito.Mockito.times(2)).execute(org.mockito.ArgumentMatchers.startsWith("create index if not exists"));
    }
}
