# 017: Branch-Account Isolation for the Chinese Instance

## Status

ready-for-agent

## Goal

Allow branch or local-team accounts within one company to use the existing Chinese J_Sys deployment, with isolated activity workspaces, a Chinese/English interface switch, and no change to the independent English deployment.

## Background

The current application has one environment-configured administrator, in-memory anonymous sessions, shared TSV data, and audit records whose operator is always `admin`. The company needs separate accounts for local teams: for example, an account used by the United States team cannot view or edit activities created by an account used by the Canada team. These are accounts within one company, not separate external customers. Country is an example only; no country field or country-level uniqueness rule is required. The existing `jsys-en` deployment is a separate instance and is explicitly out of scope.

## Scope

- Upgrade only the existing `jsys` deployment and its data.
- Add self-registered Branch Accounts using a globally unique email, password, and a required free-text workspace name, authentication, workspace authorization, and real operator audit records.
- Activate a newly registered Branch Account immediately, without platform-administrator approval.
- Allow public registration with any email address; do not add a company-domain whitelist.
- Validate passwords at 8 to 128 characters without mandatory character classes, and store only password hashes.
- Give every activity and related record exactly one Branch Account owner.
- Add an internal Platform Administrator entry that can view only Branch Account workspace name, email, and status, then disable or re-enable accounts or directly set replacement passwords for offline communication. Platform Administrators are not Branch Account users and cannot access business data.
- Seed the first platform-administrator account from server environment configuration on first deployment; do not expose platform-administrator registration.
- Provide a separate internal Platform Administrator entry URL, with no link from ordinary login or registration pages.
- Retain all legacy data and assign it to the China Account.
- Keep existing legacy registration links, QR codes, and winner-results links valid after migration.
- Give the migrated China Account the fixed initial workspace name `中国账号`; do not translate that user-entered name.
- Create the China Account Owner from separately supplied migration email and initial-password configuration; do not reuse the legacy username.
- Add a Chinese/English UI switch for built-in copy in the Chinese instance.
- Add a Chinese/English built-in-copy switch on every page; start in Chinese and remember the browser-local selection across pages without storing it on an account or activity.
- Add Account Owner settings for workspace name and password; keep the email login identifier immutable.
- Retain account and business audit records with actor, target, time, and result; restrict audit access by role and Branch Account.
- Preserve event and participant-entered multilingual content exactly as stored.
- Keep public registration, result, and big-screen links public to people who hold the event link; tenant isolation applies to administrative data and actions.
- Do not migrate, merge, or change the `jsys-en` deployment.

## Implementation Notes

Treat a Branch Account as an isolated branch or local-team workspace within one Company. Country is an example only and is not a stored field; China Account is the name of the initial migrated workspace. Registration requires a free-text workspace name, which trims leading and trailing whitespace, contains 1 to 100 characters, may contain Chinese, English, or other user-entered characters, and is not translated. The first release has one account owner user per Branch Account; internal multi-user roles and invitations are later enhancements unless separately requested. Password reset is a Platform Administrator action and must replace a password rather than reveal it. The current TSV model is insufficient for reliable account and workspace authorization. Use a transactional persistent store and define a one-time migration that assigns all legacy data to the China Account.

Email is the globally unique login identifier: one exact, case-sensitive email value can own exactly one Branch Account in this release. Email-letter case is not normalized.

The public registration page accepts any email address and immediately activates a successfully registered Branch Account. There is no company-domain whitelist or approval gate.

Passwords have a length of 8 to 128 characters; no uppercase, digit, or special-character mix is required. The persistent store retains only password hashes.

The Platform Administrator uses a separate internal entry URL. Ordinary Branch Account login and registration pages do not display a Platform Administrator entry or link.

Disabling a Branch Account revokes its sessions and makes all of its public registration, winner-results, and big-screen pages unavailable. Re-enabling restores public access without deleting data or audit records.

SQLite is the confirmed transactional persistent store for this Chinese-instance upgrade. The migration must retain the current event, submission, winner, and operation history while adding Branch Account ownership.

The first migration creates the China Account with the fixed initial workspace name `中国账号` and creates its Owner from separately supplied deployment email and initial-password configuration. Legacy administrator usernames are not valid substitutes for an email address.

Legacy public registration, QR-code, and winner-results URLs continue to resolve to the corresponding migrated China Account activity.

On an empty database, server environment configuration seeds the first platform-administrator email and password. The password is hashed before storage; later application restarts must not overwrite the stored platform-administrator password.

An authenticated session lasts seven days. Logout, manual password reset, and Branch Account disablement revoke all active sessions for that account immediately.

Account Owner settings may update only the workspace name and password. Email remains globally unique and cannot be changed in the first release.

Each page begins with Chinese built-in copy. A browser-local Chinese/English choice is remembered across all pages without being stored on an account or activity, and it affects only built-in interface copy; activity content and submitted content remain as entered.

When a Platform Administrator resets a password, it directly sets a replacement password and communicates it offline. The system does not send recovery email and does not require a password change at the next login.

Audit records retain the actor, target, time, and result for registration, login/logout, password or settings changes, account disablement or re-enablement, activities, registrations, draws, voids, and exports. Account Owners may access only their Branch Account's business records. Platform Administrators may access only account-management records.

## Acceptance Criteria

- A newly registered Branch Account receives an empty workspace.
- Registering an email that already belongs to a Branch Account is rejected.
- Email-letter case is significant for uniqueness: otherwise identical values that differ in letter case are separate registration values.
- A workspace name preserves its submitted multilingual content when displayed.
- Registration and settings reject a workspace name that is empty after trimming or longer than 100 characters.
- A newly registered Branch Account is active immediately and can sign in and create its own activities.
- A valid registration using any email domain is accepted without a whitelist or approval step.
- Registration and password change reject passwords shorter than 8 or longer than 128 characters; stored data contains no plaintext password.
- An authenticated Branch Account can list, read, edit, export, draw, and delete only its own activities and related records.
- Guessing another account's activity ID returns no data and performs no action.
- A disabled account cannot create an authorized session or use an existing authorized session.
- A disabled account's public registration, winner-results, and big-screen pages are unavailable until re-enabled, while data and audit records remain retained.
- A platform administrator can disable or re-enable an account and reset its password but cannot reveal a stored password.
- A password reset directly replaces the password without sending email or requiring a first-login password change.
- A platform administrator can view only a Branch Account's workspace name, email, and status; it cannot view, edit, export, draw from, or otherwise operate Branch Account business data.
- The platform administrator has no public registration path and is seeded only once from deployment configuration.
- Ordinary login and registration pages contain no Platform Administrator entry or link.
- Login expires after seven days; logout, password reset, and account disablement immediately invalidate existing sessions.
- An Account Owner can change its workspace name and password, but cannot change its email.
- Every page starts in Chinese built-in copy; a browser can retain a Chinese/English selection across pages without storing it on an account or activity or changing user-entered content.
- Each covered account or business operation creates an audit record with actor, target, time, and result; audit visibility follows the confirmed role and Branch Account boundary.
- Legacy activities, registrations, winners, exports, and operations remain available under the China Account after migration.
- The China Account Owner can sign in using the separately configured migration email and password; a legacy username is not used as its email.
- Migrated workspace display name is `中国账号` and remains unchanged when the interface language changes.
- Existing legacy registration, QR-code, and winner-results links still reach the corresponding China Account activity after migration.
- A public event link continues to expose only its intended registration, result, or big-screen view without granting any administrative access.
- Every mutating activity records the actual account that performed it.
- Switching UI language changes only built-in copy, never stored user content; the browser-local selection is not stored on an account or activity.
- The existing English instance remains unchanged.
