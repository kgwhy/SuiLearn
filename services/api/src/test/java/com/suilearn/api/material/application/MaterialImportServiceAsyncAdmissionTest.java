package com.suilearn.api.material.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.dto.ImportMaterialRequest;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import org.junit.jupiter.api.Test;

class MaterialImportServiceAsyncAdmissionTest {
    @Test
    void rejectsNewImportBeforeMaterialOrTaskWritesWhenAsyncProcessingIsDisabled() {
        var knowledgeBases = mock(KnowledgeBaseStore.class);
        var materials = mock(MaterialStore.class);
        var tasks = mock(TaskService.class);
        var service = new MaterialImportService(
            knowledgeBases,
            materials,
            mock(MaterialChunkStore.class),
            mock(MaterialParser.class),
            mock(MaterialChunker.class),
            mock(EmbeddingProvider.class),
            Clock.systemUTC(),
            tasks,
            mock(TaskExecutor.class),
            new AsyncProcessingAdmissionGuard(false)
        );

        assertThatThrownBy(() -> service.importMaterial("kb_1", new ImportMaterialRequest(
            "Notes", "notes.txt", MaterialSourceType.TXT, "content"
        )))
            .isInstanceOf(AsyncProcessingAdmissionGuard.AsyncProcessingDisabledException.class)
            .hasMessage("ASYNC_PROCESSING_DISABLED: new material uploads and imports are unavailable");

        verifyNoInteractions(knowledgeBases, materials, tasks);
    }
}
