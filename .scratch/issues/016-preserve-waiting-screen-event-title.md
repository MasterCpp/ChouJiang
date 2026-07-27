# 016: Preserve Event Title on the Waiting Draw Screen

## Status

done

## Goal

Keep a mixed-language event title unchanged on the English draw screen before a winner is drawn.

## Background

Issue 015 protected user content in registration, result, and rolling/complete screen views. The waiting draw-screen branch still rendered the event title without the user-content marker, so the English translator reduced `Test7/29测试` to `Test7`.

## Scope

- Mark the waiting-screen event title as user content.
- Align the remaining legacy join-form title rendering with the same marker.

## Implementation Notes

Reuse the existing `data-user-content` boundary; do not change stored event data or the fixed English screen copy.

## Acceptance Criteria

- In English mode, `Test7/29测试` remains unchanged on the waiting draw screen.
- The waiting-state system copy continues to show in English.
- Front-end syntax and build checks pass.

## Verification

- Source scan found no event-title heading rendered without `data-user-content`.
- English locale regression kept the mixed-language title unchanged inside a marked waiting-screen title while translating fixed system text to English.
- `node --check frontend/public/main.js`, `node --check frontend/public/locale.js`, and `scripts\\build.cmd` completed successfully.
