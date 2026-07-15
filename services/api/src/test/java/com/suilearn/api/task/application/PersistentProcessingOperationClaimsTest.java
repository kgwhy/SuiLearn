package com.suilearn.api.task.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.persistence.entity.ProcessingOperationEntity;
import com.suilearn.api.persistence.repository.ProcessingOperationJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PersistentProcessingOperationClaimsTest {
    @Test
    void returnsCompletedResultInsteadOfClaimingAdapterOperationAgain() {
        var operations = mock(ProcessingOperationJpaRepository.class);
        var completed = ProcessingOperationEntity.completed("operation_1", "ocr:revision_1:page_1:v1", "task_1", "OCR",
            "tesseract-v1", "block_1", Instant.EPOCH);
        when(operations.insertStartedIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ocr:revision_1:page_1:v1"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(0);
        when(operations.findByOperationKey("ocr:revision_1:page_1:v1")).thenReturn(Optional.of(completed));
        var claims = new PersistentProcessingOperationClaims(operations, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        var claim = claims.claim("ocr:revision_1:page_1:v1", "task_1", "OCR", "tesseract-v1");

        assertThat(claim.disposition()).isEqualTo(OperationClaimDisposition.REUSE_COMPLETED);
        assertThat(claim.resultReference()).isEqualTo("block_1");
    }

    @Test
    void returnsExistingOperationWhenConcurrentInsertWinsTheUniqueKey() {
        var operations = mock(ProcessingOperationJpaRepository.class);
        var running = ProcessingOperationEntity.started("operation_1", "ocr:revision_1:page_1:v1", "task_1", "OCR",
            "tesseract-v1", Instant.EPOCH);
        when(operations.insertStartedIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ocr:revision_1:page_1:v1"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(0);
        when(operations.findByOperationKey("ocr:revision_1:page_1:v1")).thenReturn(Optional.of(running));
        var claims = new PersistentProcessingOperationClaims(operations, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        var claim = claims.claim("ocr:revision_1:page_1:v1", "task_1", "OCR", "tesseract-v1");

        assertThat(claim.disposition()).isEqualTo(OperationClaimDisposition.ALREADY_RUNNING);
    }

    @Test
    void reclaimsAnExpiredStartedOperationOnRedeliveryWithoutAnApplicationRestart() {
        var operations = mock(ProcessingOperationJpaRepository.class);
        var operationKey = "ocr:revision_1:page_1:v1";
        var stale = ProcessingOperationEntity.started("operation_1", operationKey, "task_1", "OCR", "tesseract-v1", Instant.EPOCH);
        var now = Instant.parse("2026-07-15T00:03:00Z");
        when(operations.insertStartedIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(operationKey),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any())).thenReturn(0);
        when(operations.findByOperationKey(operationKey)).thenReturn(Optional.of(stale));
        when(operations.reclaimExpiredStarted(org.mockito.ArgumentMatchers.eq(operationKey), org.mockito.ArgumentMatchers.eq(now), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        var claims = new PersistentProcessingOperationClaims(operations, Clock.fixed(now, ZoneOffset.UTC));

        var claim = claims.claim(operationKey, "task_1", "OCR", "tesseract-v1");

        assertThat(claim.disposition()).isEqualTo(OperationClaimDisposition.CLAIMED);
        verify(operations).reclaimExpiredStarted(org.mockito.ArgumentMatchers.eq(operationKey), org.mockito.ArgumentMatchers.eq(now), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void persistsPermanentFailureWithSanitizedErrorAndNeverReschedulesItAfterRestart() {
        var operations = mock(ProcessingOperationJpaRepository.class);
        var failed = ProcessingOperationEntity.started("operation_1", "ocr:revision_1:page_1:v1", "task_1", "OCR",
            "tesseract-v1", Instant.EPOCH);
        when(operations.findById("operation_1")).thenReturn(Optional.of(failed));
        when(operations.insertStartedIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ocr:revision_1:page_1:v1"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(0);
        when(operations.findByOperationKey("ocr:revision_1:page_1:v1")).thenReturn(Optional.of(failed));
        var claims = new PersistentProcessingOperationClaims(operations, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        claims.fail("operation_1", FailureKind.PERMANENT,
            "adapter temporarily unavailable; Authorization: Bearer bearer-secret; apiKey=api-key-secret; "
                + "password=database-secret; body=<html><body>full source</body></html>");
        claims.recoverInterrupted();
        var reclaimed = claims.claim("ocr:revision_1:page_1:v1", "task_1", "OCR", "tesseract-v1");

        assertThat(failed.state()).isEqualTo("PERMANENT_FAILURE");
        assertThat(failed.errorCode()).isEqualTo("PERMANENT_FAILURE");
        assertThat(failed.errorMessage()).isEqualTo("adapter unavailable")
            .doesNotContain("bearer-secret", "api-key-secret", "database-secret", "full source", "<html>");
        assertThat(reclaimed.disposition()).isEqualTo(OperationClaimDisposition.ALREADY_RUNNING);
        verify(operations).findByState("STARTED");
        org.mockito.Mockito.verify(operations, org.mockito.Mockito.never()).restartRetryable(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void persistsRetryableFailureAndAllowsTheNextDeliveryToClaimIt() {
        var operations = mock(ProcessingOperationJpaRepository.class);
        var failed = ProcessingOperationEntity.started("operation_1", "ocr:revision_1:page_1:v1", "task_1", "OCR",
            "tesseract-v1", Instant.EPOCH);
        when(operations.findById("operation_1")).thenReturn(Optional.of(failed));
        when(operations.insertStartedIfAbsent(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("ocr:revision_1:page_1:v1"), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(0);
        when(operations.findByOperationKey("ocr:revision_1:page_1:v1")).thenReturn(Optional.of(failed));
        when(operations.restartRetryable(org.mockito.ArgumentMatchers.eq("ocr:revision_1:page_1:v1"), org.mockito.ArgumentMatchers.eq(Instant.EPOCH), org.mockito.ArgumentMatchers.any())).thenReturn(1);
        var claims = new PersistentProcessingOperationClaims(operations, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

        claims.fail("operation_1", FailureKind.TRANSIENT, "adapter temporarily unavailable");
        var reclaimed = claims.claim("ocr:revision_1:page_1:v1", "task_1", "OCR", "tesseract-v1");

        assertThat(failed.state()).isEqualTo("RETRYABLE");
        assertThat(reclaimed.disposition()).isEqualTo(OperationClaimDisposition.CLAIMED);
        verify(operations).restartRetryable(org.mockito.ArgumentMatchers.eq("ocr:revision_1:page_1:v1"), org.mockito.ArgumentMatchers.eq(Instant.EPOCH), org.mockito.ArgumentMatchers.any());
    }
}
