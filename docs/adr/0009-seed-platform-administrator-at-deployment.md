# ADR 0009: Seed the Platform Administrator at First Deployment

## Status

Accepted

## Decision

On first startup against an empty Chinese-instance database, create the internal Platform Administrator from server environment-provided email and initial password. Store only its password hash. It uses a separate internal entry URL that is not linked from ordinary Branch Account login or registration pages. No public route can register a Platform Administrator, and later restarts do not overwrite the stored password.

## Consequences

- Formal deployment requires protected platform-administrator environment values.
- Branch Account self-registration can never gain Platform Administrator privileges.
- The internal entry URL is an access boundary for product navigation, not a substitute for authentication or authorization checks.
- Manual password recovery remains an internal operation without an automated email workflow.
