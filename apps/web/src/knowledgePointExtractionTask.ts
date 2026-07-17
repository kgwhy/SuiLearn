import type { TaskStatus, TaskSubmission } from "./types";

type SubmissionOperations = {
  submit: (materialId: string) => Promise<TaskSubmission | null>;
  loadWorkbench: () => Promise<void>;
  openMaterial: (materialId: string) => Promise<void>;
  viewTaskStatus: (taskId: string) => Promise<void>;
};

type StatusOperations = {
  setExtractionTask: (materialId: string, status: TaskStatus) => void;
  loadWorkbench: () => Promise<void>;
  openMaterial: (materialId: string) => Promise<void>;
};

export async function submitKnowledgePointExtractionTask(
  materialId: string, operations: SubmissionOperations
): Promise<TaskSubmission | null> {
  const submission = await operations.submit(materialId);
  if (!submission) return null;
  await Promise.all([
    operations.loadWorkbench(),
    operations.openMaterial(materialId),
    operations.viewTaskStatus(submission.taskId)
  ]);
  return submission;
}

export async function applyKnowledgePointExtractionTaskStatus(
  status: TaskStatus, operations: StatusOperations
): Promise<void> {
  if (status.kind !== "KNOWLEDGE_POINT_EXTRACTION" || !status.materialId) return;
  operations.setExtractionTask(status.materialId, status);
  if (status.status === "SUCCEEDED") {
    await Promise.all([operations.loadWorkbench(), operations.openMaterial(status.materialId)]);
  }
}
