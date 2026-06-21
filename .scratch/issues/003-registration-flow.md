# 003: Registration Flow

Status: done

## Goal

Implement the participant registration H5 page.

## Scope

- Mobile-friendly registration page.
- Required fields:
  - Name
  - Job title
  - Email
  - Satisfaction score 1-10
  - Single-choice topic answer
  - Future discussion free text
- Privacy notice.
- Event-scoped email dedupe.
- Success page with a "View Winners / 查看中奖结果" action.

## Acceptance Criteria

- Given an active event registration link, when a participant opens it on a mobile-sized browser, then the form is readable and usable without horizontal scrolling.
- Given a participant opens the registration page, then the page shows the event title, bilingual form copy, all required fields, and the privacy notice.
- Given a participant submits the form with any required field missing, then submission is rejected and the missing field is indicated.
- Given a participant enters an invalid email format, then submission is rejected and an email validation message is shown.
- Given a participant submits valid name, job title, email, satisfaction score, single-choice answer, and future discussion text, then the submission is saved and a success confirmation is shown.
- Given a participant submits successfully, then the success page includes a "View Winners / 查看中奖结果" button or link for that event.
- Given a participant clicks "View Winners / 查看中奖结果" before the draw finishes, then the result page shows a waiting state.
- Given a participant has submitted `test@example.com` for event A, when the same email submits event A again, then the second submission is rejected as a duplicate.
- Given a participant has submitted `test@example.com` for event A, when the same email submits event B, then the submission is allowed.
- Given a successful submission exists, when the admin views event submissions, then the participant and all questionnaire answers are visible.
- Given the event is not available for registration, when a participant opens its registration link, then the page shows a clear unavailable message instead of accepting submissions.

## Implementation Notes

- Public registration route: `/join/{eventId}`.
- Public result route: `/results/{eventId}`.
- Public event API: `GET /api/events/{eventId}`.
- Public submission API: `POST /api/events/{eventId}/submissions`.
- Public result API: `GET /api/events/{eventId}/results`.
- Admin submission list API: `GET /api/admin/events/{eventId}/submissions`.
- Submission data is persisted to `data/submissions.tsv`.
- The result page currently shows a waiting state. Winner display is completed in issue 005 after draw logic exists.

## Verification

- `node --check frontend\public\main.js` completed successfully.
- `scripts\build.cmd` completed successfully.
- Valid submission returned a submission ID.
- Duplicate email in the same event returned 400.
- Same email in another event was accepted.
- Invalid email returned 400.
- Missing required name returned 400.
- Closed event registration returned 409.
- Admin submission list returned saved participant and questionnaire answers.
- Public result API returned `waiting`.
