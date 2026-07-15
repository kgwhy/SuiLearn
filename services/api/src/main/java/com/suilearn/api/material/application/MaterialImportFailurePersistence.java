package com.suilearn.api.material.application;

import com.suilearn.api.material.infrastructure.MaterialStore;
import com.suilearn.api.model.LearningMaterial;
import com.suilearn.api.model.MaterialStatus;
import com.suilearn.api.model.TaskKind;
import com.suilearn.api.model.TaskLifecycleStatus;
import com.suilearn.api.task.application.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Commits a failed material-processing attempt even when its broker delivery rolls back. */
@Service
public class MaterialImportFailurePersistence {
    private final MaterialStore materials;
    private final TaskService tasks;

    public MaterialImportFailurePersistence(MaterialStore materials, TaskService tasks) {
        this.materials = materials;
        this.tasks = tasks;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persist(String materialId, String taskId, String errorMessage) {
        var material = materials.find(materialId)
            .orElseThrow(() -> new IllegalArgumentException("Material not found: " + materialId));
        var task = tasks.getTaskStatus(taskId);
        var failed = materials.save(new LearningMaterial(
            material.id(), material.knowledgeBaseId(), material.title(), material.sourceType(), MaterialStatus.FAILED,
            material.importTaskId(), material.embeddingTaskId(), errorMessage, material.content(), material.createdAt(),
            material.deletedAt(), material.currentRevisionId()
        ));
        tasks.updateTask(
            task, TaskLifecycleStatus.FAILED, 100, "FAILED", null, failureCode(task.kind()), errorMessage, failed.id(), null
        );
    }

    private String failureCode(TaskKind kind) {
        return kind == TaskKind.MATERIAL_REPROCESS ? "MATERIAL_REPROCESS_FAILED" : "MATERIAL_IMPORT_FAILED";
    }
}
