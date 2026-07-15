package com.suilearn.api.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MaterialSourceTypeContractTest {
    @Test
    void acceptsEveryRawFileTypePublishedByTheMultipartContract() {
        assertThat(MaterialSourceType.values())
            .contains(MaterialSourceType.MARKDOWN, MaterialSourceType.TXT, MaterialSourceType.PDF)
            .extracting(Enum::name)
            .contains("DOC", "DOCX");
    }
}
