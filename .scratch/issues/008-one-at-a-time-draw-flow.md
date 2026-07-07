# 008 One-at-a-Time Draw Flow

Status: done

## Goal

Change the draw flow so the configured winner count is the final quota, while each draw action selects only one winner.

## Background

Customer feedback from onsite usage: the host wants to announce winners one by one. If a selected winner is not present, the host can void that winner and draw another candidate. The previous behavior drew the full configured winner count in one action, which made onsite replacement confusing when registrations were close to the winner quota.

## Scope

- Keep `winningCount` as the final number of valid winners required for the event.
- Each `/draw` action creates only one winner.
- Prevent drawing after the valid winner count reaches `winningCount`.
- Keep void and redraw audit behavior.
- Make redraw errors clearer when no eligible participant remains.
- Let the big-screen page continue drawing while valid winners are below the configured winner count.
- Show draw progress on the big-screen page.
- Let the presenter replace the latest winner directly from the big-screen page using onsite-friendly copy.

## Acceptance Criteria

- With 3 registered participants and `winningCount = 2`, the first draw creates 1 winner.
- A second draw creates the second winner.
- A third draw returns a quota-full error.
- If one winner is voided and another eligible participant remains, redraw creates 1 replacement winner.
- If no eligible participant remains, redraw explains that there are not enough remaining registrations.
- The big-screen page shows the current valid winner count against the target count.
- The big-screen page shows `下一位中奖者 / Next Winner` for continuing the draw.
- The big-screen page shows `换一位 / Pick Another` for replacing the latest winner without showing the word void onsite.
