# 024: Descending Score Options

## Status

done

## Goal

Show all Chinese-instance questionnaire score options in descending order: 10 through 1.

## Background

The customer wants 10 at the top of the numeric choices instead of scrolling past 1 through 9.

## Scope

- Apply to existing and new activities, in either interface language of the Chinese instance.
- Keep the empty Select placeholder first and require an explicit participant choice.
- Keep submitted numeric values and historical answers unchanged.
- Preserve the independent English instance's existing ascending order.

## Acceptance Criteria

- Every Chinese-instance score dropdown lists the empty placeholder, then 10, 9, 8, 7, 6, 5, 4, 3, 2, 1.
- Each option value equals its visible score; no score is preselected.
- The independent English instance continues to list 1 through 10.

## Implementation Notes

Share numeric score-option rendering between both registration templates. Use the deployment-instance flag, not the selected UI language, to choose ordering.

Verified generated dropdown option order and matching numeric values for both deployment modes, the empty initial choice, JavaScript syntax, and the complete project build/integration test command (exit 0).
