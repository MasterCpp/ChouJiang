# J_Sys Agent Guide

This repo contains a long-running multi-event H5 lucky draw system for online or hybrid meetings. Read `CONTEXT.md` before changing domain behavior, and work from local markdown issues under `.scratch/issues/`.

## Agent skills

### Issue tracker

Issues are tracked as local markdown files under `.scratch/issues/`. See `docs/agents/issue-tracker.md`.

### Triage labels

The default Matt Pocock skill label vocabulary is used. See `docs/agents/triage-labels.md`.

### Domain docs

This repo uses a single-context domain layout with `CONTEXT.md` at the repo root and ADRs under `docs/adr/`. See `docs/agents/domain.md`.

## Working Rules

- Keep the first version production-practical but small: one admin, multiple events, QR/link registration, email dedupe per event, one prize pool, draw/re-draw/void, audit records, Excel export, and simple big-screen animation.
- Do not add unrelated systems from earlier discussions, such as watermarking, asset management, or payment mini-program callback work.
- Prefer clear local documents before implementation: PRD, issues, ADRs for significant decisions, then code.
- Treat deployment as two phases: local/demo with a temporary public tunnel, then customer-provided cloud server for formal delivery.
- Do not assume a real domain is available. A server public IP can work for testing, but HTTPS and a proper domain should be called out separately.
