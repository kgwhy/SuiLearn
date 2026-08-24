package com.suilearn.api.material.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.suilearn.api.config.AsyncProcessingAdmissionGuard;
import com.suilearn.api.knowledgebase.infrastructure.KnowledgeBaseStore;
import com.suilearn.api.material.MaterialChunker;
import com.suilearn.api.material.MaterialParser;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.EmbeddingStatus;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.persistence.entity.DocumentBlockEntity;
import com.suilearn.api.persistence.entity.DocumentRevisionEntity;
import com.suilearn.api.persistence.repository.DocumentBlockJpaRepository;
import com.suilearn.api.persistence.repository.DocumentRevisionJpaRepository;
import com.suilearn.api.rag.index.EmbeddingIndexVersionRecorder;
import com.suilearn.api.retrieval.EmbeddingProvider;
import com.suilearn.api.task.application.TaskExecutor;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class MaterialImportServiceIndexVersionTest {
    @Test
    void embeddingSuccessRecordsReadyIndexVersionAfterMaterialBecomesReady() {
        var materials = mock(MaterialStore.class);
        var recorder = mock(EmbeddingIndexVersionRecorder.class);
        var provider = mock(EmbeddingProvider.class);
        var service = new MaterialImportService(
            mock(KnowledgeBaseStore.class), materials, mock(MaterialChunkStore.class), mock(MaterialParser.class),
            mock(MaterialChunker.class), provider, Clock.fixed(Instant.EPOCH, java.time.ZoneOffset.UTC),
            mock(TaskService.class), mock(TaskExecutor.class), new AsyncProcessingAdmissionGuard(true),
            null, null, null, mock(DocumentRevisionJpaRepository.class), mock(DocumentBlockJpaRepository.class),
            mock(MaterialUploadValidator.class), null, null, null, recorder);
        var material = new LearningMaterial("mat-1", "kb-1", "Notes", MaterialSourceType.TXT, MaterialStatus.READY,
            null, null, null, "", Instant.EPOCH, null);
        var chunk = new MaterialChunk("c1", "kb-1", "mat-1", "text", 0, null,
            List.of(1.0d), EmbeddingStatus.READY, "text-embedding-3-small", 1536);

        service.recordIndexVersion(material, List.of(chunk));

        verify(recorder).recordReadyVersion(eq("kb-1"), eq(chunk), eq(provider), eq("pgvector-hybrid:mat-1"));
    }
}
