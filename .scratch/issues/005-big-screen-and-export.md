# 005: Big Screen and Export

Status: done

## Goal

Implement the big-screen draw display and per-event Excel export.

## Scope

- Big-screen page for one event.
- Public result page for one event.
- Simple rolling animation.
- Final winner display.
- Excel export for all event data.
- Export operation record.

## Acceptance Criteria

- Given an event exists, when the big-screen URL is opened separately from the admin page, then the event title and draw area are visible.
- Given the admin starts a draw for the event, when the big-screen page is open, then it shows a simple rolling animation before the final result.
- Given the draw finishes, then the final winner names are clearly visible on the big-screen page.
- Given the draw finishes, when the public result page is opened, then the final winner names are visible without requiring admin login.
- Given no draw has happened yet, when the big-screen page is opened, then it shows an empty or waiting state instead of an error.
- Given no draw has happened yet, when the public result page is opened, then it shows a waiting state instead of an error.
- Given a winner has been voided or replaced by redraw, when the public result page is opened, then it shows only the latest valid winner list.
- Given an event has submissions, when the admin exports the event, then an Excel-compatible file is downloaded.
- Given the exported file is opened, then it includes name, job title, email, satisfaction score, single-choice answer, future discussion text, winner status, winning time, and void status if applicable.
- Given the event has no submissions, when the admin exports it, then the export still succeeds with headers and no data rows.
- Given an export is completed, when operation records are viewed, then the export action is recorded with event, operator, and timestamp.
- Given a winner has been voided or redrawn, when export is generated, then the exported winner status matches the latest stored state.

## Implementation Notes

- Big-screen route: `/screen/{eventId}`.
- Big-screen page shows a waiting state before draw.
- After draw, the big-screen page plays a short rolling-name animation before showing final winners.
- Export API: `GET /api/admin/events/{eventId}/export`.
- Export returns UTF-8 BOM CSV with `Content-Disposition` so Excel can open it.
- Export writes an `export` operation record.

## Verification

- `node --check frontend\public\main.js` completed successfully.
- `scripts\build.cmd` completed successfully.
- Export for an event with no submissions returned 200 with headers.
- Big-screen URL returned 200 before draw.
- Public result API returned `waiting` before draw.
- Public result API returned `completed` with valid winner after draw.
- Big-screen URL returned 200 after draw.
- After void and redraw, public result API showed only the latest valid winner.
- Export included participant data, winner status, and void status.
- Export action was recorded in operation records.
