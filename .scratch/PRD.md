# PRD: Multi-Event H5 Lucky Draw System

## Objective

Build a long-running H5 lucky draw system for small meetings. The system supports many events, each with its own QR code and registration link. Participants submit required information, enter the draw pool, and the admin draws winners on a big-screen page.

## Users

- Participant: opens the H5 registration page by QR code or link and submits information.
- Branch account owner: self-registers with email and password, then creates events, configures form copy, starts draws, manages winner records, and exports data in their own isolated workspace.
- Platform administrator: an internal operator who can view Branch Account name, email, and status, then disable or re-enable Branch Accounts and manually reset passwords, but cannot access Branch Account business data.
- Audience/host: views the big-screen rolling draw and winner result.

## Core Flow

1. A customer registers an account with email and password, then signs in.
2. The Branch Account owner creates an event in their own workspace.
3. Admin configures event title, bilingual form copy, single-choice options, winning count, and privacy notice.
4. System generates a registration link and QR code.
5. Participant opens the registration page and submits all required fields.
6. System rejects duplicate email submissions for the same event.
7. After successful registration, participant can use a "View Winners" link to open the public event result page.
8. Admin opens the event draw page and starts a draw.
9. System draws the configured number of winners from eligible participants.
10. Winner result is shown on the big-screen page and public result page.
11. Admin can void a winner record and redraw, with operation records retained.
12. Admin exports event data to Excel.

## Functional Requirements

- Self-registered Branch Accounts with email, password, and required free-text workspace name for different country or subsidiary teams within one company; workspace names trim leading/trailing whitespace and contain 1 to 100 characters.
- One exact, case-sensitive email address can register exactly one Branch Account; email-letter case is not normalized.
- Branch Account registration immediately creates an active account and empty workspace; no platform-administrator approval is required.
- The public registration page accepts any email address; no company-domain whitelist is required.
- Passwords must contain 8 to 128 characters, without a character-class complexity rule; only password hashes are stored.
- No country or region field, country list, or country-level uniqueness rule.
- Workspace names may contain Chinese, English, or other user-entered characters and are never translated.
- One initial account owner per Branch Account.
- Branch Account data isolation: activities, registrations, winners, exports, and operation records belong to one Branch Account and cannot be read or changed by another Branch Account.
- Internal platform-administrator entry for account disablement, re-enablement, and manually setting a replacement password for offline communication; customer self-deletion is not supported.
- The platform-administrator account is seeded only on the Chinese-instance server's first deployment from environment-provided email and initial password; it has no public registration path.
- Platform Administrator access uses a separate internal URL that is not linked from ordinary Branch Account login or registration pages.
- A platform administrator resets a password by directly setting a new one and communicating it offline; the system does not send password-recovery email or force a first-login password change.
- Platform administrators cannot view, edit, export, draw from, or otherwise operate Branch Account activities, registrations, winners, exports, or operation records.
- Audit records retain actor, target, time, and result for registration, login/logout, credential or setting changes, account disablement or re-enablement, activity operations, registrations, draws, voids, and exports. Account Owners can access only their own business records; Platform Administrators can access only account-management records.
- Login sessions expire after seven days and are revoked immediately on logout, password reset, or account disablement.
- A disabled Branch Account's public registration, winner-results, and big-screen pages are unavailable until the account is re-enabled; its data and audit records remain retained.
- Existing historical data is retained and assigned to the China Account.
- Existing registration links, QR codes, and winner-results URLs remain valid after migration and resolve to their migrated China Account activities.
- The initial China Account workspace name is fixed as `中国账号` and is treated as user-entered content, so it is not translated by the interface.
- The initial China Account Owner email and password are supplied through deployment configuration during the first migration, rather than being inferred from the legacy username.
- SQLite is the persistent store for the Chinese instance's Branch Accounts, ownership, activity data, and audit records.
- Every page begins with Chinese built-in copy and offers a Chinese/English switch. The browser remembers its selection across pages, without storing a language preference on an account or activity. User-entered event and participant content is not translated.
- Account settings allow an Account Owner to update its workspace name and password; the email login identifier cannot be changed.
- Multi-event management.
- Required registration fields:
  - Name
  - Job title
  - Email
  - Satisfaction score, 1-10
  - Most satisfying topic, single choice
  - Expected future discussion topic, free text
- Event-level editable content:
  - Event title
  - Question text
  - Single-choice options
  - Privacy notice
- Email dedupe per event.
- One prize category.
- Winning count set at event creation or editing.
- Winners are excluded from later draws in the same event.
- Re-draw and void actions with operation records.
- Simple rolling big-screen animation.
- QR code and copyable registration link.
- Public "View Winners" result page for each event.
- Excel export per event.
- Long-term data retention.

## Non-Functional Requirements

- Usable on mobile browsers without WeChat dependency.
- Works for domestic and overseas participants as long as they can access the deployment address.
- First version targets fewer than 100 participants per event.
- Deployment should support local demo first, then customer server deployment.
- Registration page includes a short privacy notice.

## First-Version Constraints

- No customer-team member, invitation, or multi-admin permission system in this release.
- No automated email password-recovery service in this release; reset is handled by a platform administrator.
- No complex prize tiers.
- No pre-imported attendee list.
- No production-grade animation design beyond a simple rolling effect.
- No SMS or email winner notification in the first version.
- No payment, watermarking, or asset management features.

## Acceptance Criteria

- Admin can create an event and obtain a link plus QR code.
- Participant can submit the form once per event by email.
- Duplicate email in the same event is rejected.
- Same email can register for a different event.
- Admin can draw the configured winner count.
- Existing winners are not selected again for the same event.
- Admin can void a winner and redraw.
- Operation records show draw, void, redraw, and export actions.
- Admin can export all event data.
- Big-screen page shows a simple rolling animation and final winners.
- Registration success page includes a "View Winners / 查看中奖结果" button or link.
- Public result page shows a waiting state before the draw and winner list after the draw.
