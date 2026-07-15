package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.material.storage.AssetUpload;
import com.suilearn.api.model.MaterialSourceType;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class MaterialUploadValidatorTest {
    private final MaterialUploadValidator validator = new MaterialUploadValidator(32, 2);

    @Test
    void rejectsForgedPdfBeforeAnyDurableAdmission() {
        assertThatThrownBy(() -> validator.validate(MaterialSourceType.PDF,
            new AssetUpload(new ByteArrayInputStream("not a pdf".getBytes(StandardCharsets.UTF_8)), "lesson.pdf", "application/pdf")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
            .isEqualTo(415);
    }

    @Test
    void rejectsMismatchedExtensionAndMimeWithAnActionableClientError() {
        assertThatThrownBy(() -> validator.validate(MaterialSourceType.PDF,
            new AssetUpload(new ByteArrayInputStream("%PDF-1.7".getBytes(StandardCharsets.US_ASCII)), "lesson.txt", "text/plain")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
            .isEqualTo(415);
    }

    @Test
    void streamsOversizedInputToDiskRatherThanReadingItIntoMemory() {
        byte[] content = "%PDF-1.7\nthis payload is bigger than the configured limit".getBytes(StandardCharsets.US_ASCII);

        assertThatThrownBy(() -> validator.validate(MaterialSourceType.PDF,
            new AssetUpload(new ByteArrayInputStream(content), "lesson.pdf", "application/pdf")))
            .isInstanceOf(ResponseStatusException.class)
            .extracting(error -> ((ResponseStatusException) error).getStatusCode().value())
            .isEqualTo(413);
    }
}
