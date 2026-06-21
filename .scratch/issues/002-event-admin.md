# 002: Event Admin

Status: done

## Goal

Implement one-admin login and event management.

## Scope

- Admin login.
- Event list.
- Create/edit event.
- Configure title, bilingual question copy, single-choice options, winning count, and privacy notice.
- Event status if needed.

## Acceptance Criteria

- Given the admin login page is open, when valid admin credentials are submitted, then the admin is redirected to the event list.
- Given the admin login page is open, when invalid credentials are submitted, then login is rejected and a clear error message is shown.
- Given an unauthenticated user opens an admin-only page, then they are redirected to login or blocked.
- Given the admin is logged in, when they create an event with title, bilingual question copy, single-choice options, winning count, and privacy notice, then the event is saved and appears in the event list.
- Given the admin is editing an existing event, when they update configurable copy, options, winning count, or privacy notice, then the saved registration page reflects the latest values.
- Given an event exists, when its detail page is opened, then it has a stable event ID that does not change after edits.
- Given an event exists, when the admin views its sharing section, then a registration link and QR code target are available.
- Given the admin enters an invalid winning count, such as zero or a non-number, then the event cannot be saved and a validation message is shown.
- Given the admin removes all single-choice options, then the event cannot be saved and a validation message is shown.

## Implementation Notes

- Default admin credentials are `admin / admin123`.
- Credentials can be overridden with `ADMIN_USERNAME` and `ADMIN_PASSWORD`.
- Sessions are in memory for the current local baseline.
- Event configuration is persisted to `data/events.tsv`.
- The registration link and QR target currently point to `/join/{eventId}`. The registration page itself is implemented in issue 003.

## Verification

- `scripts\build.cmd` completed successfully.
- Unauthenticated `GET /api/admin/events` returned 401.
- Invalid `POST /api/admin/login` returned 401.
- Valid `POST /api/admin/login` returned 200.
- Authenticated event creation returned an event ID.
- Event edit preserved the ID and updated event values.
- Invalid winning count returned 400.
- Empty single-choice options returned 400.
- Home page returned HTTP 200 and includes the admin login view.
