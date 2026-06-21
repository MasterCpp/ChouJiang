# ADR 0002: Start With a Zero-Dependency Runnable Scaffold

## Status

Accepted

## Context

The first implementation issue requires a local runnable scaffold. The expected production direction is Vue3 plus Java, but the current workspace has npm PowerShell execution-policy friction and no need for a cloud service or dependency downloads to prove the first baseline.

## Decision

Start with a Java 17 standard-library HTTP server and static H5 files:

- `backend/src/main/java/com/jsys/App.java`
- `frontend/public/`
- `scripts/build.cmd`
- `scripts/start-dev.cmd`

The server provides:

- `/api/health`
- static frontend serving from `frontend/public`

## Consequences

- The project can run locally with only Java 17.
- The first scaffold is easy to verify and demo.
- The app can later move to Spring Boot and Vue3 if the implementation outgrows the zero-dependency scaffold.
