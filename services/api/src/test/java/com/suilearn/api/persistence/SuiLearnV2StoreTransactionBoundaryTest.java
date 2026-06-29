package com.suilearn.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class SuiLearnV2StoreTransactionBoundaryTest {
    @Test
    void lobBackedReadMethodsKeepEntityMappingInsideReadOnlyTransactions() throws NoSuchMethodException {
        var methods = List.of(
            method("listMaterials"),
            method("listMaterials", String.class),
            method("findMaterial", String.class),
            method("listChunks"),
            method("listChunksByMaterial", String.class),
            method("listChunksByScope", String.class, String.class),
            method("findChunk", String.class),
            method("listKnowledgePoints"),
            method("listKnowledgePoints", String.class),
            method("findKnowledgePoint", String.class),
            method("listGeneratedContents"),
            method("findGeneratedContent", String.class),
            method("listQuestions"),
            method("listQuestions", String.class),
            method("listAnswerRecords", String.class),
            method("listAnswerRecordsByQuestion", String.class),
            method("listAiNoteDrafts", String.class),
            method("listAiNotes"),
            method("listAiNotes", String.class),
            method("findTask", String.class),
            method("listTasks")
        );

        for (var method : methods) {
            var transactional = method.getAnnotation(Transactional.class);

            assertThat(transactional)
                .as(method.getName() + " should keep PostgreSQL LOB extraction inside a transaction")
                .isNotNull();
            assertThat(transactional.readOnly())
                .as(method.getName() + " should not start a write transaction")
                .isTrue();
        }
    }

    private Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return SuiLearnV2Store.class.getMethod(name, parameterTypes);
    }
}
