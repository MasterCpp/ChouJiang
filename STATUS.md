# Status

## Current State

Planning and local workflow setup are complete. The first runnable application scaffold now exists.

## Completed

- Confirmed product scope: long-running multi-event H5 lucky draw system.
- Confirmed first-version rules:
  - Multiple events, long-term use.
  - Fewer than 100 participants per event.
  - Email dedupe within the same event only.
  - Same person can join different events.
  - All registration fields are required.
  - Chinese and English copy appears together on the same page.
  - One prize category.
  - Winning count is configured on the event.
  - Winners cannot be selected again in the same event.
  - Void and redraw are required and must leave operation records.
  - Simple rolling big-screen animation for the first version.
  - QR code and copyable registration link are both required.
  - Registration success page includes a "View Winners / 查看中奖结果" action.
  - Public event result page shows waiting state before draw and winners after draw.
  - One admin login is enough.
  - Long-term retention for events, forms, winner records, export records, and operation records.
  - Excel export includes all registration fields, questionnaire answers, winner state, and winning time.
  - Registration page includes a privacy notice.
- Created agent workflow files:
  - `AGENTS.md`
  - `CONTEXT.md`
  - `docs/agents/`
  - `docs/adr/`
  - `.scratch/PRD.md`
  - `.scratch/issues/`
- Expanded all initial issues with testable acceptance criteria.
- Added a Java 17 standard-library dev server.
- Added static H5 landing page files.
- Added Windows build and start scripts.
- Added admin login, event list, event create/edit, validation, and event sharing targets.
- Added public registration page and submission API.
- Added event-scoped email dedupe.
- Added public result page waiting state.
- Added admin submission list API and UI.
- Added draw, winner record, void, replacement redraw, and operation record APIs.
- Added admin draw controls and public result winner display.
- Added big-screen page with waiting state and simple rolling winner animation.
- Added Excel-compatible CSV export and export operation records.
- Added demo guide, deployment guide, and local verification helper.
- Polished the admin page UI with event cards, visible QR preview, copy buttons, collapsible operation records, sticky edit form, and blank new-event title.
- Added an admin-only start-draw control on the big-screen page so the presenter can draw and show results on the same screen.
- Changed the admin start-draw action to open the big-screen page, where registered participant names roll before the final winner result appears.
- Added admin event deletion from the event list, including related submissions, winners, and operation records.
- Changed the big-screen presenter flow to two steps: start participant rolling first, then reveal winners with a second button press.
- Added admin winner-record deletion from the winner list, with a `delete_winner` operation record.

## Current Issue

Latest completed implementation issue:

```text
.scratch/issues/006-demo-and-deployment.md
```

Next implementation issue:

```text
All initial issues are done. Next step is manual product QA and customer demo preparation.
```

## Next Steps

1. Verify the scaffold commands.
2. Start implementation issue `002-event-admin.md`.
3. Add admin login and event management.
4. Update this file with test results after each implementation step.

## Open Questions

- Final UI style and branding.
- Final bilingual copy for each event.
- Whether formal delivery will use a real domain plus HTTPS or only a public IP.
- Exact formal server configuration after customer confirms the Alibaba Cloud Hong Kong server.

## Latest Verification

- Documentation structure verified.
- All 6 initial issue files have acceptance criteria.
- `scripts\build.cmd` completed successfully.
- Temporary local server verified with `java -cp backend\out com.jsys.App 8080`.
- `http://127.0.0.1:8080/api/health` returned `status: ok`.
- `http://127.0.0.1:8080/` returned HTTP 200 and the landing page content.
- Admin APIs verified on a temporary PowerShell job server:
  - unauthenticated event API access returned 401.
  - invalid login returned 401.
  - valid login returned 200.
  - event creation returned a stable ID.
  - event edit preserved the ID and updated winning count.
  - invalid winning count returned 400.
  - empty single-choice options returned 400.
- Registration APIs verified on a temporary PowerShell job server:
  - valid submission returned an ID.
  - duplicate email in the same event returned 400.
  - same email in a different event was accepted.
  - invalid email returned 400.
  - missing required name returned 400.
  - closed event registration returned 409.
  - admin submission list returned the saved participant and questionnaire answers.
  - public result API returned `waiting`.
- Draw APIs verified on a temporary PowerShell job server:
  - draw with too few eligible participants returned 409.
  - event with 4 participants and winning count 3 created exactly 3 winners.
  - repeat draw for the same event returned 409 because only 1 eligible participant remained.
  - winner state in event A did not block drawing the same email in event B.
  - void changed a winner status to `voided`.
  - replacement redraw selected a different submission from the just-voided winner.
  - valid winner count after redraw remained 3.
  - operation records include draw, void, and redraw.
  - public result API returned `completed` with 3 valid winners.
- Big-screen/export verified on a temporary PowerShell job server:
  - export for an event with no submissions returned 200 with headers.
  - big-screen URL returned 200 before draw.
  - public result API returned `waiting` before draw.
  - public result API returned `completed` after draw.
  - big-screen URL returned 200 after draw.
  - after void and redraw, public results showed only the latest valid winner.
  - export included participant data, winner status, and void status.
  - export action was recorded in operation records.
- Demo/deployment docs verified:
  - `docs/demo.md` includes local start, demo data, video sequence, and temporary tunnel notes.
  - `docs/deployment.md` includes server baseline, public IP vs domain notes, deployment checklist, and backup notes.
  - `scripts\verify-local.cmd` verifies local health and home page once the app is running.
- `scripts\verify-local.cmd` passed against a temporary local server.
- Admin UI polish verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - `scripts\verify-local.cmd` passed against a temporary local server after the UI changes.
- Big-screen draw control verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - Cloudflare public `main.js` contains the new `screenDrawButton` logic.
- Big-screen presenter flow verified:
  - Admin event-card start draw redirects to `/screen/{eventId}`.
  - Big-screen draw rolls registered participant names for at least 2.4 seconds before showing winners.
  - Cloudflare public `main.js` contains the new `renderScreenRolling` logic.
- Event deletion verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - Temporary server create/delete check removed the event from the admin list.
  - Deleted event submission lookup returned 404.
  - Cloudflare public `main.js` contains the new delete button logic.
- Two-step big-screen draw verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
- Big-screen confirm-draw button copy updated to `确认抽奖 / Confirm Draw` per customer feedback.
- Added `恭喜中奖 / Congratulations` copy above the big-screen winner list.
- Winner-record deletion verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - Temporary server smoke test created an event, submitted one participant, drew one winner, deleted the winner record, and confirmed the winner list returned `[]`.
  - Operation records contained `delete_winner`.

## Local Commands

Build:

```text
scripts\build.cmd
```

Start:

```text
scripts\start-dev.cmd
```

Open:

```text
http://127.0.0.1:8080/
```

Health:

```text
http://127.0.0.1:8080/api/health
```
