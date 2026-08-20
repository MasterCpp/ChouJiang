# 022: Explicit English Admin Interface Copy

## Status

done

## Goal

Remove residual Chinese and bilingual wording from the English activity-management page.

## Background

Two help-text sentences were partially or not translated by generic text replacement. Question-type options are intentionally excluded from generic translation to protect editable form values, so their system labels remained bilingual.

## Scope

- Supply explicit Chinese and English copy for the two activity-management help texts.
- Supply explicit Chinese and English copy for question-type select options.
- Preserve event title, question text, and choice-option values exactly as entered.

## Acceptance Criteria

- The English activity-management page has no Chinese in the two help texts or question-type options.
- The Chinese page shows the corresponding Chinese wording.
- Editable question labels and choice options are unchanged by the correction.

## Implementation Notes

Use `data-locale-zh` and `data-locale-en`, which the locale module renders directly and independently of generic text translation.
