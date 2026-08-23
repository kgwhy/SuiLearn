package com.suilearn.api.rag.parsing;

import com.suilearn.api.material.document.TesseractOcrAdapter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class OcrParseEngine implements ParseEngine {
    private final TesseractOcrAdapter ocr;

    public OcrParseEngine(TesseractOcrAdapter ocr) {
        this.ocr = ocr;
    }

    @Override public String name() { return "ocr"; }
    @Override public boolean supports(String mediaType) { return mediaType != null && mediaType.startsWith("image/"); }
    @Override public ParsedDocument parse(String mediaType, byte[] content) {
        if (!supports(mediaType)) throw new IllegalArgumentException("unsupported media type: " + mediaType);
        Path temp = null;
        try {
            temp = Files.createTempFile("suilearn-ocr-", ".png");
            Files.write(temp, content);
            var result = ocr.recognize(temp, "rag-parse", 1);
            if (!"SUCCEEDED".equals(result.status())) {
                throw new IllegalArgumentException("OCR failed: " + result.status());
            }
            return new ParsedDocument(mediaType, result.text(), Map.of("ocrStatus", result.status()));
        } catch (IOException exception) {
            throw new IllegalArgumentException("OCR temporary file failed", exception);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }
}
