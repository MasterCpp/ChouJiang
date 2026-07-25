# 012: Bounded Concurrent HTTP Request Executor

## Status

done

## Goal

Handle concurrent participant page loads and submissions without the request queuing caused by the HTTP server's default dispatcher.

## Background

The application passed `null` to `HttpServer.setExecutor`, leaving request dispatch to the JDK default executor. Under a burst of concurrent browser requests, this can serialize or queue requests long enough for clients to time out.

## Scope

- Configure only the application's in-process HTTP request handling.
- Keep event-scoped email deduplication correct when submissions are processed concurrently.
- Do not change public ports, event data, authentication, or reverse-proxy configuration.

## Implementation Notes

Use a named, fixed-size request executor with 16 workers and a queue of 256 requests. When that bounded queue is full, `CallerRunsPolicy` applies backpressure instead of rejecting a request abruptly.

## Acceptance Criteria

- The application no longer calls `server.setExecutor(null)`.
- The request worker count and queue capacity are explicit and bounded.
- A local 100-request concurrent read-only smoke test returns HTTP 200 for every request.
- Concurrent requests using the same email create exactly one submission for an event.
- The normal local verification and Java build still pass.

## Verification

- `scripts\\build.cmd` completed successfully.
- A temporary local server handled 100 parallel `GET /` requests: 100 HTTP 200 responses, with the slowest response at 0.97 seconds.
- A temporary event received 100 simultaneous same-email submissions: 1 returned `201`, 99 were rejected with `400`, and exactly 1 submission was saved.
- `scripts\\verify-local.cmd` passed against a temporary local server.
