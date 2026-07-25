# Operations Record

This document records the completed production-facing changes and their operating boundaries. It contains no administrator credentials or event data.

## Completed Releases

| Commit | Change | Operating result |
| --- | --- | --- |
| `6354655` | Service recovery guard | `jsys.service` restarts the Java process; a one-minute local health timer checks port 8080. External monitoring is still required for public-network and whole-server outages. |
| `dc95c14` | English isolated instance | English mode runs independently from `/opt/jsys-en` on port 8081, with separate service, administrator configuration, and runtime data. |
| `94d0403` | Bounded concurrent request handling | The Java HTTP server uses 16 request workers and a bounded queue; same-event email deduplication is atomic during concurrent submission. |
| `914aea4` | English winner action and time display | English winner actions display `Void` and `Redraw`; winner and activity timestamps display as `UTC+8`. |
| `1cee179` | Live concurrency verification record | The English public instance passed 100 concurrent page reads and 100 concurrent unique-email submissions without an error or timeout. |

## Current Deployment Shape

| Instance | Working directory | Service | Local health endpoint | Purpose |
| --- | --- | --- | --- | --- |
| Bilingual/default | `/opt/jsys` | `jsys.service` | `http://127.0.0.1:8080/api/health` | Existing customer instance |
| English | `/opt/jsys-en` | `jsys-en.service` | `http://127.0.0.1:8081/api/health` | Independently managed English customer instance |

The two instances must not share `data/` or administrator credential files.

## Safe English-Instance Update

The English server currently retains one intentional local source change in `backend/src/main/java/com/jsys/App.java`: its listener binds to `0.0.0.0` so the public port 8081 remains reachable. Preserve that change before a Git update; do not use `git reset --hard`.

```bash
cd /opt/jsys-en
git status --short
git stash push -m "keep-8081-public-listen" -- backend/src/main/java/com/jsys/App.java
git pull --ff-only
git stash apply
javac -encoding UTF-8 -d backend/out backend/src/main/java/com/jsys/App.java
systemctl restart jsys-en
curl -fsS http://127.0.0.1:8081/api/health
```

If `git stash apply` reports a conflict, stop and inspect the conflict before compiling or restarting. After confirming the listener change and service health, remove the temporary stash with `git stash drop`.

## Latest Live Verification

On 2026-07-25, a dedicated English test event completed:

- 100 concurrent read-only registration-page requests: 100 HTTP 200 responses, no timeout or non-200 response; slowest response 2.088 seconds.
- 100 concurrent unique-email registrations: 100 HTTP 201 responses, no timeout or other error; slowest response 2.07 seconds.

The clearly named `loadtest` registrations remain in that test event until an administrator deletes them. Do not use that event for a real draw.
