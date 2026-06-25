# 007 Dynamic Registration Form

Status: done

## Goal

Allow each event to configure its own registration questions instead of using only the fixed first-version questionnaire.

## Scope

- Admin can add and delete event questions.
- Supported question types:
  - single choice
  - multiple choice
  - text answer
  - score from 1 to 10
- Each question can be marked required or optional.
- Choice questions can configure their own options.
- Public registration renders questions from the event configuration.
- Submission validation follows the event question configuration.
- Admin submission list and CSV export include the dynamic question answers.
- Existing fixed-form events and submissions remain readable through compatibility fields.

## Acceptance Criteria

- Creating an event with single-choice, multiple-choice, text, and score questions succeeds.
- Public registration accepts valid answers for all four question types.
- Required dynamic questions reject blank answers.
- Multiple-choice answers persist and are shown in the admin submission list.
- CSV export uses the event's current question labels as dynamic columns.
- Draw, winner, void, redraw, and winner deletion continue to work with dynamically configured forms.

## Implementation Notes

- The current zero-dependency Java server is kept.
- Dynamic questions are stored as an additional encoded column in `data/events.tsv`.
- Dynamic answers are stored as an additional encoded column in `data/submissions.tsv`.
- Old event and submission rows are converted in memory to the default three-question shape.
