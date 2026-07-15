package com.suilearn.api.material.document;

import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetUpload;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import org.springframework.stereotype.Service;

/** Promotes only a successfully generated LibreOffice PDF as a durable PREVIEW asset. */
@Service
public class LibreOfficePreviewAssetService {
    private final LibreOfficePreviewAdapter adapter;
    private final AssetPromotionCoordinator promotions;

    public LibreOfficePreviewAssetService(LibreOfficePreviewAdapter adapter, AssetPromotionCoordinator promotions) {
        this.adapter = Objects.requireNonNull(adapter);
        this.promotions = Objects.requireNonNull(promotions);
    }

    public Result preview(Path original, String materialId, String revisionId) {
        var converted = adapter.preview(original, revisionId);
        if (!"SUCCEEDED".equals(converted.status()) || converted.previewReference() == null) {
            return new Result(converted.status(), converted.originalReference(), null, converted.operationKey());
        }
        Path localPreview = Path.of(converted.previewReference());
        try (var stream = Files.newInputStream(localPreview)) {
            var stored = promotions.store(new AssetUpload(stream, "preview.pdf", "application/pdf"), materialId, "PREVIEW");
            return new Result("SUCCEEDED", converted.originalReference(), stored.objectKey(), converted.operationKey());
        } catch (IOException | RuntimeException exception) {
            return new Result("FAILED", converted.originalReference(), null, converted.operationKey());
        } finally {
            deletePreviewWorkspace(localPreview);
        }
    }

    private void deletePreviewWorkspace(Path preview) {
        Path outputDirectory = preview.getParent();
        if (outputDirectory == null || !outputDirectory.getFileName().toString().startsWith("suilearn-preview-output-")) return;
        try (var paths = Files.walk(outputDirectory)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ignored) { }
            });
        } catch (IOException ignored) { }
    }

    public record Result(String status, String originalReference, String previewReference, String operationKey) { }
}
