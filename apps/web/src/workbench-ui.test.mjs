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

test("material body and chunks are normalized before display so encoded anchors are readable", () => {
  assert.match(appSource, /function readableMaterialText/);
  assert.match(appSource, /decodeURIComponent/);
  assert.match(appSource, /<pre>\{readableMaterialText\(content\)\}<\/pre>/);
  assert.match(appSource, /<p>\{readableMaterialText\(chunk\.content\)\}<\/p>/);
});

test("extracted knowledge points are rendered as readable details instead of keyword-only chips", () => {
  assert.match(appSource, /KnowledgePointSummaryList/);
  assert.match(appSource, /points=\{props\.materialDetail\.extractedKnowledgePoints\s*\?\?\s*\[\]\}/);
  assert.match(appSource, /ref\.excerpt/);
});
