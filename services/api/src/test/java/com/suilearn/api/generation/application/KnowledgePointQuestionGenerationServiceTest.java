package com.suilearn.api.generation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.suilearn.api.ai.AiProvider;
import com.suilearn.api.generation.infrastructure.GeneratedContentStore;
import com.suilearn.api.knowledgepoint.infrastructure.KnowledgePointStore;
import com.suilearn.api.model.GeneratedContentStatus;
import com.suilearn.api.model.GeneratedQuestionDraft;
import com.suilearn.api.model.QuestionType;
import com.suilearn.api.model.SourceType;
import com.suilearn.api.task.application.TaskOutboxSubmissionService;
import com.suilearn.api.task.application.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class KnowledgePointQuestionGenerationServiceTest {
    @Test
    void listsOnlyDraftsActuallyGeneratedByTheRequestedTask() {
        var contents = mock(GeneratedContentStore.class);
        when(contents.list()).thenReturn(List.of(draft("draft_1", "task_1"), draft("draft_2", "task_2")));
        var service = new KnowledgePointQuestionGenerationService(mock(AiProvider.class), mock(KnowledgePointStore.class), contents,
            mock(TaskService.class), mock(TaskOutboxSubmissionService.class), new ObjectMapper(), Clock.systemUTC());

        assertThat(service.listDrafts("task_1")).extracting(GeneratedQuestionDraft::id).containsExactly("draft_1");
    }

    private static GeneratedQuestionDraft draft(String id, String taskId) {
        var now = Instant.parse("2026-07-01T00:00:00Z");
        return new GeneratedQuestionDraft(id, "kb_1", taskId, GeneratedContentStatus.PENDING_REVIEW, List.of(), SourceType.KNOWLEDGE_POINT,
            "kp_1", QuestionType.SHORT_ANSWER, "kp_1", "HashMap", List.of("kp_1"), "stem", List.of(), List.of("answer"), "explanation",
            null, null, now, now, "kp_1", "mat_1", "rev_1", "evidence");
    }
}
