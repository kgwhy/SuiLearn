package com.suilearn.api.material.document;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class OcrOperationalMetricsTest {
    @Test
    void recordsPageResultsWithOnlyTheBoundedOutcomeTag() {
        var registry = new SimpleMeterRegistry();
        var metrics = new OcrOperationalMetrics(registry);

        metrics.recordPageResult("TIMED_OUT", 12);

        var counter = registry.find("suilearn.ocr.pages").tag("outcome", "timed_out").counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1);
        assertThat(registry.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags())
            .extracting(tag -> tag.getKey())
            .doesNotContain("materialId", "revisionId", "taskId", "pageNumber", "path", "content", "error"));
    }
}
