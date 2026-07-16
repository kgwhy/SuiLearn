import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const appSource = readFileSync(new URL("./App.tsx", import.meta.url), "utf8");

test("asynchronous material submission opens its detail and tracks the returned task", () => {
  assert.match(appSource, /setSection\("materials"\)/);
  assert.match(appSource, /openMaterial\(submission\.materialId\)/);
  assert.match(appSource, /viewTaskStatus\(submission\.taskId\)/);
});

test("material workbench uploads originals and exposes durable reading recovery actions", () => {
  assert.doesNotMatch(appSource, /\.file\.text\(\)/);
  assert.match(appSource, /api\.uploadMaterial/);
  assert.match(appSource, /onDrop/);
  assert.match(appSource, /\.doc,.docx/);
  assert.match(appSource, /api\.getMaterialReading/);
  assert.match(appSource, /api\.materialOriginalHref/);
  assert.match(appSource, /api\.materialDownloadHref/);
  assert.match(appSource, /api\.reprocessMaterial/);
  assert.match(appSource, /RETRY_WAIT/);
  assert.match(appSource, /scrollIntoView/);
});

test("material detail renders full content before falling back to preview text", () => {
  assert.match(appSource, /detail\.content\s*\?\?\s*detail\.contentPreview/);
  assert.match(appSource, /className="material-content-reader"/);
});

test("material body and knowledge point text are normalized before display so encoded anchors are readable", () => {
  assert.match(appSource, /function readableMaterialText/);
  assert.match(appSource, /decodeURIComponent/);
  assert.match(appSource, /<pre>\{readableMaterialText\(content\)\}<\/pre>/);
  assert.match(appSource, /readableUrlLabel/);
  assert.match(appSource, /\{readableMaterialText\(point\.name\)\}/);
  assert.match(appSource, /point\.description \? readableMaterialText\(point\.description\) : "暂无描述"/);
});

test("extracted knowledge points are rendered as readable details instead of keyword-only chips", () => {
  assert.match(appSource, /KnowledgePointSummaryList/);
  assert.match(appSource, /points=\{props\.materialDetail\.extractedKnowledgePoints\s*\?\?\s*\[\]\}/);
});

test("imported material detail only renders full content and knowledge points, not chunk excerpts", () => {
  assert.doesNotMatch(appSource, /className="chunk-list"/);
  assert.doesNotMatch(appSource, /materialChunkEmbeddingMeta/);
  assert.doesNotMatch(appSource, /ref\.excerpt/);
});

test("knowledge point extraction stores and displays the extraction task status", () => {
  assert.match(appSource, /const \[extractionTasks, setExtractionTasks]/);
  assert.match(appSource, /setExtractionTasks\(\(current\) => \(\{ \.\.\.current, \[materialId]: extraction\.task \}\)\)/);
  assert.match(appSource, /<TaskStatusCard task=\{props\.extractionTasks\[props\.materialDetail\.id\]\} \/>/);
});

test("knowledge-point workbench shows structured list and detail fields with citation jumps and review actions", () => {
  assert.match(appSource, /function KnowledgePointSummaryList/);
  assert.match(appSource, /point\.legacy \? readableMaterialText\(point\.name\) : readableMaterialText\(point\.title\)/);
  assert.match(appSource, /point\.legacy \? readableMaterialText\(point\.description\) : readableMaterialText\(point\.shortSummary\)/);
  assert.match(appSource, /point\.definition/);
  assert.match(appSource, /point\.principles/);
  assert.match(appSource, /point\.applicationScenarios/);
  assert.match(appSource, /point\.pitfalls/);
  assert.match(appSource, /openCitation\(citation\)/);
  assert.match(appSource, /api\.confirmKnowledgePoint/);
  assert.match(appSource, /api\.rejectKnowledgePoint/);
});

test("only confirmed, current structured knowledge points can submit default or advanced interview questions", () => {
  assert.match(appSource, /function isKnowledgePointQuestionEligible/);
  assert.match(appSource, /point\.legacy === false/);
  assert.match(appSource, /point\.status === "CONFIRMED"/);
  assert.match(appSource, /!point\.sourceOutdated/);
  assert.match(appSource, /citation\.deleted !== true/);
  assert.match(appSource, /generateQuestions\(point, \{ count: 1, difficulty: "MEDIUM", questionType: "SHORT_ANSWER" \}\)/);
  assert.match(appSource, /count.*1.*10/);
  assert.match(appSource, /更多设置/);
  assert.match(appSource, /generationForm\.sourceKind === "knowledgePoint"\)\s*\{\s*if \(!\("legacy" in selectedSource\) \|\| !isKnowledgePointQuestionEligible\(selectedSource\)\)/);
});

test("knowledge-point question generation loads task-scoped drafts and keeps attribution immutable during review", () => {
  assert.match(appSource, /api\.listTaskQuestionDrafts\(taskId\)/);
  assert.match(appSource, /questionDraftsByTask/);
  assert.match(appSource, /api\.reviewKnowledgePointQuestionDraft/);
  assert.match(appSource, /contentKind:\s*"KNOWLEDGE_POINT_INTERVIEW_QUESTION"/);
  assert.match(appSource, /action:\s*status === "SAVED" \? "SAVE" : "DISCARD"/);
  assert.doesNotMatch(appSource, /api\.reviewKnowledgePointQuestionDraft\([\s\S]{0,420}(knowledgePointId|knowledgePointIds|sourceRefs|citations|generationTaskId)\s*:/);
});

test("citation navigation reads the cited immutable revision and supports either block or page anchors", () => {
  assert.match(appSource, /openMaterial\(citation\.materialId, citation\.revisionId\)/);
  assert.match(appSource, /getMaterialReading\(materialId, revisionId \?\? nextDetail\.currentRevisionId\)/);
  assert.match(appSource, /data-page-number=\{block\.pageNumber\}/);
  assert.match(appSource, /\[data-page-number="\$\{pageNumber\}"\]/);
});

test("generic generation never submits a knowledge-point source to the deprecated endpoint", () => {
  assert.match(appSource, /generationForm\.sourceKind === "knowledgePoint"[\s\S]{0,700}api\.generateKnowledgePointQuestions/);
  assert.match(appSource, /await viewTaskStatus\(submission\.taskId\);\s*return;\s*}\s*const errors/);
});
