# Status

## Current State

Initial implementation is complete. The project now has a runnable local/demo application, customer-feedback UI changes, and dynamic per-event registration form configuration.

## Completed

- Replaced the default JDK HTTP request dispatcher with a bounded 16-worker executor and overload backpressure for concurrent participant page loads and submissions.

- Fixed the English instance winner actions that could display as empty buttons, and made winner/activity timestamps display explicitly in UTC+8.

- Added an English-only locale mode and isolated `jsys-en` deployment templates for a second, independently managed instance on port 8081.

- Added a formal Linux service-recovery guard: boot-time service enablement, automatic Java-process restart, and a one-minute local health-check timer that restarts an unhealthy service.

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
- Added dynamic per-event registration form configuration with single-choice, multiple-choice, text-answer, and 1-10 score questions.
- Updated public registration, admin submission display, submission validation, and CSV export to use dynamic event questions.
- Changed draw flow so the configured winner count is the final quota, while each draw action selects one winner.
- Added admin deletion for individual registrations, including cleanup of related winner records.
- Added event copy so a new activity can reuse an existing event's settings and dynamic questions without copying submissions or winners.

## Current Issue

Latest completed implementation issue:

```text
.scratch/issues/013-admin-time-and-winner-action-labels.md
```

Next implementation issue:

```text
English instance concurrency verification is complete. Next step is visual QA of the English winner actions and UTC+8 timestamps after a browser refresh.
```

## Next Steps

1. Manually QA the dynamic form builder in the browser.
2. Prepare a customer demo activity using the customer's final bilingual copy.
3. Confirm whether formal delivery uses a public IP only or a domain plus HTTPS.
4. Update deployment data backup and admin password before formal delivery.

## Open Questions

- Final UI style and branding.
- Final bilingual copy for each event.
- Whether formal delivery will use a real domain plus HTTPS or only a public IP.
- Exact formal server configuration after customer confirms the Alibaba Cloud Hong Kong server.

## Latest Verification

- Public English test activity `694a10e9-ceba-4194-9809-598d08c3e14b` passed a 100-user concurrent read test: 100 HTTP 200 responses, no timeout or non-200 response, and a 2.088-second slowest response.
- The same activity passed a 100-user concurrent registration test: 100 HTTP 201 responses, no timeout or other error, and a 2.07-second slowest response. The activity contains those clearly named `loadtest` submissions for later cleanup.
- `scripts\\build.cmd` completed successfully after the bounded HTTP executor change.
- A temporary local server handled 100 simultaneous read-only home-page requests: 100 HTTP 200 responses, with the slowest response at 0.97 seconds.
- A temporary event handled 100 same-email concurrent submissions correctly: 1 created submission, 99 duplicate rejections, and 1 saved data row.
- `scripts\\verify-local.cmd` passed against a temporary local server after the change.
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
  - draw with no eligible participants returned 409.
  - each draw action creates exactly 1 winner.
  - repeated draws fill the configured winner quota one winner at a time.
  - drawing after the valid winner count reaches the configured quota returns 409.
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
- Dynamic registration form verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - Temporary server smoke test created an event with single-choice, multiple-choice, text, and score questions.
  - Public submission accepted dynamic answers, including multiple-choice answers.
  - Admin submission API returned the dynamic answer map.
  - CSV export included dynamic question headers.
  - Draw still selected a winner from the dynamically submitted participant.
- Multiple-choice answer display polished:
  - Admin submission table now shows each selected multiple-choice answer on its own line.
  - CSV export continues to keep multiple-choice answers line-separated inside the answer cell.
- One-at-a-time draw flow verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - Draw action now creates one winner per click.
  - Draw action returns a quota-full error after valid winners reach the configured winner count.
  - Redraw error copy now explains when there are not enough remaining eligible participants.
  - Big-screen page shows current winner progress and can continue drawing until the quota is full.
  - Big-screen page uses `下一位中奖者 / Next Winner` for continuing onsite draws.
  - Big-screen page uses `换一位 / Pick Another` to replace the latest winner while backend records keep `void` and `redraw` audit actions.
  - Replacement smoke test confirmed `换一位 / Pick Another` creates one replacement when an eligible participant remains.
  - Replacement smoke test confirmed no-replacement errors do not void the current winner first.
  - No-replacement feedback now shows a Chinese-friendly alert: `没有可替换候选人了，请保留当前中奖者或增加报名候选人。`

- Admin submission deletion and event copy verified:
  - `node --check frontend\public\main.js` completed successfully.
  - `scripts\build.cmd` completed successfully.
  - Temporary server smoke test copied an event and confirmed the copied event had the same question configuration and zero submissions.
  - Temporary server smoke test deleted a registration tied to a winner and confirmed the related winner record was removed.
  - Operation records contained `delete_submission`.

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
