import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const apiSource = readFileSync(new URL("./api.ts", import.meta.url), "utf8");
const typesSource = readFileSync(new URL("./types.ts", import.meta.url), "utf8");

test("knowledge-base workbench API client keeps V2 heavy-flow endpoints centralized", () => {
  [
    "/knowledge-bases",
    "/knowledge-bases/${id}/statistics",
    "/knowledge-bases/${knowledgeBaseId}/materials",
    "/materials/${materialId}/extract-knowledge-points",
    "/ai/generated-questions",
    "/ai/generated-contents",
    "/ai/knowledge-point-explanations",
    "/ai/review-suggestions",
    "/ai/notes",
    "/search?",
    "/rag/ask",
    "/tasks/${taskId}",
  ].forEach((endpoint) => {
    assert.match(apiSource, new RegExp(endpoint.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  });
});

test("search client forwards an explicit limit parameter", () => {
  assert.match(apiSource, /search:\s*\(params:\s*\{[^}]*limit\?:\s*number/s);
  assert.match(apiSource, /query\.set\("limit",\s*String\(params\.limit\)\)/);
});

test("generated content review supports save and discard status updates", () => {
  assert.match(apiSource, /reviewGeneratedContent/);
  assert.match(apiSource, /status:\s*GeneratedContentStatus/);
  assert.match(apiSource, /deleteGeneratedContent/);
});

test("resilient knowledge-pipeline client uses multipart submission and task-scoped resources", () => {
  [
    "uploadMaterial",
    "FormData",
    "/materials/${materialId}/reading",
    "/materials/${materialId}/original",
    "/materials/${materialId}/original/download",
    "/materials/${materialId}/reprocess",
    "/materials/${materialId}/knowledge-point-generations",
    "/knowledge-points/${knowledgePointId}",
    "/knowledge-points/${knowledgePointId}/confirm",
    "/knowledge-points/${knowledgePointId}/reject",
    "/knowledge-points/${knowledgePointId}/interview-question-generations",
    "/tasks/${taskId}/knowledge-points",
    "/tasks/${taskId}/question-drafts",
  ].forEach((contract) => {
    assert.match(apiSource, new RegExp(contract.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")));
  });
  assert.match(apiSource, /request<MaterialTaskSubmission>/);
  assert.match(apiSource, /request<TaskSubmission>/);
  assert.match(apiSource, /GenerateKnowledgePointQuestionsRequest/);
});

test("document revision and block projections match the immutable reading contract", () => {
  assert.match(typesSource, /origin:\s*"FILE_IMPORT"\s*\|\s*"REPROCESS"\s*\|\s*"LEGACY_TEXT_MIGRATION"/);
  assert.match(typesSource, /export interface DocumentBlock[\s\S]*revisionId:\s*string/);
  assert.match(typesSource, /sectionPath\?:\s*string\[\]/);
  assert.doesNotMatch(typesSource, /headingPath/);
});
