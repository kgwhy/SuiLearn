package com.suilearn.api.agent.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.suilearn.api.agent.application.LearningAgentPort.AgentScope;
import com.suilearn.api.ai.application.RetrievalPort;
import com.suilearn.api.model.MaterialChunk;
import com.suilearn.api.model.SearchResult;
import com.suilearn.api.model.SearchResultType;
import com.suilearn.api.model.SourceRef;
import com.suilearn.api.model.SourceType;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class AgentToolContractTest {
    @Test
    void exposesOnlyTheFixedNonRecursiveAgentAndToolTopology() {
        AgentToolCatalog catalog = AgentToolCatalog.fixedMvp();

        assertThat(catalog.allowedActions(AgentRole.SUPERVISOR))
            .containsExactlyInAnyOrder(AgentAction.KNOWLEDGE_RESEARCH, AgentAction.PRACTICE_COACH);
        assertThat(catalog.allowedActions(AgentRole.KNOWLEDGE_RESEARCH))
            .containsExactlyInAnyOrder(AgentAction.SEARCH_KNOWLEDGE, AgentAction.READ_EVIDENCE);
        assertThat(catalog.allowedActions(AgentRole.PRACTICE_COACH)).isEmpty();
        assertThat(catalog.agentRoles()).containsExactlyInAnyOrder(
            AgentRole.SUPERVISOR, AgentRole.KNOWLEDGE_RESEARCH, AgentRole.PRACTICE_COACH);
        assertThat(AgentRole.values()).containsExactly(
            AgentRole.SUPERVISOR, AgentRole.KNOWLEDGE_RESEARCH, AgentRole.PRACTICE_COACH);
    }

    @Test
    void rejectsForbiddenOrCrossAgentActionsWithoutExpandingTheCatalog() {
        AgentToolCatalog catalog = AgentToolCatalog.fixedMvp();

        assertThatThrownBy(() -> catalog.requireAllowed(AgentRole.PRACTICE_COACH, AgentAction.READ_EVIDENCE))
            .isInstanceOf(ForbiddenAgentActionException.class)
            .hasMessage("FORBIDDEN_AGENT_ACTION");
        assertThatThrownBy(() -> catalog.requireAllowed(AgentRole.KNOWLEDGE_RESEARCH, AgentAction.PRACTICE_COACH))
            .isInstanceOf(ForbiddenAgentActionException.class)
            .hasMessage("FORBIDDEN_AGENT_ACTION");
        assertThat(catalog.allowedActions(AgentRole.PRACTICE_COACH)).isEqualTo(Set.of());
    }

    @Test
    void requiresAtLeastOneControlledKnowledgeScope() {
        assertThatThrownBy(() -> new AgentScope(null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("knowledgeBaseId or materialId is required");
        assertThat(new AgentScope("kb-1", null).matches("kb-1", "material-any")).isTrue();
        assertThat(new AgentScope(null, "material-1").matches("kb-any", "material-1")).isTrue();
        assertThat(new AgentScope("kb-1", "material-1").matches("kb-2", "material-1")).isFalse();
    }

    @Test
    void retrievalToolsReuseThePublicPortAndFilterDeletedOrOutOfScopeSources() {
        SourceRef valid = source("source-1", "kb-1", "material-1", false);
        SourceRef deleted = source("source-2", "kb-1", "material-2", true);
        SourceRef outside = source("source-3", "kb-2", "material-3", false);
        RetrievalPort retrieval = new RetrievalPort() {
            @Override
            public List<SearchResult> search(RetrievalRequest request) {
                assertThat(request.knowledgeBaseId()).isEqualTo("kb-1");
                return List.of(new SearchResult("result", SearchResultType.MATERIAL_CHUNK, "title", "summary",
                    0.9, "kb-1", List.of(), List.of(valid, deleted, outside)));
            }

            @Override
            public List<MaterialChunk> retrieveEvidence(RetrievalRequest request, int limit) {
                return List.of(new MaterialChunk("chunk-1", "kb-1", "material-1", "body", 0, valid));
            }
        };
        var tools = new RetrievalEvidenceTools(retrieval);
        AgentScope scope = new AgentScope("kb-1", null);

        List<EvidencePointer> pointers = tools.search(new EvidenceSearchPort.SearchRequest("query", scope, 5));
        assertThat(pointers).singleElement().extracting(EvidencePointer::sourceRef).isEqualTo("source-1");
        assertThat(tools.read(new EvidenceReadPort.ReadRequest("query", pointers.getFirst(), scope)))
            .get().extracting(EvidenceRecord::content).isEqualTo("body");
    }

    private SourceRef source(String id, String knowledgeBaseId, String materialId, boolean deleted) {
        return new SourceRef(SourceType.MATERIAL_CHUNK, id, knowledgeBaseId, "title", materialId,
            "chunk", deleted, "excerpt");
    }
}
