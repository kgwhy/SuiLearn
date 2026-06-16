import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const apiSource = readFileSync(new URL("./api.ts", import.meta.url), "utf8");

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
