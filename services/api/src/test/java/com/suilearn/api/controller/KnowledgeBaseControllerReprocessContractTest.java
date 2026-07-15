package com.suilearn.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.suilearn.api.knowledgebase.application.KnowledgeBaseService;
import com.suilearn.api.knowledgepoint.application.KnowledgePointService;
import com.suilearn.api.material.application.MaterialImportService;
import com.suilearn.api.material.application.MaterialQueryService;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.model.TaskStatus;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialSourceType;
import com.suilearn.api.model.MaterialStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class KnowledgeBaseControllerReprocessContractTest {
    @Test
    void multipartAdmissionMatchesMaterialTaskSubmissionAndSetsLocation() throws Exception {
        var imports = mock(MaterialImportService.class);
        when(imports.importMultipartMaterial(org.mockito.ArgumentMatchers.eq("kb_1"), org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(new LearningMaterial("mat_1", "kb_1", "Notes", MaterialSourceType.TXT, MaterialStatus.UPLOADED,
                "task_1", null, null, "", Instant.EPOCH, null));
        var controller = new KnowledgeBaseController(mock(KnowledgeBaseService.class), imports,
            mock(MaterialQueryService.class), mock(KnowledgePointService.class));

        var response = controller.importMaterialFormData("kb_1", "Notes", "notes.txt", MaterialSourceType.TXT,
            new MockMultipartFile("file", "notes.txt", "text/plain", "notes".getBytes()));

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getHeaders().getLocation()).hasToString("/api/v2/tasks/task_1");
        assertThat(response.getBody()).satisfies(body -> {
            assertThat(body.taskId()).isEqualTo("task_1");
            assertThat(body.status()).isEqualTo(TaskLifecycleStatus.QUEUED);
            assertThat(body.taskHref()).isEqualTo("/api/v2/tasks/task_1");
            assertThat(body.materialId()).isEqualTo("mat_1");
            assertThat(body.materialHref()).isEqualTo("/api/v2/materials/mat_1");
        });
    }

    @Test
    void exposesTheOpenApiReprocessRouteAsAnAsyncTaskSubmission() {
        var imports = mock(MaterialImportService.class);
        when(imports.reprocessMaterial("mat_1")).thenReturn(new TaskStatus("task_1", TaskKind.MATERIAL_IMPORT,
            TaskLifecycleStatus.QUEUED, "kb_1", "mat_1", null, null, null, 0, "REPROCESS", null, null, 0, null,
            Instant.EPOCH, null, null, Instant.EPOCH));
        var controller = new KnowledgeBaseController(mock(KnowledgeBaseService.class), imports,
            mock(MaterialQueryService.class), mock(KnowledgePointService.class));

        var response = controller.reprocessMaterial("mat_1");

        assertThat(response.getStatusCode().value()).isEqualTo(202);
        assertThat(response.getBody().taskId()).isEqualTo("task_1");
        assertThat(response.getBody().taskHref()).isEqualTo("/api/v2/tasks/task_1");
    }
}
