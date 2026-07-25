# J_Sys

J_Sys is a multi-event H5 lucky draw system for small online or hybrid meetings. Participants scan a QR code or open a registration link, submit required information, and enter that event's draw pool. An admin creates events, starts draws, manages winner records, and exports event data.

## Current Status

The project has a runnable local/demo implementation. Initial issues are complete, and the app now includes dynamic per-event registration form configuration.

Read first:

- `STATUS.md` for current progress and next steps.
- `CONTEXT.md` for product scope and business rules.
- `.scratch/PRD.md` for the product requirements.
- `.scratch/issues/` for implementation tasks and acceptance criteria.

## Confirmed First Version

- Multi-event long-term use.
- Fewer than 100 participants per event.
- Email dedupe within one event.
- Same email can join different events.
- Required registration fields:
  - Name
  - Job title
  - Email
- Dynamic per-event questions:
  - Score from 1 to 10
  - Single choice
  - Multiple choice
  - Text answer
- Chinese and English copy shown together.
- One admin login.
- One prize category.
- Winning count configured on the event as the final quota.
- Each draw action selects one winner; repeat the draw until the quota is full.
- Void and redraw with operation records.
- Simple rolling big-screen animation.
- QR code and copyable registration link.
- Excel export for each event.
- Privacy notice on the registration page.

## Development

The first runnable scaffold uses Java 17 standard library APIs plus static H5 files. It does not require npm, Maven downloads, a cloud service, or a formal domain for local development.

### Requirements

- Java 17
- Windows command prompt or PowerShell

### Start Locally

From the repo root:

```text
scripts\start-dev.cmd
```

Then open:

```text
http://127.0.0.1:8080/
```

Health endpoint:

```text
http://127.0.0.1:8080/api/health
```

Default admin credentials:

```text
username: admin
password: admin123
```

For local override:

```text
set ADMIN_USERNAME=your-user
set ADMIN_PASSWORD=your-password
scripts\start-dev.cmd
```

### Build

```text
scripts\build.cmd
```

### Test

There is no automated test suite yet. For the scaffold baseline, verify:

- `scripts\build.cmd` succeeds.
- `scripts\start-dev.cmd` starts the local server.
- `http://127.0.0.1:8080/` shows the landing page.
- `http://127.0.0.1:8080/api/health` returns JSON with `"status":"ok"`.
- Admin login accepts the default credentials.
- Invalid admin credentials are rejected.
- Admin can create and edit an event.
- Admin can configure event-specific registration questions.
- Admin can delete individual registrations.
- Admin can copy an existing event's settings into a clean new event.
- Event validation rejects invalid winning count and empty single-choice options.
- Public registration link accepts valid submissions.
- Public registration renders dynamic event questions.
- Duplicate email in the same event is rejected.
- The same email can register for another event.
- Public result link shows a waiting state before winners are drawn.
- Admin can draw winners one at a time, void a winner, redraw a replacement, and view operation records.
- Public result link shows valid winners after the draw.
- Big-screen link shows waiting state before draw and winner display after draw.
- Event export downloads an Excel-compatible CSV and records an export operation.
- Event export includes dynamic question columns and answers.

## Runtime Data

Local runtime data is stored under:

```text
data/
```

Current event configuration data is stored in `data/events.tsv`.
Current registration data is stored in `data/submissions.tsv`.
Current winner data is stored in `data/winners.tsv`.
Current operation records are stored in `data/operations.tsv`.
Runtime data is ignored by git.

## URL Map

- Admin: `http://127.0.0.1:8080/`
- Registration: `http://127.0.0.1:8080/join/{eventId}`
- Public result page: `http://127.0.0.1:8080/results/{eventId}`
- Big screen: `http://127.0.0.1:8080/screen/{eventId}`
- Export: `http://127.0.0.1:8080/api/admin/events/{eventId}/export`
- Health: `http://127.0.0.1:8080/api/health`

The latest implementation task is tracked in:

```text
.scratch/issues/009-admin-submission-delete-and-event-copy.md
```

## Deployment Plan

Development and customer preview should happen in two phases:

1. Local demo and temporary QR/link preview.
2. Formal deployment on a customer-provided server.

The expected formal deployment target is an Alibaba Cloud Hong Kong node. A public IP can be used for testing or minimal delivery, but a real domain plus HTTPS is preferable for long-term production use.

Detailed guides:

- `docs/demo.md`: local demo, recording sequence, temporary customer scan test.
- `docs/deployment.md`: server baseline, public IP/domain notes, deployment checklist, backup notes.

### English Isolated Instance

Set `JSYS_LOCALE=en` to render built-in copy in English. The Linux English deployment template runs a separate instance from `/opt/jsys-en` on port `8081`, with independent administrator credentials and runtime data. See `docs/deployment.md` for the installation steps.

Quick local verification after starting the app:

```text
scripts\verify-local.cmd
```

## Documentation Map

- `AGENTS.md`: agent collaboration rules.
- `CONTEXT.md`: domain context and confirmed scope.
- `STATUS.md`: current progress.
- `.scratch/PRD.md`: product requirements.
- `.scratch/issues/`: local markdown issues.
- `docs/adr/`: architecture decisions.
- `docs/agents/`: Matt Pocock skills configuration.
- `docs/demo.md`: demo and temporary scan-test guide.
- `docs/deployment.md`: deployment and verification guide.
