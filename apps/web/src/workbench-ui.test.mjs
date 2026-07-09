import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";

const appSource = readFileSync(new URL("./App.tsx", import.meta.url), "utf8");

test("successful ready material import opens the imported material detail in the materials panel", () => {
  assert.match(appSource, /setSection\("materials"\)/);
  assert.match(appSource, /await\s+openMaterial\(material\.id\)/);
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
