# 018: Visible Chinese/English Language Switch

## Status

done

## Goal

Keep the Chinese-instance language control visible and understandable after either language is selected.

## Background

The floating language control is inserted dynamically. When English is active, the mutation observer translates the control itself; its Chinese target label has no English replacement and becomes blank.

## Scope

- Show a stable `中文 / English` label on the Chinese-instance language control.
- Exclude the language control from content translation.
- Preserve any untranslated built-in Chinese copy instead of rendering it as an empty string.
- Do not alter user-entered event or registration content, and do not change the separate English instance.

## Acceptance Criteria

- The language control remains visible after switching in either direction.
- Clicking it still changes built-in interface language and remembers the browser choice.
- Unmapped built-in Chinese copy remains readable rather than disappearing in English mode.
- User-entered content remains unmodified.

## Implementation Notes

The control is system UI rather than user content, so mark it with a dedicated translation-exclusion attribute.
