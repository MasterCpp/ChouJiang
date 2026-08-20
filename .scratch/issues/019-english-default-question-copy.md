# 019: English Default Question Copy

## Status

done

## Goal

Show English-only default registration-question content when the interface begins in English.

## Background

The editable default question template contains bilingual text for the Chinese starting interface. The prior localization path ran only in the separate English instance, so the Chinese instance with a browser-local English choice still displayed the Chinese portions of that default template.

## Scope

- Localize only the unsaved built-in default event template at page boot when the interface language is English.
- Keep persisted activities and user-edited draft content unchanged.
- Do not change the separate English instance behaviour.

## Acceptance Criteria

- A new event page opened with English selected shows English-only default question labels and options.
- A Chinese page continues to show the existing bilingual defaults.
- Existing event, questionnaire, and registration content is not translated or changed.

## Implementation Notes

Use the existing `JSysLocale.isEnglish()` effective browser-local language state rather than checking only the separate English instance.
