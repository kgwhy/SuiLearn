package com.suilearn.api.rag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.material.document.DocumentParser;
import com.suilearn.api.material.document.TesseractOcrAdapter;
import com.suilearn.api.rag.parsing.DocumentParseEngine;
import com.suilearn.api.rag.parsing.OcrParseEngine;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class ComplexParseEngineTest {
    @Test
    void ocrEngineReturnsTesseractTextAndCleansTempFile() {
        var ocr = mock(TesseractOcrAdapter.class);
        when(ocr.recognize(any(java.nio.file.Path.class), anyString(), anyInt()))
            .thenReturn(new TesseractOcrAdapter.Result("SUCCEEDED", "ocr text", "op"));
        var engine = new OcrParseEngine(ocr);

        var parsed = engine.parse("image/png", "bytes".getBytes(StandardCharsets.UTF_8));

        assertThat(parsed.text()).isEqualTo("ocr text");
        assertThat(parsed.metadata()).containsEntry("ocrStatus", "SUCCEEDED");
    }

    @Test
    void documentEngineRejectsEmptyOrUnsupportedContent() {
        var engine = new DocumentParseEngine("pdf", "application/pdf", "document.pdf", new DocumentParser());
        assertThat(engine.supports("application/pdf")).isTrue();
        assertThatThrownBy(() -> engine.parse("application/pdf", new byte[0]))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
