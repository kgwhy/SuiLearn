package com.suilearn.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcOperations;

class PostgresLargeObjectTextMigrationTest {
    @Test
    void skipsNonPostgresDatabases() {
        var jdbc = mock(JdbcOperations.class);
        var migration = new PostgresLargeObjectTextMigration(jdbc);

        migration.migrate("GenericDB");

        verify(jdbc, never()).queryForList(anyString(), eq(String.class), any(), any());
        verify(jdbc, never()).execute(anyString());
    }

    @Test
    void convertsOnlyOidColumns() {
        var jdbc = mock(JdbcOperations.class);
        when(jdbc.queryForList(anyString(), eq(String.class), any(), any())).thenReturn(List.of("text"));
        when(jdbc.queryForList(anyString(), eq(String.class), eq("learning_materials"), eq("content")))
            .thenReturn(List.of("oid"));
        var migration = new PostgresLargeObjectTextMigration(jdbc);

        migration.migrate("PostgreSQL");

        verify(jdbc).execute(PostgresLargeObjectTextMigration.conversionSql(
            new PostgresLargeObjectTextMigration.TextColumn("learning_materials", "content")
        ));
    }

    @Test
    void conversionSqlReadsLargeObjectAndStoresPlainText() {
        var sql = PostgresLargeObjectTextMigration.conversionSql(
            new PostgresLargeObjectTextMigration.TextColumn("learning_materials", "content")
        );

        assertThat(sql).isEqualTo("""
            alter table "learning_materials" alter column "content" type text using case when "content" is null then null else convert_from(lo_get("content"), 'UTF8') end\
            """);
    }
}
