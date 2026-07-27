# 015: Preserve User Content in English Mode

## Status

done

## Goal

Keep Chinese and other user-provided text visible in the English deployment.

## Background

The English locale observer translated every newly inserted text node. For a Chinese registration value without a matching built-in translation, it removed all Chinese characters, leaving the administrator table blank even though the server retained the original value.

## Scope

- Mark user-provided display content in event cards, registration tables, winner views, result pages, and draw screens.
- Make the locale translator skip marked user content.
- Preserve translation of fixed system copy.

## Implementation Notes

Use a `data-user-content` marker on rendered user values. The locale walker and its attribute pass must not transform text inside that marker.

## Acceptance Criteria

- In English mode, a Chinese user name such as `欧万` remains `欧万` after rendering.
- English built-in buttons and labels still translate.
- No server-side registration data migration is required.

## Verification

- English locale regression harness kept `欧万` unchanged both during initial rendering and when the draw screen inserted it through `textContent`; the fixed `作废` button label still translated to `Void`.
- `node --check frontend/public/main.js`, `node --check frontend/public/locale.js`, and `scripts\\build.cmd` completed successfully.
