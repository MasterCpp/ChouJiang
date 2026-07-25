# ADR 0005: Use a Bounded HTTP Request Executor

## Status

Accepted

## Context

J_Sys is expected to support events with fewer than 100 participants. A burst of page loads or form requests should not be processed through the JDK HTTP server's unspecified default executor, because that can create visible client-side queuing and timeouts.

## Decision

Use a fixed pool of 16 request workers with a bounded queue of 256 pending requests. On overload, the HTTP dispatcher runs the work itself (`CallerRunsPolicy`), applying backpressure instead of dropping the request.

## Consequences

- Normal bursts receive concurrent handling while thread usage remains bounded.
- The server does not create an unbounded number of request threads or pending tasks.
- Submission creation performs the event-email duplicate check and file write under one store lock, so concurrent same-email requests cannot create duplicates.
- This is a practical limit for the current lightweight two-core deployment; future heavier endpoints should be measured and tuned separately.
