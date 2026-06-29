package com.suilearn.api.persistence;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
public class PostgresLargeObjectTextMigration implements ApplicationRunner {
    static final List<TextColumn> TEXT_COLUMNS = List.of(
        new TextColumn("ai_note_drafts", "content"),
        new TextColumn("ai_note_drafts", "source_refs_json"),
        new TextColumn("ai_notes", "content"),
        new TextColumn("ai_notes", "source_refs_json"),
        new TextColumn("answer_records", "user_answer_json"),
        new TextColumn("generated_contents", "source_refs_json"),
        new TextColumn("generated_contents", "knowledge_point_ids_json"),
        new TextColumn("generated_contents", "stem"),
        new TextColumn("generated_contents", "options_json"),
        new TextColumn("generated_contents", "answer_json"),
        new TextColumn("generated_contents", "explanation"),
        new TextColumn("knowledge_points", "source_refs_json"),
        new TextColumn("learning_materials", "content"),
        new TextColumn("material_chunks", "content"),
        new TextColumn("material_chunks", "source_ref_json"),
        new TextColumn("material_chunks", "embedding_json"),
        new TextColumn("questions", "stem"),
        new TextColumn("questions", "knowledge_point_ids_json"),
        new TextColumn("questions", "source_refs_json"),
        new TextColumn("generation_tasks", "result_ref_json")
    );

    private static final String COLUMN_TYPE_SQL = """
        select udt_name
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = ?
          and column_name = ?
        """;

    private final JdbcOperations jdbc;

    public PostgresLargeObjectTextMigration(JdbcOperations jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrate(databaseProductName());
    }

    void migrate(String databaseProductName) {
        if (!isPostgres(databaseProductName)) {
            return;
        }
        for (var column : TEXT_COLUMNS) {
            if (isOidColumn(column)) {
                jdbc.execute(conversionSql(column));
            }
        }
    }

    static boolean isPostgres(String databaseProductName) {
        return "PostgreSQL".equalsIgnoreCase(databaseProductName);
    }

    static String conversionSql(TextColumn column) {
        var tableName = quoteIdentifier(column.tableName());
        var columnName = quoteIdentifier(column.columnName());
        return "alter table " + tableName + " alter column " + columnName + " type text using case "
            + "when " + columnName + " is null then null "
            + "else convert_from(lo_get(" + columnName + "), 'UTF8') end";
    }

    private String databaseProductName() {
        return jdbc.execute((ConnectionCallback<String>) connection -> connection.getMetaData().getDatabaseProductName());
    }

    private boolean isOidColumn(TextColumn column) {
        return jdbc.queryForList(COLUMN_TYPE_SQL, String.class, column.tableName(), column.columnName()).stream()
            .anyMatch(type -> "oid".equalsIgnoreCase(type));
    }

    private static String quoteIdentifier(String identifier) {
        if (!identifier.matches("[a-z_][a-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + identifier);
        }
        return "\"" + identifier + "\"";
    }

    record TextColumn(String tableName, String columnName) {
    }
}
