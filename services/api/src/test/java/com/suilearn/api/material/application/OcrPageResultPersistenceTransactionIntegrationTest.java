package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.StoredAssetRecord;
import com.suilearn.api.task.application.PersistentProcessingOperationClaims;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OcrPageResultPersistenceTransactionIntegrationTest {
    @Test
    void commitsOcrPageAssetAndOperationReferenceBeforeTheOuterWorkerTransactionRollsBack() {
        try (var context = new AnnotationConfigApplicationContext(TransactionTestConfiguration.class)) {
            var transactions = context.getBean(RecordingTransactionManager.class);
            var persistence = context.getBean(OcrPageResultPersistence.class);
            var promotions = context.getBean(AssetPromotionCoordinator.class);
            var operations = context.getBean(PersistentProcessingOperationClaims.class);
            when(promotions.store(any(), eq("mat_1"), eq("OCR_PAGE"))).thenReturn(asset("asset_ocr_1"));

            assertThatThrownBy(() -> new TransactionTemplate(transactions).executeWithoutResult(outer -> {
                assertThat(persistence.persist("op_1", "mat_1", 1, "OCR text")).isEqualTo("ocr-asset:asset_ocr_1");
                throw new WorkerFailure();
            })).isInstanceOf(WorkerFailure.class);

            assertThat(transactions.commits.get()).isEqualTo(1);
            assertThat(transactions.rollbacks.get()).isEqualTo(1);
            verify(operations).complete("op_1", "ocr-asset:asset_ocr_1");
        }
    }

    private static StoredAssetRecord asset(String id) {
        return new StoredAssetRecord(id, "assets/ocr", "mat_1", "OCR_PAGE", "checksum", 8L, "rev_1", "text/plain",
            null, null, com.suilearn.api.material.storage.AssetPromotionState.PROMOTED, "ocr-page-1.txt");
    }

    @Configuration
    @EnableTransactionManagement
    static class TransactionTestConfiguration {
        @Bean RecordingTransactionManager transactionManager() { return new RecordingTransactionManager(); }
        @Bean AssetPromotionCoordinator assetPromotionCoordinator() { return mock(AssetPromotionCoordinator.class); }
        @Bean PersistentProcessingOperationClaims persistentProcessingOperationClaims() { return mock(PersistentProcessingOperationClaims.class); }
        @Bean OcrPageResultPersistence ocrPageResultPersistence(AssetPromotionCoordinator promotions,
                                                                 PersistentProcessingOperationClaims operations) {
            return new OcrPageResultPersistence(promotions, operations);
        }
    }

    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        final AtomicInteger commits = new AtomicInteger();
        final AtomicInteger rollbacks = new AtomicInteger();
        private final ThreadLocal<RecordingTransaction> current = new ThreadLocal<>();

        @Override protected Object doGetTransaction() {
            return current.get() == null ? new RecordingTransaction() : current.get();
        }
        @Override protected boolean isExistingTransaction(Object transaction) {
            return ((RecordingTransaction) transaction).active;
        }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            var recorded = (RecordingTransaction) transaction;
            recorded.active = true;
            current.set(recorded);
        }
        @Override protected Object doSuspend(Object transaction) {
            var suspended = current.get();
            current.remove();
            return suspended;
        }
        @Override protected void doResume(Object transaction, Object suspendedResources) {
            current.set((RecordingTransaction) suspendedResources);
        }
        @Override protected void doCommit(DefaultTransactionStatus status) {
            commits.incrementAndGet();
            ((RecordingTransaction) status.getTransaction()).active = false;
            current.remove();
        }
        @Override protected void doRollback(DefaultTransactionStatus status) {
            rollbacks.incrementAndGet();
            ((RecordingTransaction) status.getTransaction()).active = false;
            current.remove();
        }

        private static final class RecordingTransaction { private boolean active; }
    }

    private static final class WorkerFailure extends RuntimeException { }
}
