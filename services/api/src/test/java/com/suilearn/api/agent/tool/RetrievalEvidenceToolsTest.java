package com.suilearn.api.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.suilearn.api.ai.application.RetrievalPort;
import com.suilearn.api.agent.runtime.StudyScope;
import com.suilearn.api.material.infrastructure.MaterialChunkStore;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RetrievalEvidenceToolsTest {
    @Test
    void readFallsBackToExcerptWhenChunkContentIsBlank() {
        var sourceRef = new SourceRef(
            SourceType.MATERIAL_CHUNK,
            "chunk-blank",
            "kb-1",
            "Java",
            "mat-1",
            "chunk-blank",
            false,
            "fallback excerpt",
            "rev-1",
            1,
            "chunk-blank"
        );
        var chunk = new MaterialChunk(
            "chunk-blank",
            "kb-1",
            "mat-1",
            null,
            1,
            sourceRef
        );
        var store = mock(MaterialChunkStore.class);
        when(store.find("chunk-blank")).thenReturn(Optional.of(chunk));
        var port = mock(RetrievalPort.class);
        var tools = new RetrievalEvidenceTools(port, store);
        var pointer = new EvidencePointer(
            "chunk-blank", "chunk-blank", "kb-1", "mat-1", 0.9,
            "rev-1", 1, "chunk-blank", "fallback excerpt"
        );
        var scope = new StudyScope("kb-1", "mat-1");
        var result = tools.read(new EvidenceReadPort.ReadRequest("chunk-blank", pointer, scope));
        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo("fallback excerpt");
    }

    @Test
    void readUsesChunkStoreByIdWhenSearchFindsMaterialChunkPointer() {
        var sourceRef = new SourceRef(
            SourceType.MATERIAL_CHUNK,
            "chunk-1",
            "kb-1",
            "Java",
            "mat-1",
            "chunk-1",
            false,
            "Java exceptions are...",
            "rev-1",
            1,
            "chunk-1"
        );
        var chunk = new MaterialChunk(
            "chunk-1",
            "kb-1",
            "mat-1",
            "Java exceptions are a mechanism...",
            1,
            sourceRef
        );
        var store = mock(MaterialChunkStore.class);
        when(store.find("chunk-1")).thenReturn(Optional.of(chunk));
        var port = mock(RetrievalPort.class);
        when(port.search(any())).thenReturn(List.of(new SearchResult(
            "sr-1",
            SearchResultType.MATERIAL_CHUNK,
            "Java",
            "Java exceptions are...",
            0.9,
            "kb-1",
            List.of(),
            List.of(sourceRef)
        )));

        var tools = new RetrievalEvidenceTools(port, store);
        var scope = new StudyScope("kb-1", "mat-1");
        var pointers = tools.search(new EvidenceSearchPort.SearchRequest("java exceptions", scope, 5));

        assertThat(pointers).hasSize(1);
        var result = tools.read(new EvidenceReadPort.ReadRequest(
            "chunk-1",
            pointers.get(0),
            scope
        ));

        assertThat(result).isPresent();
        assertThat(result.get().content()).isEqualTo("Java exceptions are a mechanism...");
        assertThat(result.get().sourceRef()).isEqualTo("chunk-1");
        verify(store).find("chunk-1");
    }
}
