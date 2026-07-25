# 013: Clarify Admin Times and Winner Actions

## Status

done

## Goal

Make winner action buttons readable in the English instance and show administrator timestamps in a human-readable, explicit time zone.

## Background

Winner buttons use Chinese action labels rendered after the page loads. The English translation table did not include the labels for voiding or redrawing, so the translator removed the Chinese characters and left empty button outlines. Winner and operation timestamps were shown as raw UTC ISO strings ending in `Z`.

## Scope

- Add English translations for the two winner actions.
- Format winner and operation timestamps as UTC+8 (Hong Kong/China) administrator time.
- Preserve stored timestamps and all event data.

## Implementation Notes

Use the existing locale translation mechanism and browser-side `Intl.DateTimeFormat` formatting with the `Asia/Shanghai` time zone (the same UTC+8 offset as Hong Kong).

## Acceptance Criteria

- English winner controls display `Void` or `Redraw`, never an empty button.
- A stored timestamp ending in `Z` displays as `YYYY-MM-DD HH:mm:ss UTC+8` in winner and activity tables.
- The Java build and front-end syntax check pass.

## Verification

- English locale test: `作废` renders as `Void`; `补抽` renders as `Redraw`.
- `2026-07-25T11:37:42.831889287Z` formats as `2026-07-25 19:37:42 UTC+8`.
- `node --check frontend/public/main.js` and `scripts\\build.cmd` completed successfully.
