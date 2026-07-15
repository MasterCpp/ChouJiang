# ADR 0003: Use Systemd and a Local Health Timer for Service Recovery

## Status

Accepted

## Context

J_Sys is formally deployed as one Java process on a customer-provided Linux server. A 502 was observed while accessing the admin page. Process restart alone cannot recover a process that remains running but no longer answers its local health endpoint.

## Decision

- Run the Java process as `jsys.service`, enabled at boot and configured with `Restart=always` and a three-second restart delay.
- Run a systemd timer once per minute that requests `http://127.0.0.1:8080/api/health` with an eight-second timeout. On a failed, timed-out, or non-OK response it restarts `jsys.service`.
- Install the health-check script outside the application release directory at `/usr/local/lib/jsys/` so an application update cannot leave a partially copied executable.
- Continue to use the existing root service account temporarily. Moving to a dedicated non-login service user is separate hardening work because it changes ownership of long-lived event data.
- Require an external HTTP monitor for public-IP, DNS, firewall, or entire-server failures; a host cannot observe or remediate its own loss of external reachability.

## Consequences

- Java exit and local API failures recover automatically, usually within about one minute plus restart time.
- An outage has an attributable systemd journal record for follow-up diagnosis.
- The system does not automatically reboot the server or modify its network stack, avoiding a recovery loop that could make remote access worse.
