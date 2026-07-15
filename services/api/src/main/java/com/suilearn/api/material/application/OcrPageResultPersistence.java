package com.suilearn.api.material.application;

import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.task.application.PersistentProcessingOperationClaims;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Persists one successful OCR page outside the import worker transaction so it can be reused after retry. */
@Service
public class OcrPageResultPersistence {
    private final AssetPromotionCoordinator assets;
    private final PersistentProcessingOperationClaims operations;

    public OcrPageResultPersistence(AssetPromotionCoordinator assets, PersistentProcessingOperationClaims operations) {
        this.assets = assets;
        this.operations = operations;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String persist(String operationId, String materialId, int pageNumber, String text) {
        var asset = assets.store(new AssetUpload(
            new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
            "ocr-page-" + pageNumber + ".txt", "text/plain"
        ), materialId, "OCR_PAGE");
        String reference = "ocr-asset:" + asset.id();
        operations.complete(operationId, reference);
        return reference;
    }
}
