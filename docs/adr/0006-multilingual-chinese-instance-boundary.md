# ADR 0006: Keep the Account Upgrade in the Existing Chinese Instance

## Status

Accepted

## Decision

Implement the planned multi-account capability only in the existing `jsys` deployment. It is a single-company, multi-workspace product: each Branch Account represents a branch or local team and owns a separate workspace of activities and related data. Country is an example only; the system does not store or validate countries. Branch Accounts cannot view or modify one another's data. Every page starts with Chinese built-in UI copy and presents a Chinese/English switch. Its browser-local selection is remembered across pages, while event titles, form questions, names, and other user-entered content remain unchanged. No language preference is stored on a Branch Account or activity. The independent `jsys-en` deployment is neither migrated nor modified by this work.

## Consequences

- The Chinese instance becomes the single scope for Branch Account, workspace authorization, migration, deployment, and browser verification work.
- Language selection is a presentation preference, not a second copy of activity data or a translation of user-entered content.
- A browser-selected language affects only its built-in UI across pages; it is not an account or activity property and never alters stored user content.
- Every activity, registration, winner, export, and operation record must belong to exactly one Branch Account, and every protected request must enforce that ownership on the server.
- The existing English instance continues to use its current independent data, credentials, service, and release process.
