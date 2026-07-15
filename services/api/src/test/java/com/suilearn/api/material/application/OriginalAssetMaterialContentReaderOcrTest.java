package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.config.SuiLearnProcessingProperties;
import com.suilearn.api.material.document.ExternalProcessRunner;
import com.suilearn.api.material.document.RunningExternalProcess;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.StoredAssetRecord;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;

class OriginalAssetMaterialContentReaderOcrTest {
    @Test
    void invokesInjectedTesseractForTextInsufficientPdfAndMergesTheResultIntoOrderedBlocks() throws Exception {
        var assets = mock(MaterialAssetJpaRepository.class);
        var storage = mock(AssetStorage.class);
        var material = new LearningMaterial("mat_1", "kb_1", "Scan", MaterialSourceType.PDF, MaterialStatus.PARSING,
            "task_1", null, null, "", Instant.EPOCH, null);
        when(assets.findFirstByMaterialIdAndAssetTypeAndPromotionState("mat_1", "ORIGINAL", "PROMOTED"))
            .thenReturn(Optional.of(MaterialAssetEntity.from(mockAsset())));
        when(storage.openPrivate("private/object")).thenReturn(new ByteArrayInputStream(scannedPdf()));

        var adapter = new FakeTesseractAdapter("Recovered OCR page");
        var reader = newReader(assets, storage, adapter);
        var parsed = readDocument(reader, material, "rev_1");

        assertThat(adapter.calls).isEqualTo(1);
        assertThat(parsed.content()).isEqualTo("Recovered OCR page");
        assertThat(parsed.blocks()).extracting(block -> block.order(), block -> block.pageNumber(), block -> block.content())
            .containsExactly(org.assertj.core.groups.Tuple.tuple(0, 1, "Recovered OCR page"));
    }

    private static OriginalAssetMaterialContentReader newReader(
        MaterialAssetJpaRepository assets, AssetStorage storage, TesseractOcrAdapter adapter
    ) throws Exception {
        Constructor<OriginalAssetMaterialContentReader> constructor = constructorWithOcrAdapter()
            .orElseThrow(() -> new AssertionError("OCR-capable reader constructor is missing"));
        return constructor.newInstance(assets, storage, properties(), adapter);
    }

    @SuppressWarnings("unchecked")
    private static Optional<Constructor<OriginalAssetMaterialContentReader>> constructorWithOcrAdapter() {
        try {
            return Optional.of(OriginalAssetMaterialContentReader.class.getConstructor(
                MaterialAssetJpaRepository.class, AssetStorage.class, SuiLearnProcessingProperties.class, TesseractOcrAdapter.class
            ));
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private static OriginalAssetMaterialContentReader.ParsedOriginalDocument readDocument(
        OriginalAssetMaterialContentReader reader, LearningMaterial material, String revisionId
    ) throws Exception {
        Method method;
        try {
            method = OriginalAssetMaterialContentReader.class.getMethod("readDocument", LearningMaterial.class, String.class);
        } catch (NoSuchMethodException missing) {
            throw new AssertionError("OCR-aware reader method is missing", missing);
        }
        return ((Optional<OriginalAssetMaterialContentReader.ParsedOriginalDocument>) method.invoke(reader, material, revisionId))
            .orElseThrow();
    }

    private static StoredAssetRecord mockAsset() {
        return new StoredAssetRecord("asset_1", "private/object", "mat_1", "ORIGINAL", "sum", 10L, null,
            "application/pdf", null, null, com.suilearn.api.material.storage.AssetPromotionState.PROMOTED, "scan.pdf");
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

    private static final class FakeTesseractAdapter extends TesseractOcrAdapter {
        private final String text;
        private int calls;

        private FakeTesseractAdapter(String text) {
            super("tesseract", unusedRunner(), 1, java.time.Duration.ofSeconds(1), "tesseract-v1");
            this.text = text;
        }

        @Override
        public Result recognize(Path input, String revisionId, int pageNumber) {
            calls++;
            return new Result("SUCCEEDED", text, "ocr:" + revisionId + ":page-" + pageNumber + ":tesseract-v1");
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
