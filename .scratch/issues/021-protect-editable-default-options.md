# 021: Protect Editable Default Options During Language Switching

## Status

done

## Goal

Prevent language switching from collapsing a multi-line default option list, while still localizing untouched default questions.

## Background

The locale tree walker translated a `textarea` text node as if it were fixed interface copy. Because the whole multi-line option list was processed as one slash-separated string, only its final English segment survived in the English interface.

## Scope

- Exclude values inside editable form controls from generic locale translation.
- Update untouched default fields and default question labels/options individually when the language changes.
- Preserve user-edited fields, extra questions, saved activities, and registrations.

## Acceptance Criteria

- A new English event contains all nine English default single-choice options.
- A new Chinese event contains all nine Chinese default single-choice options.
- Entering an activity title does not stop untouched default questions and options from switching language.
- Editing an option list prevents it from being overwritten on language change.

## Implementation Notes

Text inputs and textareas are editable content surfaces even when they initially contain a system default. They must never be handled by the generic DOM text translator.
