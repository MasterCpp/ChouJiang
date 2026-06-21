# PRD: Multi-Event H5 Lucky Draw System

## Objective

Build a long-running H5 lucky draw system for small meetings. The system supports many events, each with its own QR code and registration link. Participants submit required information, enter the draw pool, and the admin draws winners on a big-screen page.

## Users

- Participant: opens the H5 registration page by QR code or link and submits information.
- Admin: logs in, creates events, configures form copy, starts draws, manages winner records, and exports data.
- Audience/host: views the big-screen rolling draw and winner result.

## Core Flow

1. Admin logs in.
2. Admin creates an event.
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

- One admin login.
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

- No multi-admin permission system.
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
