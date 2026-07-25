# ADR 0004: Deploy the English Product as a Separate Instance

## Status

Accepted

## Context

An English-speaking customer needs its own administrator and data, with no visibility into the existing Chinese/bilingual customer's activities, registrations, winners, exports, or operation records. The existing J_Sys application has one administrator and one local data directory.

## Decision

Deploy a separate English instance at `/opt/jsys-en`, with its own administrator credentials, `data/` directory, systemd services, and port `8081`. Add an application locale mode so the shared codebase can render English-only built-in copy for that instance while preserving the existing bilingual default.

## Consequences

- Data isolation is enforced by separate processes and storage directories, without introducing multi-tenant authorization to the current product.
- The two instances must be deployed, monitored, backed up, and upgraded independently.
- Later migration to a true multi-tenant product remains possible, but is outside this delivery.
