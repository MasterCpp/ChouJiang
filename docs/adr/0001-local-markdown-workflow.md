# ADR 0001: Use Local Markdown Workflow

## Status

Accepted

## Context

The project is starting from an empty local workspace and has no GitHub repository or remote issue tracker yet. The user wants to use Matt Pocock-style skills to guide planning and implementation.

## Decision

Use local markdown issues under `.scratch/issues/` and a single root `CONTEXT.md`.

## Consequences

- Work can start immediately without external services.
- Issues and PRD remain versionable if the folder is later turned into a git repo.
- If the project later moves to GitHub or GitLab, the issue tracker docs should be updated.
