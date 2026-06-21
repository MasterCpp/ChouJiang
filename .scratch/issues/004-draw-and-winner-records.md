# 004: Draw and Winner Records

Status: done

## Goal

Implement draw behavior, winner records, void, and redraw.

## Scope

- Draw configured winner count.
- Exclude existing valid winners from later draws.
- Store draw results.
- Void winner records.
- Redraw after voiding.
- Store operation records.

## Acceptance Criteria

- Given event A has enough eligible participants and a configured winning count of 3, when the admin starts the draw, then exactly 3 valid winners are created.
- Given event A has fewer eligible participants than the configured winning count, when the admin starts the draw, then the system prevents the draw or clearly reports that there are not enough eligible participants.
- Given a participant is already a valid winner for event A, when another draw is run for event A, then that participant is not selected again.
- Given a participant is a winner in event A, when a draw is run for event B, then event A's winner state does not affect event B.
- Given winners have been drawn, when the admin views winner records, then each record shows event, participant, status, draw time, and operation source if available.
- Given the admin voids a winner record, when the action is confirmed, then the winner status changes to voided and the original winner is no longer shown as a valid winner.
- Given a winner record has been voided, when the admin performs a replacement redraw, then a replacement winner is selected from participants who are not already valid winners and are not the just-voided participant for that replacement.
- Given draw, void, or redraw actions occur, when operation records are viewed, then each action includes action type, event, target record, operator, and timestamp.
- Given there are no eligible participants, when the admin starts a draw, then no winner is created and a clear message is shown.

## Implementation Notes

- Draw API: `POST /api/admin/events/{eventId}/draw`.
- Winner list API: `GET /api/admin/events/{eventId}/winners`.
- Void API: `POST /api/admin/events/{eventId}/winners/{winnerId}/void`.
- Redraw API: `POST /api/admin/events/{eventId}/winners/{winnerId}/redraw`.
- Operation list API: `GET /api/admin/events/{eventId}/operations`.
- Public result API now returns `waiting` when there are no valid winners and `completed` with valid winners after draw.
- Winner data is persisted to `data/winners.tsv`.
- Operation records are persisted to `data/operations.tsv`.

## Verification

- `node --check frontend\public\main.js` completed successfully.
- `scripts\build.cmd` completed successfully.
- Draw with too few eligible participants returned 409.
- Event with 4 participants and winning count 3 created exactly 3 winners.
- Repeat draw for the same event returned 409 when only 1 eligible participant remained.
- Winner state in event A did not block drawing the same email in event B.
- Winner records were visible to admin.
- Void changed a winner status to `voided`.
- Replacement redraw selected a different submission from the just-voided winner.
- Valid winner count after redraw remained 3.
- Operation records include draw, void, and redraw.
- Public result API returned `completed` with 3 valid winners.
