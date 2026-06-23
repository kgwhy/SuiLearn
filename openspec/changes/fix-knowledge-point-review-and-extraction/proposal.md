# Proposal

## Summary

Fix the web knowledge point review experience and reduce noisy extracted knowledge points after material import.

## Problem

Imported materials currently produce knowledge point chips that cannot be opened from the workbench overview. The extraction path also behaves like text splitting: it accepts punctuation-only separators, duplicate terms, and long sentence fragments, and it does not use retrieved material evidence or the configured AI provider before producing knowledge points.

## Scope

- Make knowledge points in the web workbench clickable and show their detail.
- Apply the same interaction to extracted knowledge points shown in material detail.
- Improve backend extraction by using material chunk evidence with the configured AI provider before falling back to local candidate filtering.
- Filter punctuation-only values, markdown separators, long sentence fragments, and case-insensitive duplicates.
- Add backend regression coverage for noisy extraction input.

## Non-goals

- No contract or endpoint shape changes.
- No Android changes.
- No product or architecture fact document changes.

## Acceptance

- After importing material, generated knowledge points can be opened and reviewed in the web UI.
- Knowledge point extraction is backed by material chunk source refs and the configured AI provider when provider output is available.
- Noisy input containing separators, duplicate Java/MySQL terms, and long sentence fragments does not create those invalid knowledge point names.
- Existing web build/test and backend tests pass.
