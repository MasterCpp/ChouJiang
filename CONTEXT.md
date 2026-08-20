# Context

## Product

J_Sys is a multi-event lucky draw system for small online or hybrid meetings. Each event has its own registration link and QR code. Participants open an H5 page, submit required information, and enter that event's draw pool. An admin configures events, starts draws, views winners, voids or redraws results, and exports event data.

## Confirmed Scope

- Long-term use across many events.
- Each event has fewer than 100 participants.
- Participants may join different events with the same email.
- Email dedupe is scoped to one event only.
- No pre-imported participant list in the first version.
- Base registration fields are required.
- Each event can configure its own additional registration questions.
- Dynamic registration questions support single choice, multiple choice, text answer, and 1-10 score.
- The existing bilingual/Chinese deployment is the in-scope product for the next account upgrade. It will offer a Chinese/English UI switch while preserving user-entered event and registration content.
- The separate English deployment remains independent and is out of scope for this upgrade.
- Customer will provide or approve final bilingual copy.
- The event creator sets the final winner count before the draw.
- One prize category in the first version.
- Winners cannot join later draws for the same event.
- Each draw action selects one winner; hosts repeat the draw until the valid winner count reaches the configured winner count.
- Re-draw and void winner actions are required and must leave operation records.
- Admins can delete an individual registration; related winner records are cleaned up to avoid orphaned draw data.
- Admins can copy an existing event's settings into a new clean event without copying registrations, winners, or operation history.
- First version uses a simple rolling big-screen animation.
- Both QR code and copyable registration link are required.
- After registration, participants can open a public "View Winners" result page for the event.
- The next upgrade introduces independent Branch Accounts for one company. Each Branch Account owns its activities and related data; one Branch Account must never view or modify another Branch Account's activities.
- Branch Accounts are self-registered with an email address, password, and a free-text workspace name. The name is required, trims leading and trailing whitespace, and contains 1 to 100 characters; it may contain Chinese, English, or other user-entered characters. No country or region field is required, and the system does not enforce country-level uniqueness.
- The Branch Account registration page accepts any email address; there is no company-domain whitelist or approval gate in the first release. Email uniqueness is case-sensitive.
- A Branch Account password must contain 8 to 128 characters. Passwords are stored only as hashes; no character-class complexity rule is required.
- A Platform Administrator is an internal operator, outside every Branch Account. Platform Administrators can disable or re-enable accounts and directly set a replacement password for offline communication to the Account Owner, but Branch Accounts cannot delete themselves.
- The Platform Administrator uses a separate internal entry URL. Ordinary Branch Account login and registration pages show no platform-administrator entry or link.
- Disabling a Branch Account invalidates its sessions and temporarily makes all of its public registration, winner-results, and big-screen pages unavailable. Re-enabling restores access; data and audit records remain retained.
- All legacy activity data will be retained and assigned to the China Account, whose fixed initial workspace name is `中国账号`.
- Existing public registration links, QR codes, and winner-results links continue to resolve to their migrated China Account activities.
- The China Account is created during the first migration with a separately supplied Owner email and initial password; it does not inherit the legacy username.
- The Chinese instance starts its built-in interface copy in Chinese. Every page offers a Chinese/English switch, whose browser-local choice is remembered across pages without being stored on an account or activity. The switch never translates user-entered content.
- An Account Owner can update its workspace name and password in account settings. Its email remains the immutable global login identifier.
- Account and business operations retain audit records with actor, target, time, and result. An Account Owner can access only its own business audit records; a Platform Administrator can access only account-management audit records.
- Data is retained long term: events, form definitions, submissions, winner records, export records, and operation records.
- Excel export includes all registration fields, questionnaire answers, winner state, and winning time.
- A short privacy notice is shown on the registration page.

## Registration Form

The base registration form always includes these fields:

- Name
- Job title
- Email

Each event can then configure its own registration questions. The default template includes:

- Overall satisfaction with today's topic sharing, scored 1 to 10
- Most satisfying sharing topic, single choice
- Expected topic for future deeper discussion, free text

Supported dynamic question types:

- Single choice
- Multiple choice
- Text answer
- Score from 1 to 10

The editable event copy includes:

- Event title
- Dynamic question text
- Question type
- Required or optional setting
- Choice options for single-choice and multiple-choice questions
- Privacy notice copy if needed

## Interface Language

**User**:
A person who signs in with an email address and password. The exact, case-sensitive email address identifies exactly one User and one Branch Account in the first multi-account release.
_Avoid_: Account, customer

**Company**:
The single organization that uses this J_Sys deployment through multiple Branch Accounts.
_Avoid_: Customer Account, tenant

**Branch Account**:
An independently registered branch or local-team workspace within the Company that owns activities and all related administrative data. A country is a common example, not a required field. Branch Accounts cannot access one another's administrative data or actions.
_Avoid_: User, country record, tenant

**Workspace Name**:
The required, user-entered display name of a Branch Account. It is trimmed of leading and trailing whitespace, contains 1 to 100 characters, identifies a workspace to its owner and Platform Administrator, and is not translated by the interface.
_Avoid_: Country, locale

**Account Owner**:
The initial user responsible for a Branch Account in the first multi-account release.
_Avoid_: Platform administrator

**Platform Administrator**:
An internal operator who can view Branch Account name, email, and status, then disable or re-enable an account or directly set a replacement password for offline communication to the Account Owner. A Platform Administrator cannot access Branch Account business data.
_Avoid_: Branch Account Owner

**Interface Language**:
A browser-local choice between Chinese and English for built-in interface copy. Every page starts in Chinese until the browser has a remembered choice. It is not stored on a Branch Account or activity and never translates user-entered content.
_Avoid_: Content translation

**Audit Record**:
An immutable record of an account or business operation, including actor, target, time, and result. Business audit records remain within their owning Branch Account; Platform Administrator audit access is limited to account-management operations.
_Avoid_: Editable activity data

## Deployment Understanding

The user will first run and test locally, then create a demo video or temporary QR experience for the customer. Formal deployment is expected on a customer-provided server, likely an Alibaba Cloud Hong Kong node.

For formal long-term operation, a real domain plus HTTPS is preferable. A server public IP can work for testing or a minimal delivery, but it is less professional and harder to secure with HTTPS.

Suggested minimum server baseline for Java plus database:

- 2 CPU cores
- 2 GB RAM minimum, 4 GB preferred
- 40 GB or more disk
- 3-5 Mbps bandwidth for this traffic level
- Ubuntu LTS

## Out of Scope

- WeChat mini-program dependency.
- Payment, order, or Tonglian callback logic.
- Image/video watermarking.
- Asset/version management system.
- Complex prize tiers in the first version.
- Complex animated show production in the first version.
- SMS or email winner notifications in the first version.
- Multi-admin permission matrix in the first version.

## Open Decisions

- Exact UI style and customer branding.
- Whether formal delivery will use a real domain and HTTPS or only a public IP.
- Final bilingual copy for each event.
- Final technology stack after project scaffolding.
