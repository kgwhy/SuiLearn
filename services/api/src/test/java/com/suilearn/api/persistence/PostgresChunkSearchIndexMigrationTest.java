package com.suilearn.api.persistence;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.suilearn.api.retrieval.TextSearchTokenizer;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcOperations;

class PostgresChunkSearchIndexMigrationTest {
    private final JdbcOperations jdbc = mock(JdbcOperations.class);
    private final PostgresChunkSearchIndexMigration migration =
        new PostgresChunkSearchIndexMigration(jdbc, new TextSearchTokenizer());

    @Test
    void skipsNonPostgresDatabases() {
        migration.migrate("H2");

        verifyNoInteractions(jdbc);
    }

    @Test
    void createsGeneratedColumnAndGinIndexThenStopsWhenNoRowsToBackfill() {
        when(jdbc.queryForList(anyString())).thenReturn(List.of());

        migration.migrate("PostgreSQL");

        verify(jdbc).execute(contains("add column if not exists search_tsv"));
        verify(jdbc).execute(contains("idx_material_chunks_search_tsv"));
        verify(jdbc, never()).batchUpdate(anyString(), anyList());
    }

    @Test
    void abortsBackfillWithoutThrowingWhenSelectFails() {
        when(jdbc.queryForList(anyString())).thenThrow(new RuntimeException("boom"));

        migration.migrate("PostgreSQL");

        verify(jdbc, never()).batchUpdate(anyString(), anyList());
    }
}
