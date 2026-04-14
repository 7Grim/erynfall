# Documentation Guide

## Purpose

This file tells new contributors and LLMs which docs are current, which are historical, and where to start.

Not every markdown file in `docs/` is equally current.

## Read These First

For current engineering and art workflow context, start here:

1. `../AGENTS.md`
2. `../ARTIST_GUIDE.md`
3. `ARCHITECTURE.md`
4. `PROGRESS.md`
5. `ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
6. `ART_PIPELINE_IMPLEMENTATION_ORDER.md`
7. `SKILL_IMPLEMENTATION_BASELINE.md`
8. `CONTRIBUTING.md`

For general repo onboarding, also read:
- `../README.md`
- `../CLAUDE.md`

## Active Operational Docs

These are the most useful current docs for implementation work:

- `ARCHITECTURE.md`
- `PROGRESS.md`
- `ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
- `ART_PIPELINE_IMPLEMENTATION_ORDER.md`
- `SKILL_IMPLEMENTATION_BASELINE.md`
- `CONTRIBUTING.md`
- `../ARTIST_GUIDE.md`

## Historical / Planning Docs

Many other docs in this folder are still useful, but should be treated as:
- historical planning
- broader game design reference
- aspirational or older specification material

Examples:
- `EXHAUSTIVE_DEVELOPMENT_ROADMAP.md`
- `OSRS_REFERENCE.md`
- `VISION.md`
- `SCOPE.md`
- `GAMEPLAY.md`
- `WORLD.md`
- various older implementation milestone docs

These can still help with intent and direction, but they must be cross-checked against:
- current code
- `ARCHITECTURE.md`
- `PROGRESS.md`
- `ARTIST_GUIDE.md`

## Current Recommendation For New LLMs

Use this order:

1. `../AGENTS.md`
2. `../CLAUDE.md`
3. `../ARTIST_GUIDE.md`
4. `ARCHITECTURE.md`
5. `PROGRESS.md`
6. `ART_PIPELINE_IMPLEMENTATION_CHECKLIST.md`
7. `ART_PIPELINE_IMPLEMENTATION_ORDER.md`
8. `SKILL_IMPLEMENTATION_BASELINE.md`
9. `CONTRIBUTING.md`

Then inspect code.

Do not assume older design docs override current code.
