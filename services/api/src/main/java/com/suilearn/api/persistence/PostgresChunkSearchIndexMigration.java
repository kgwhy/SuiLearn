package com.suilearn.api.persistence;

import com.suilearn.api.retrieval.TextSearchTokenizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Component;

/**
 * 为 text-only RAG 检索建立持久化索引并回填存量 chunk。
 *
 * <p>沿用 {@link PostgresLargeObjectTextMigration} 的运行时迁移模式（项目未使用
 * Flyway/Liquibase，schema 由 Hibernate {@code ddl-auto} 管理）。本组件负责
 * Hibernate 不管理的部分：
 *
 * <ul>
 *   <li>创建生成列 {@code search_tsv = to_tsvector('simple', search_text)}；</li>
 *   <li>创建 GIN 索引 {@code idx_material_chunks_search_tsv}；</li>
 *   <li>为 {@code search_text} 为空的旧 chunk 回填（reindex）。</li>
 * </ul>
 *
 * 全部操作幂等（{@code if not exists} 与 {@code search_text is null} 守卫），
 * 仅在 PostgreSQL 运行，回填按批执行且失败不阻塞应用启动。运行顺序晚于
 * {@link PostgresLargeObjectTextMigration}，确保 {@code content} 已转为 text。
 */
@Component
@Order(20)
public class PostgresChunkSearchIndexMigration implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(PostgresChunkSearchIndexMigration.class);
    private static final int BATCH_SIZE = 500;
    private static final int MAX_BATCHES = 100_000;

    private static final String ADD_GENERATED_COLUMN_SQL = """
        alter table material_chunks
          add column if not exists search_tsv tsvector
          generated always as (to_tsvector('simple', coalesce(search_text, ''))) stored
        """;
    private static final String CREATE_INDEX_SQL = """
        create index if not exists idx_material_chunks_search_tsv
          on material_chunks using gin (search_tsv)
        """;
    private static final String SELECT_BACKFILL_BATCH_SQL =
        "select id, content from material_chunks where search_text is null limit " + BATCH_SIZE;
    private static final String UPDATE_SEARCH_TEXT_SQL =
        "update material_chunks set search_text = ? where id = ?";

    private final JdbcOperations jdbc;
    private final TextSearchTokenizer tokenizer;

    public PostgresChunkSearchIndexMigration(JdbcOperations jdbc, TextSearchTokenizer tokenizer) {
        this.jdbc = jdbc;
        this.tokenizer = tokenizer;
    }

    @Override
    public void run(ApplicationArguments args) {
        migrate(databaseProductName());
    }

    void migrate(String databaseProductName) {
        if (!PostgresLargeObjectTextMigration.isPostgres(databaseProductName)) {
            return;
        }
        jdbc.execute(ADD_GENERATED_COLUMN_SQL);
        jdbc.execute(CREATE_INDEX_SQL);
        backfillSearchText();
    }

    private void backfillSearchText() {
        var total = 0;
        for (var batch = 0; batch < MAX_BATCHES; batch++) {
            List<Map<String, Object>> rows;
            try {
                rows = jdbc.queryForList(SELECT_BACKFILL_BATCH_SQL);
            } catch (RuntimeException exception) {
                LOG.warn("Chunk search_text backfill aborted while selecting rows", exception);
                return;
            }
            if (rows.isEmpty()) {
                break;
            }
            var batchArgs = new ArrayList<Object[]>(rows.size());
            for (var row : rows) {
                var id = (String) row.get("id");
                var content = (String) row.get("content");
                batchArgs.add(new Object[] {tokenizer.searchText(content), id});
            }
            try {
                jdbc.batchUpdate(UPDATE_SEARCH_TEXT_SQL, batchArgs);
            } catch (RuntimeException exception) {
                LOG.warn("Chunk search_text backfill aborted after {} rows", total, exception);
                return;
            }
            total += rows.size();
        }
        if (total > 0) {
            LOG.info("Backfilled search_text for {} chunk(s)", total);
        }
    }

    private String databaseProductName() {
        return jdbc.execute((ConnectionCallback<String>) connection ->
            connection.getMetaData().getDatabaseProductName());
    }
}
