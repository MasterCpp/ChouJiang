# English Isolated Instance

## Goal

Provide an English-only deployment of J_Sys on the same server without changing or exposing the existing Chinese/bilingual instance's data, accounts, or activities.

## Background

The English-speaking customer needs its own administrator, activity data, registration flow, draw screen, results, and export. The confirmed delivery model is a separate deployment instance rather than a shared multi-tenant backend.

## Scope

- Add an English locale mode for all built-in UI copy, validation feedback, default form templates, and CSV headers.
- Keep the existing bilingual mode as the default and do not change its current copy.
- Add Linux systemd and health-check templates for an independent `jsys-en` process on port `8081` with a separate `/opt/jsys-en` working directory and data folder.
- Document deployment, verification, and rollback boundaries for the English instance.

## Acceptance Criteria

- Starting the app with English locale enabled renders administrator, registration, result, and screen UI without Chinese built-in copy.
- English locale emits English default questions, validation errors, and CSV headers.
- Existing locale behavior remains unchanged when no locale is configured.
- English instance service and health-check templates cannot target the existing `jsys` service or its `8080` data directory.
- Deployment instructions preserve the existing instance and clearly require separate administrator credentials and data.
- Local build and smoke checks pass for both default and English locale modes.

## Implementation Notes

The English instance is intentionally separate from the proposed future multi-tenant upgrade. It provides hard data isolation through a separate working directory and process, not through account filtering inside the existing instance.

## Status

done
