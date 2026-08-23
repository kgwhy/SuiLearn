package com.suilearn.api.rag;

import static org.assertj.core.api.Assertions.assertThat;

import com.suilearn.api.rag.index.EmbeddingSignature;
import com.suilearn.api.rag.index.IndexVersionEntity;
import com.suilearn.api.rag.index.IndexVersionManager;
import com.suilearn.api.rag.index.IndexVersionRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EmbeddingSignatureTest {
    @Test
    void signatureChangesWhenModelChanges() {
        var a = new EmbeddingSignature("openai", "text-embedding-3-small", 1536, "", "");
        var b = new EmbeddingSignature("openai", "text-embedding-3-large", 3072, "", "");
        assertThat(a.hash()).isNotEqualTo(b.hash());
        assertThat(new EmbeddingSignature("openai", "text-embedding-3-small", 1536, "", "").hash()).isEqualTo(a.hash());
    }

    @Test
    void managerReportsNeedsReindexUntilNewReadyVersionExists() {
        var repo = Mockito.mock(IndexVersionRepository.class);
        var old = new IndexVersionEntity("i1", "kb", "oldhash", 1, "pg://old", true, Instant.EPOCH);
        var now = Instant.parse("2026-08-23T08:00:00Z");
        var newSig = new EmbeddingSignature("openai", "new-model", 3072, "", "");
        Mockito.when(repo.findFirstByKnowledgeBaseIdAndSignatureAndReadyTrueOrderByVersionNoDesc("kb", newSig.hash()))
            .thenReturn(Optional.empty());
        Mockito.when(repo.findByKnowledgeBaseIdOrderByVersionNoDesc("kb")).thenReturn(List.of(old));
        var manager = new IndexVersionManager(repo, Clock.fixed(now, ZoneOffset.UTC));

        var status = manager.status("kb", newSig);

        assertThat(status.needsReindex()).isTrue();
        assertThat(status.latest().getId()).isEqualTo("i1");
    }
}
