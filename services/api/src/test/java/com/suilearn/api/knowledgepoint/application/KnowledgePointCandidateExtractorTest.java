package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KnowledgePointCandidateExtractorTest {
    @Test
    void filtersMarkdownFragmentsShortIdsSeparatorsAndGenericLabels() {
        var candidates = KnowledgePointCandidateExtractor.extract("""
            ---
            #](https
            [HashMap resizing](https://example.test/hashmap)
            A1 A2 AF BA E5 E7 E8 E9
            Java Markdown PDF TXT
            JVM TCP SQL API
            StringBuilder HashMap hashCode
            """);

        assertThat(candidates)
            .contains("HashMap resizing", "JVM", "TCP", "SQL", "API", "StringBuilder", "HashMap", "hashCode")
            .doesNotContain("#](https", "---", "A1", "A2", "AF", "BA", "E5", "E7", "E8", "E9",
                "Java", "Markdown", "PDF", "TXT", "https");
    }

    @Test
    void rejectsNoisyAiGeneratedNamesBeforePersistingKnowledgePoints() {
        assertThat(KnowledgePointCandidateExtractor.isUsableName("#](https")).isFalse();
        assertThat(KnowledgePointCandidateExtractor.isUsableName("---")).isFalse();
        assertThat(KnowledgePointCandidateExtractor.isUsableName("A1")).isFalse();
        assertThat(KnowledgePointCandidateExtractor.isUsableName("Java")).isFalse();
        assertThat(KnowledgePointCandidateExtractor.isUsableName("HashMap resizing")).isTrue();
    }
}
