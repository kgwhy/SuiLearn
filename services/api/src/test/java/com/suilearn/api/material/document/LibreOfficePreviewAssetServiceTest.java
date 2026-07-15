package com.suilearn.api.material.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.material.storage.StoredAssetRecord;
import java.nio.file.Files;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class LibreOfficePreviewAssetServiceTest {
    @Test
    void promotesSuccessfulPreviewAsDurablePreviewAssetAndReturnsItsObjectKey() throws Exception {
        var adapter = mock(LibreOfficePreviewAdapter.class);
        var promotions = mock(AssetPromotionCoordinator.class);
        var original = Files.createTempFile("source-", ".docx");
        var generatedPdf = Files.createTempFile("generated-", ".pdf");
        Files.writeString(generatedPdf, "%PDF-1.7");
        when(adapter.preview(original, "rev_1")).thenReturn(
            new LibreOfficePreviewAdapter.Result("SUCCEEDED", original.toString(), generatedPdf.toString(), "preview:rev_1:v1")
        );
        when(promotions.store(any(AssetUpload.class), org.mockito.ArgumentMatchers.eq("mat_1"),
            org.mockito.ArgumentMatchers.eq("PREVIEW"))).thenReturn(
                new StoredAssetRecord("asset_1", "assets/durable-preview.pdf", "mat_1", "PREVIEW", "checksum", 8)
            );

        var result = new LibreOfficePreviewAssetService(adapter, promotions).preview(original, "mat_1", "rev_1");

        assertThat(result.status()).isEqualTo("SUCCEEDED");
        assertThat(result.previewReference()).isEqualTo("assets/durable-preview.pdf");
        assertThat(result.previewReference()).isNotEqualTo(generatedPdf.toString());
        verify(promotions).store(any(AssetUpload.class), org.mockito.ArgumentMatchers.eq("mat_1"),
            org.mockito.ArgumentMatchers.eq("PREVIEW"));
    }

    @Test
    void conversionFailurePreservesTheOriginalWithoutCreatingPreviewAsset() throws Exception {
        var adapter = mock(LibreOfficePreviewAdapter.class);
        var promotions = mock(AssetPromotionCoordinator.class);
        var original = Files.createTempFile("source-", ".docx");
        when(adapter.preview(original, "rev_1")).thenReturn(
            new LibreOfficePreviewAdapter.Result("FAILED", original.toString(), null, "preview:rev_1:v1")
        );

        var result = new LibreOfficePreviewAssetService(adapter, promotions).preview(original, "mat_1", "rev_1");

        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.originalReference()).isEqualTo(original.toString());
        assertThat(result.previewReference()).isNull();
        verifyNoInteractions(promotions);
    }
}
