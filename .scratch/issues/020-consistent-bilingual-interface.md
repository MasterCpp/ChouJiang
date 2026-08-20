# 020: Consistent Chinese and English Interface Copy

## Status

done

## Goal

Make Chinese interface copy Chinese-only and English interface copy English-only, while preserving all user-entered activity and registration data.

## Background

The existing UI contains many built-in labels in `中文 / English` form. It also converted the default event template only at page boot, which left an English default template visible after a user switched back to Chinese.

## Scope

- Render built-in bilingual labels in the active interface language across dynamically inserted page content.
- Rebuild only an untouched, unsaved default event template when the browser language changes.
- Localize built-in activity status labels.
- Leave existing activities, participant records, workspace names, and any edited draft fields unchanged.

## Acceptance Criteria

- Chinese mode displays Chinese-only system labels and Chinese-only default question text.
- English mode displays English-only system labels and English-only default question text.
- Switching language updates a pristine new-event template in place.
- Switching language does not alter saved activities or edited draft content.
- Activity status labels follow the interface language.

## Implementation Notes

Use an explicit locale-change event and derive defaults from an immutable bilingual source template. Translation exclusion continues to protect user-entered DOM content.
