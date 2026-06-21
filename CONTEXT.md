# Context

## Product

J_Sys is a multi-event lucky draw system for small online or hybrid meetings. Each event has its own registration link and QR code. Participants open an H5 page, submit required information, and enter that event's draw pool. An admin configures events, starts draws, views winners, voids or redraws results, and exports event data.

## Confirmed Scope

- Long-term use across many events.
- Each event has fewer than 100 participants.
- Participants may join different events with the same email.
- Email dedupe is scoped to one event only.
- No pre-imported participant list in the first version.
- Registration fields are required.
- Chinese and English content appear together on the same page; no language switch is required.
- Customer will provide or approve final bilingual copy.
- The event creator sets the winning count before the draw.
- One prize category in the first version.
- Winners cannot join later draws for the same event.
- Re-draw and void winner actions are required and must leave operation records.
- First version uses a simple rolling big-screen animation.
- Both QR code and copyable registration link are required.
- After registration, participants can open a public "View Winners" result page for the event.
- One administrator login is enough for the first version.
- Data is retained long term: events, form definitions, submissions, winner records, export records, and operation records.
- Excel export includes all registration fields, questionnaire answers, winner state, and winning time.
- A short privacy notice is shown on the registration page.

## Registration Form

The current event form should support these fields:

- Name
- Job title
- Email
- Overall satisfaction with today's topic sharing, scored 1 to 10
- Most satisfying sharing topic, single choice
- Expected topic for future deeper discussion, free text

The editable event copy includes:

- Event title
- Satisfaction question text
- Single-choice question text
- Single-choice options
- Free-text question text
- Privacy notice copy if needed

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
