import assert from "node:assert/strict";
import test from "node:test";
import {
  applyKnowledgePointExtractionTaskStatus,
  submitKnowledgePointExtractionTask
} from "./knowledgePointExtractionTask.ts";

test("submitting knowledge-point extraction refreshes the material and queries the accepted task", async () => {
  const calls = [];

  const submission = await submitKnowledgePointExtractionTask("mat_1", {
    submit: async (materialId) => {
      calls.push(`submit:${materialId}`);
      return { taskId: "task_1", status: "QUEUED", taskHref: "/api/v2/tasks/task_1" };
    },
    loadWorkbench: async () => calls.push("workbench"),
    openMaterial: async (materialId) => calls.push(`material:${materialId}`),
    viewTaskStatus: async (taskId) => calls.push(`status:${taskId}`)
  });

  assert.equal(submission?.taskId, "task_1");
  assert.deepEqual(calls, ["submit:mat_1", "workbench", "material:mat_1", "status:task_1"]);
});

test("a failed extraction task is retained without refreshing material content", async () => {
  const calls = [];
  let stored;

  await applyKnowledgePointExtractionTaskStatus(
    {
      id: "task_1", kind: "KNOWLEDGE_POINT_EXTRACTION", status: "FAILED", materialId: "mat_1",
      currentStep: "AI_EXTRACTION_FAILED", errorCode: "AI_STRUCTURED_OUTPUT_INVALID",
      errorMessage: "AI returned incomplete structured knowledge points", createdAt: "2026-07-17T00:00:00Z", updatedAt: "2026-07-17T00:00:00Z"
    },
    {
      setExtractionTask: (materialId, status) => { stored = { materialId, status }; },
      loadWorkbench: async () => calls.push("workbench"),
      openMaterial: async (materialId) => calls.push(`material:${materialId}`)
    }
  );

  assert.equal(stored.materialId, "mat_1");
  assert.equal(stored.status.errorMessage, "AI returned incomplete structured knowledge points");
  assert.deepEqual(calls, []);
});

test("a successful extraction task refreshes both workbench and selected material", async () => {
  const calls = [];

  await applyKnowledgePointExtractionTaskStatus(
    {
      id: "task_1", kind: "KNOWLEDGE_POINT_EXTRACTION", status: "SUCCEEDED", materialId: "mat_1",
      currentStep: "AI_EXTRACTED", createdAt: "2026-07-17T00:00:00Z", updatedAt: "2026-07-17T00:00:00Z"
    },
    {
      setExtractionTask: (materialId, status) => calls.push(`task:${materialId}:${status.status}`),
      loadWorkbench: async () => calls.push("workbench"),
      openMaterial: async (materialId) => calls.push(`material:${materialId}`)
    }
  );

  assert.deepEqual(calls, ["task:mat_1:SUCCEEDED", "workbench", "material:mat_1"]);
});
