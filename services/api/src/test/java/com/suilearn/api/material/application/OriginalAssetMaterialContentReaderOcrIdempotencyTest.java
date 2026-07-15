package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.config.SuiLearnProcessingProperties;
import com.suilearn.api.material.document.ExternalProcessRunner;
import com.suilearn.api.material.document.RunningExternalProcess;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.StoredAssetRecord;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import com.suilearn.api.task.application.OperationClaim;
import com.suilearn.api.task.application.OperationClaimDisposition;
import com.suilearn.api.task.application.PersistentProcessingOperationClaims;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class OriginalAssetMaterialContentReaderOcrIdempotencyTest {
    @Test
    void reusesPersistedPrivateOcrPageAssetWithoutInvokingTesseractAgain() throws Exception {
        var assets = mock(MaterialAssetJpaRepository.class);
        var storage = mock(AssetStorage.class);
        var operations = mock(PersistentProcessingOperationClaims.class);
        var material = new LearningMaterial("mat_1", "kb_1", "Scan", MaterialSourceType.PDF, MaterialStatus.PARSING,
            "task_1", null, null, "", Instant.EPOCH, null);
        when(assets.findFirstByMaterialIdAndAssetTypeAndPromotionState("mat_1", "ORIGINAL", "PROMOTED"))
            .thenReturn(Optional.of(MaterialAssetEntity.from(new StoredAssetRecord("asset_1", "private/object", "mat_1", "ORIGINAL",
                "sum", 10L, null, "application/pdf", null, null, com.suilearn.api.material.storage.AssetPromotionState.PROMOTED, "scan.pdf"))));
        when(storage.openPrivate("private/object")).thenReturn(new ByteArrayInputStream(scannedPdf()));
        when(operations.claim(eq("ocr:rev_1:page-1:tesseract-v1"), eq("task_1"), eq("OCR"), eq("tesseract-v1")))
            .thenReturn(new OperationClaim("op_1", OperationClaimDisposition.REUSE_COMPLETED, "ocr-asset:asset_ocr_1"));
        when(assets.findById("asset_ocr_1")).thenReturn(Optional.of(MaterialAssetEntity.from(new StoredAssetRecord(
            "asset_ocr_1", "private/ocr-page", "mat_1", "OCR_PAGE", "ocr-sum", 23L, "rev_1", "text/plain",
            null, null, com.suilearn.api.material.storage.AssetPromotionState.PROMOTED, "ocr-page-1.txt"
        ))));
        when(storage.openPrivate("private/ocr-page")).thenReturn(new ByteArrayInputStream("Recovered persisted page".getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        var adapter = new CountingTesseractAdapter();
        var reader = new OriginalAssetMaterialContentReader(assets, storage, properties(), adapter, operations);

        var parsed = reader.readDocument(material, "rev_1").orElseThrow();

        assertThat(parsed.content()).isEqualTo("Recovered persisted page");
        assertThat(adapter.calls).isZero();
        verify(operations).claim("ocr:rev_1:page-1:tesseract-v1", "task_1", "OCR", "tesseract-v1");
    }

    @Test
    void persistsOnlyPrivateOcrAssetReferenceAndReusesItOnRetry() throws Exception {
        var assets = mock(MaterialAssetJpaRepository.class);
        var storage = mock(AssetStorage.class);
        var operations = mock(PersistentProcessingOperationClaims.class);
        var promotions = mock(AssetPromotionCoordinator.class);
        var material = new LearningMaterial("mat_1", "kb_1", "Scan", MaterialSourceType.PDF, MaterialStatus.PARSING,
            "task_1", null, null, "", Instant.EPOCH, null);
        when(assets.findFirstByMaterialIdAndAssetTypeAndPromotionState("mat_1", "ORIGINAL", "PROMOTED"))
            .thenReturn(Optional.of(MaterialAssetEntity.from(new StoredAssetRecord("asset_1", "private/object", "mat_1", "ORIGINAL",
                "sum", 10L, null, "application/pdf", null, null, com.suilearn.api.material.storage.AssetPromotionState.PROMOTED, "scan.pdf"))));
        when(storage.openPrivate("private/object")).thenReturn(new ByteArrayInputStream(scannedPdf()));
        when(operations.claim(eq("ocr:rev_1:page-1:tesseract-v1"), eq("task_1"), eq("OCR"), eq("tesseract-v1")))
            .thenReturn(new OperationClaim("op_1", OperationClaimDisposition.CLAIMED, null));
        when(promotions.store(any(), eq("mat_1"), eq("OCR_PAGE"))).thenReturn(new StoredAssetRecord(
            "asset_ocr_1", "private/ocr-page", "mat_1", "OCR_PAGE", "ocr-sum", 7L, "rev_1", "text/plain",
            null, null, com.suilearn.api.material.storage.AssetPromotionState.PROMOTED, "ocr-page-1.txt"
        ));

        var reader = newOcrAssetReader(assets, storage, properties(), new CountingTesseractAdapter(), operations, promotions);
        reader.readDocument(material, "rev_1").orElseThrow();

        var reference = ArgumentCaptor.forClass(String.class);
        verify(operations).complete(eq("op_1"), reference.capture());
        assertThat(reference.getValue()).isEqualTo("ocr-asset:asset_ocr_1");
        assertThat(reference.getValue()).doesNotContain("new OCR");
    }

    @Test
    void sourceReaderNeverUsesInputStreamReadAllBytesForOriginalAssets() throws Exception {
        var source = java.nio.file.Files.readString(Path.of("src/main/java/com/suilearn/api/material/application/OriginalAssetMaterialContentReader.java"));

        assertThat(source).doesNotContain("readAllBytes()");
    }

    private static byte[] scannedPdf() throws Exception {
        try (var pdf = new PDDocument(); var output = new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());
            pdf.save(output);
            return output.toByteArray();
        }
    }

    private static SuiLearnProcessingProperties properties() {
        return new SuiLearnProcessingProperties(true, true, 50, 500, 2, 1, true, true, 3,
            120_000, 60_000, 120_000, 30_000, 300_000, "http://minio", "", "", "assets", 16);
    }

    private static OriginalAssetMaterialContentReader newOcrAssetReader(
        MaterialAssetJpaRepository assets, AssetStorage storage, SuiLearnProcessingProperties properties, TesseractOcrAdapter adapter,
        PersistentProcessingOperationClaims operations, AssetPromotionCoordinator promotions
    ) throws Exception {
        var constructor = java.util.Arrays.stream(OriginalAssetMaterialContentReader.class.getConstructors())
            .filter(candidate -> java.util.Arrays.equals(candidate.getParameterTypes(), new Class<?>[] {
                MaterialAssetJpaRepository.class, AssetStorage.class, SuiLearnProcessingProperties.class, TesseractOcrAdapter.class,
                PersistentProcessingOperationClaims.class, AssetPromotionCoordinator.class
            }))
            .findFirst();
        assertThat(constructor).as("OCR reader needs an asset-persistence constructor").isPresent();
        return (OriginalAssetMaterialContentReader) constructor.orElseThrow().newInstance(
            assets, storage, properties, adapter, operations, promotions
        );
    }

    private static final class CountingTesseractAdapter extends TesseractOcrAdapter {
        private int calls;
        private CountingTesseractAdapter() { super("tesseract", unusedRunner(), 1, java.time.Duration.ofSeconds(1), "tesseract-v1"); }
        @Override public Result recognize(Path input, String revisionId, int pageNumber) {
            calls++;
            return new Result("SUCCEEDED", "new OCR", "ocr:" + revisionId + ":page-" + pageNumber + ":tesseract-v1");
        }
        private static ExternalProcessRunner unusedRunner() {
            return command -> new RunningExternalProcess() {
                @Override public boolean await(java.time.Duration timeout) { return true; }
                @Override public int exitCode() { return 0; }
                @Override public String stdout() { return ""; }
                @Override public String stderr() { return ""; }
                @Override public void terminate() { }
            };
        }
    }
}
