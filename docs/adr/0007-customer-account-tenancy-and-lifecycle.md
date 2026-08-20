# ADR 0007: Model Branch Accounts as Isolated Workspaces

## Status

Accepted

## Decision

The Chinese instance serves one Company through Branch Accounts. A Branch Account is an isolated local-team workspace; country is an example only, not a stored field or a uniqueness rule. Each Branch Account self-registers with a globally unique, case-sensitive email value and password, and registration immediately creates an active isolated workspace with one Account Owner in the first release. Registration accepts any email domain and immediately activates the workspace; there is no company-domain whitelist or approval gate. Passwords must be 8 to 128 characters and are stored only as hashes, without a mandatory character-class mix. The Account Owner may change the workspace name and password, but not its email. All activities and related administrative data belong to that workspace. Account and business audit records retain actor, target, time, and result; Account Owners can access only their own business records, while Platform Administrators can access only account-management records. Login sessions last seven days and are revoked on logout, password reset, or account disablement. Disabling also makes the Branch Account's public registration, winner-results, and big-screen pages unavailable until it is re-enabled, while retaining data and audit records. A separate internal Platform Administrator can view only workspace name, email, and status, then disable or re-enable accounts and directly set replacement passwords for offline communication; it cannot access Branch Account business data. Branch Account self-deletion, additional team membership, approval workflow, and automated password-recovery email are out of scope.

## Consequences

- Authorization must be enforced for every administrative request using Branch Account ownership, not by hiding browser controls.
- All existing historical data is retained and migrated to the China Account.
- The persistent store must support account identity, password hashes, account status, ownership, audit history, and an atomic data migration; the current shared TSV files do not provide a suitable foundation.
