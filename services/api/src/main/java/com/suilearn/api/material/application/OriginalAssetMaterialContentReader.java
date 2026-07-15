package com.suilearn.api.material.application;

import com.suilearn.api.material.document.DocumentParser;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.material.storage.AssetPromotionCoordinator;
import com.suilearn.api.material.storage.AssetStorage;
import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.persistence.repository.MaterialAssetJpaRepository;
import com.suilearn.api.task.application.OperationClaimDisposition;
import com.suilearn.api.task.application.PersistentProcessingOperationClaims;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

/** Reads a promoted ORIGINAL asset only from the asynchronous worker path. */
@Service
public class OriginalAssetMaterialContentReader {
    private static final String ORIGINAL = "ORIGINAL";
    private static final String PROMOTED = "PROMOTED";

    private final MaterialAssetJpaRepository assets;
    private final AssetStorage storage;
    private final DocumentParser documentParser;
    private final TesseractOcrAdapter tesseract;
    private final boolean ocrEnabled;
    private final PersistentProcessingOperationClaims operations;
    private final OcrPageResultPersistence ocrPageResults;

    public OriginalAssetMaterialContentReader(MaterialAssetJpaRepository assets, AssetStorage storage) {
        this(assets, storage, 16, 50L * 1024 * 1024, null, false, null, null);
    }

    public OriginalAssetMaterialContentReader(MaterialAssetJpaRepository assets, AssetStorage storage,
                                              com.suilearn.api.config.SuiLearnProcessingProperties properties,
                                              TesseractOcrAdapter tesseract) {
        this(assets, storage, properties.pdfOcrTextDensityThreshold(), maxTextBytes(properties), tesseract, properties.ocrEnabled(), null, null);
    }

    public OriginalAssetMaterialContentReader(MaterialAssetJpaRepository assets, AssetStorage storage,
                                              com.suilearn.api.config.SuiLearnProcessingProperties properties,
                                              TesseractOcrAdapter tesseract, PersistentProcessingOperationClaims operations) {
        this(assets, storage, properties.pdfOcrTextDensityThreshold(), maxTextBytes(properties), tesseract, properties.ocrEnabled(), operations, null);
    }

    public OriginalAssetMaterialContentReader(MaterialAssetJpaRepository assets, AssetStorage storage,
                                              com.suilearn.api.config.SuiLearnProcessingProperties properties,
                                              TesseractOcrAdapter tesseract, PersistentProcessingOperationClaims operations,
                                              AssetPromotionCoordinator ocrAssets) {
        this(assets, storage, properties.pdfOcrTextDensityThreshold(), maxTextBytes(properties), tesseract, properties.ocrEnabled(), operations,
            ocrAssets == null ? null : new OcrPageResultPersistence(ocrAssets, operations));
    }

    @Autowired
    public OriginalAssetMaterialContentReader(MaterialAssetJpaRepository assets, AssetStorage storage,
                                              com.suilearn.api.config.SuiLearnProcessingProperties properties,
                                              TesseractOcrAdapter tesseract, PersistentProcessingOperationClaims operations,
                                              OcrPageResultPersistence ocrPageResults) {
        this(assets, storage, properties.pdfOcrTextDensityThreshold(), maxTextBytes(properties), tesseract, properties.ocrEnabled(), operations, ocrPageResults);
    }

    private OriginalAssetMaterialContentReader(MaterialAssetJpaRepository assets, AssetStorage storage, int ocrTextDensityThreshold,
                                               long maxTextParseBytes,
                                               TesseractOcrAdapter tesseract, boolean ocrEnabled, PersistentProcessingOperationClaims operations,
                                               OcrPageResultPersistence ocrPageResults) {
        this.assets = assets;
        this.storage = storage;
        this.documentParser = new DocumentParser(ocrTextDensityThreshold, maxTextParseBytes);
        this.tesseract = tesseract;
        this.ocrEnabled = ocrEnabled;
        this.operations = operations;
        this.ocrPageResults = ocrPageResults;
    }

    private static long maxTextBytes(com.suilearn.api.config.SuiLearnProcessingProperties properties) {
        return Math.max(1, properties.maxFileSizeMb()) * 1024L * 1024L;
    }

    public boolean hasOriginal(LearningMaterial material) { return originalAsset(material).isPresent(); }

    public Optional<String> read(LearningMaterial material) {
        return readDocument(material).map(ParsedOriginalDocument::content);
    }

    public Optional<ParsedOriginalDocument> readDocument(LearningMaterial material) {
        return readDocument(material, null);
    }

    /** Worker-only route: OCR results are merged before the immutable revision is persisted. */
    public Optional<ParsedOriginalDocument> readDocument(LearningMaterial material, String revisionId) {
        var asset = originalAsset(material);
        if (asset.isEmpty()) {
            return Optional.empty();
        }
        Path original = null;
        try (var stream = storage.openPrivate(asset.orElseThrow().objectKey())) {
            original = Files.createTempFile("suilearn-original-read-", "." + extension(asset.orElseThrow().originalFilename()));
            try (var output = Files.newOutputStream(original)) { stream.transferTo(output); }
            var parsed = documentParser.parse(
                original,
                fileNameFor(asset.orElseThrow().originalFilename(), material.sourceType()),
                mimeTypeFor(asset.orElseThrow().mimeType(), material.sourceType())
            );
            if (!"PARSED".equals(parsed.disposition())) {
                throw new IllegalArgumentException("Original asset could not be parsed");
            }
            var blocks = mergeOcrPages(original, material, revisionId, parsed);
            return Optional.of(new ParsedOriginalDocument(
                blocks.stream().map(DocumentParser.Block::content).collect(java.util.stream.Collectors.joining("\n\n")),
                blocks
            ));
        } catch (IOException exception) {
            throw new IllegalStateException("Original asset could not be read", exception);
        } finally {
            if (original != null) try { Files.deleteIfExists(original); } catch (IOException ignored) { }
        }
    }

    private java.util.List<DocumentParser.Block> mergeOcrPages(
        Path original, LearningMaterial material, String revisionId, DocumentParser.Result parsed
    ) {
        if (parsed.ocrPageNumbers().isEmpty()) {
            return parsed.blocks();
        }
        if (material.sourceType() != MaterialSourceType.PDF || !ocrEnabled || tesseract == null || revisionId == null || revisionId.isBlank()) {
            return parsed.blocks();
        }
        var merged = new ArrayList<>(parsed.blocks());
        try (PDDocument document = Loader.loadPDF(original.toFile())) {
            var renderer = new PDFRenderer(document);
            for (int pageNumber : parsed.ocrPageNumbers()) {
                Path pageImage = Files.createTempFile("suilearn-ocr-page-", ".png");
                try {
                    BufferedImage image = renderer.renderImageWithDPI(pageNumber - 1, 200);
                    if (!ImageIO.write(image, "png", pageImage.toFile())) {
                        throw new IllegalStateException("OCR page rendering failed");
                    }
                    merged.add(new DocumentParser.Block(-1, pageNumber, "", recognizedPageText(pageImage, material, revisionId, pageNumber)));
                } finally {
                    Files.deleteIfExists(pageImage);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException("OCR page could not be prepared", exception);
        }
        var sorted = merged.stream()
            .sorted(Comparator.comparingInt(DocumentParser.Block::pageNumber).thenComparingInt(DocumentParser.Block::order))
            .toList();
        var ordered = new ArrayList<DocumentParser.Block>();
        for (int index = 0; index < sorted.size(); index++) {
            var block = sorted.get(index);
            ordered.add(new DocumentParser.Block(index, block.pageNumber(), block.sectionPath(), block.content()));
        }
        return List.copyOf(ordered);
    }

    private String recognizedPageText(Path pageImage, LearningMaterial material, String revisionId, int pageNumber) {
        String operationKey = "ocr:" + revisionId + ":page-" + pageNumber + ":" + adapterVersion();
        if (operations != null) {
            var claim = operations.claim(operationKey, material.importTaskId(), "OCR", adapterVersion());
            if (claim.disposition() == OperationClaimDisposition.REUSE_COMPLETED) return ocrText(claim.resultReference());
            if (claim.disposition() == OperationClaimDisposition.ALREADY_RUNNING) {
                throw new IllegalStateException("OCR operation is already running for PDF page " + pageNumber);
            }
            try {
                String text = recognize(pageImage, revisionId, pageNumber);
                storeOcrPage(claim.operationId(), text, material, pageNumber);
                return text;
            } catch (RuntimeException exception) {
                operations.failRetryable(claim.operationId(), exception.getMessage());
                throw exception;
            }
        }
        return recognize(pageImage, revisionId, pageNumber);
    }

    private String recognize(Path pageImage, String revisionId, int pageNumber) {
        var result = tesseract.recognize(pageImage, revisionId, pageNumber);
        if (!"SUCCEEDED".equals(result.status()) || result.text() == null || result.text().isBlank()) {
            throw new IllegalStateException("OCR failed for PDF page " + pageNumber);
        }
        return result.text().trim();
    }

    private String adapterVersion() {
        return tesseract.adapterVersion();
    }

    private void storeOcrPage(String operationId, String text, LearningMaterial material, int pageNumber) {
        if (ocrPageResults == null) {
            throw new IllegalStateException("OCR result storage is unavailable");
        }
        ocrPageResults.persist(operationId, material.id(), pageNumber, text);
    }

    private String ocrText(String reference) {
        if (reference == null || !reference.startsWith("ocr-asset:") || reference.length() == "ocr-asset:".length()) {
            throw new IllegalStateException("Persisted OCR result is unavailable");
        }
        String assetId = reference.substring("ocr-asset:".length());
        var asset = assets.findById(assetId)
            .map(entity -> entity.toRecord())
            .filter(record -> "OCR_PAGE".equals(record.assetType()))
            .filter(record -> record.promotionState() == com.suilearn.api.material.storage.AssetPromotionState.PROMOTED)
            .orElseThrow(() -> new IllegalStateException("Persisted OCR result is unavailable"));
        try (var stream = storage.openPrivate(asset.objectKey())) {
            byte[] bytes = stream.readNBytes(1_048_577);
            if (bytes.length > 1_048_576) {
                throw new IllegalStateException("Persisted OCR result exceeds the page limit");
            }
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8).trim();
            if (text.isEmpty()) {
                throw new IllegalStateException("Persisted OCR result is unavailable");
            }
            return text;
        } catch (IOException exception) {
            throw new IllegalStateException("Persisted OCR result is unavailable", exception);
        }
    }

    /** Materializes a private original only for the duration of an external converter invocation. */
    public <T> Optional<T> withOriginalTempFile(LearningMaterial material, Function<Path, T> action) {
        var asset = originalAsset(material);
        if (asset.isEmpty()) return Optional.empty();
        Path temporary = null;
        try (var stream = storage.openPrivate(asset.orElseThrow().objectKey())) {
            temporary = Files.createTempFile("suilearn-original-", "." + extension(asset.orElseThrow().originalFilename()));
            try (var output = Files.newOutputStream(temporary)) { stream.transferTo(output); }
            return Optional.ofNullable(action.apply(temporary));
        } catch (IOException exception) {
            return Optional.empty();
        } finally {
            if (temporary != null) try { Files.deleteIfExists(temporary); } catch (IOException ignored) { }
        }
    }

    private Optional<com.suilearn.api.material.storage.StoredAssetRecord> originalAsset(LearningMaterial material) {
        return assets.findFirstByMaterialIdAndAssetTypeAndPromotionState(material.id(), ORIGINAL, PROMOTED)
            .map(entity -> entity.toRecord());
    }

    private String extension(String filename) {
        if (filename == null) return "bin";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "bin" : filename.substring(dot + 1);
    }

    private String fileNameFor(String originalFilename, MaterialSourceType sourceType) {
        if (originalFilename != null && !originalFilename.isBlank()) {
            return originalFilename;
        }
        return switch (sourceType) {
            case MARKDOWN -> "original.md";
            case TXT -> "original.txt";
            case PDF -> "original.pdf";
            case DOC -> "original.doc";
            case DOCX -> "original.docx";
        };
    }

    private String mimeTypeFor(String mimeType, MaterialSourceType sourceType) {
        if (mimeType != null && !mimeType.isBlank()) {
            return mimeType;
        }
        return switch (sourceType) {
            case MARKDOWN -> "text/markdown";
            case TXT -> "text/plain";
            case PDF -> "application/pdf";
            case DOC -> "application/msword";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }

    public record ParsedOriginalDocument(String content, java.util.List<DocumentParser.Block> blocks) {
    }
}
