package com.suilearn.api.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.entity.MaterialAssetEntity;
import com.suilearn.api.persistence.entity.ProcessingOperationEntity;
import com.suilearn.api.persistence.entity.TaskStatusEntity;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;

class DurableProcessingEntityMappingTest {
    @Test void mapsImmutableRevisionsAndUniqueAdapterOperations() {
        assertThat(DocumentRevisionEntity.class.getAnnotation(Table.class).name()).isEqualTo("document_revisions");
        var operationTable = ProcessingOperationEntity.class.getAnnotation(Table.class);
        assertThat(operationTable.name()).isEqualTo("processing_operations");
        assertThat(operationTable.uniqueConstraints()).anySatisfy(c -> assertThat(c.columnNames()).containsExactly("operationKey"));
    }

    @Test void keepsAssetMetadataAndTaskDurabilityFieldsInTheIncrementalSchema() {
        assertThat(MaterialAssetEntity.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
            .contains("revisionId", "mimeType");
        assertThat(DocumentBlockEntity.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
            .contains("sectionPath");
        assertThat(TaskStatusEntity.class.getDeclaredFields()).extracting(java.lang.reflect.Field::getName)
            .contains("attemptCount", "nextRetryAt", "correlationId", "processingVersion", "idempotencyKey");
    }
}
