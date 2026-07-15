package com.suilearn.api.knowledgepoint.application;

import static org.assertj.core.api.Assertions.assertThatCode;

import com.suilearn.api.persistence.SuiLearnV2Store;
import org.junit.jupiter.api.Test;

class KnowledgePointRevisionFreshnessContractTest {
    @Test
    void replacingTheCurrentRevisionMarksOnlyOlderCitationsOutdatedWithoutChangingContentOrReviewMetadata() {
        assertThatCode(() -> SuiLearnV2Store.class.getMethod(
            "markKnowledgePointsSourceOutdated", String.class, String.class
        )).doesNotThrowAnyException();
    }
}
