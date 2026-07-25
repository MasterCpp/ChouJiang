# Production Service Recovery Guard

## Goal

Make the formal Linux deployment recover automatically when the Java process exits or the local health endpoint becomes unavailable.

## Background

The customer saw a 502 while entering the admin page. `jsys.service` already restarts an exited Java process, but it had no independent local health probe for a process that remains running while unhealthy.

## Scope

- Version the systemd service, health-check service, timer, and installer under `scripts/linux/`.
- Check `http://127.0.0.1:8080/api/health` once per minute and restart `jsys.service` after a failed or timed-out response.
- Document verification, recovery logs, and the boundary between local recovery and cloud-network failures.
- Do not add an in-app monitoring dashboard or automatic server/network reboot.

## Acceptance Criteria

- `jsys.service` is enabled on boot and restarts after an unexpected Java exit.
- `jsys-healthcheck.timer` is enabled and calls the local health endpoint at least once per minute.
- A failed local health check restarts `jsys.service` and writes an identifiable journal message.
- The installer validates Java, curl, and compiled application output before changing systemd units.
- Deployment documentation includes installation, verification, and recovery-log commands.
- The documentation explicitly states that an external monitor is needed to detect a server or cloud-network outage.

## Implementation Notes

The intended formal app directory is `/opt/jsys`; the server currently uses the root account for the existing service. Migrate to a dedicated non-login service user only in a separately planned hardening task.

## Status

done

## Verification

- `scripts/linux/install-systemd.sh` installs and enables `jsys.service`, `jsys-healthcheck.service`, and `jsys-healthcheck.timer` for `/opt/jsys`.
- The local health probe uses `http://127.0.0.1:8080/api/health` with an eight-second timeout and records failed checks in the system journal.
- The deployment guide documents both internal recovery limits and the required external monitor.
