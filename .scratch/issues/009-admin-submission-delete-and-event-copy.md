# 009 Admin Submission Delete and Event Copy

Status: done

## Goal

Support two onsite admin needs:

- Delete an individual registration from an event.
- Copy an existing event's configuration into a clean new event.

## Scope

- Add a delete action to each admin registration row.
- Deleting a registration removes the registration and any winner records tied to that submission.
- Deleting a registration writes a `delete_submission` operation record.
- Add a copy action to each event card.
- Copying an event duplicates only event settings and dynamic question configuration.
- Copied events do not include submissions, winner records, or operation history.

## Acceptance Criteria

- Admin can delete one registration from the submission list.
- Deleting a registration with a winner record removes that related winner record.
- Admin winner list no longer shows deleted-submission winner records.
- Admin can copy an event and receives a new event ID.
- Copied event keeps title, winner count, privacy notice, and dynamic registration questions.
- Copied event has zero submissions and zero winners.
