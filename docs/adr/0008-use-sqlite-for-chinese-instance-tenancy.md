# ADR 0008: Use SQLite for Chinese-Instance Account Data

## Status

Accepted

## Decision

Use SQLite as the transactional persistent store for the Chinese-instance multi-account upgrade. It replaces the shared TSV runtime files for Branch Accounts, authentication data, workspace ownership, activities, registrations, winners, exports, and audit records. The one-time migration creates the China Account with fixed initial workspace name `中国账号` and creates its Owner from separately supplied deployment email and initial-password configuration rather than interpreting the legacy username as an email. Existing legacy registration, QR-code, and winner-results URLs remain valid and resolve to their migrated China Account activities. The independent English instance is not changed.

## Consequences

- The implementation must package the SQLite JDBC dependency, create and migrate the schema, create the China Account Owner from configured credentials, and provide a safe one-time import of the existing TSV data into the China Account.
- Public URL identifiers must be preserved during import so legacy registration, QR-code, and winner-results links keep resolving.
- Backups and recovery must include the SQLite database as one consistent artifact.
- SQLite is selected for the current single-server deployment; moving to a separate database service remains a future scaling decision.
