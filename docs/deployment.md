# Deployment Guide

This project can run locally first, then move to a customer-provided server.

## Recommended Server Baseline

For Alibaba Cloud Hong Kong and fewer than 100 participants per event:

```text
CPU: 2 cores
Memory: 2 GB minimum, 4 GB preferred
Disk: 40 GB minimum
Bandwidth: 3-5 Mbps minimum
OS: Ubuntu LTS
Runtime: Java 17
```

Avoid `1 core / 1 GB` for long-term use. It may run the current lightweight app, but it leaves little room for Java, logs, data backup, and future features.

## Public IP vs Domain

A server public IP can work:

```text
http://server-ip:8080/
```

This is acceptable for testing or a minimal first delivery, but it is not ideal for long-term production:

- It looks less professional.
- HTTPS is harder to configure cleanly.
- If the server changes, the QR code link changes.
- Some browsers may show security warnings without HTTPS.

Preferred long-term setup:

```text
domain + HTTPS + reverse proxy
```

If the customer does not buy a domain, clearly state that delivery uses a public IP and HTTPS/domain setup is not included.

## Required Software

On the server:

```text
Java 17
Nginx, optional but recommended for port 80/443 reverse proxy
Firewall rule allowing HTTP/HTTPS or the chosen app port
```

## Basic Deployment Shape

1. Copy the project folder to the server.
2. Run:

```text
scripts\build.cmd
```

On Linux, use the equivalent Java compile command:

```text
mkdir -p backend/out
javac -encoding UTF-8 -d backend/out backend/src/main/java/com/jsys/App.java
```

3. Build the app and install the service guard (recommended for every formal deployment):

```text
cd /opt/jsys
mkdir -p backend/out
javac -encoding UTF-8 -d backend/out backend/src/main/java/com/jsys/App.java
sudo bash scripts/linux/install-systemd.sh
```

The installer enables all of the following:

- `jsys.service`: starts on server boot and restarts the Java process three seconds after an unexpected exit.
- `jsys-healthcheck.timer`: calls `http://127.0.0.1:8080/api/health` every minute. A timeout, connection failure, or non-OK result restarts `jsys.service`.
- Journal records: check `journalctl -u jsys -u jsys-healthcheck -f` while investigating an outage.

4. Start the app manually only for a short-lived test:

```text
java -cp backend/out com.jsys.App 8080
```

5. Open:

```text
http://server-ip:8080/
```

## Formal Verification Checklist

After deployment, verify:

- Admin page opens.
- Admin login works.
- Event can be created.
- Registration link opens from a phone.
- QR code opens the registration page.
- Participant submission succeeds.
- Duplicate email in the same event is rejected.
- Admin can view submissions.
- Draw works.
- Big-screen page shows the winner.
- Public result page shows the winner.
- Void and redraw work.
- Export downloads a CSV.
- Operation records include draw, void, redraw, and export.
- `systemctl is-enabled jsys` returns `enabled`.
- `systemctl is-enabled jsys-healthcheck.timer` returns `enabled`.
- `systemctl list-timers jsys-healthcheck.timer --no-pager` shows a future run time.

## Availability and Recovery

The local recovery guard repairs an exited or unhealthy Java process. It cannot repair an Alibaba Cloud network outage, DNS failure, firewall rule, or a server that is entirely offline: the server cannot observe those failures from inside itself.

For customer-facing availability, also configure an **external** HTTP monitor (for example, Alibaba Cloud CloudMonitor) against:

```text
http://server-ip:8080/api/health
```

Use a one-minute check interval and alert on one or two consecutive failures. The alert must go to the delivery owner, so a cloud-network failure is visible immediately rather than being discovered by the customer.

When a domain and HTTPS are configured, monitor the final `https://your-domain/api/health` address instead of the raw IP.

After any outage, collect these before restarting anything else:

```text
journalctl -u jsys -u jsys-healthcheck --since "30 minutes ago" --no-pager
systemctl status jsys jsys-healthcheck.timer --no-pager
```

## Data Retention and Backup

Runtime data files:

```text
data/events.tsv
data/submissions.tsv
data/winners.tsv
data/operations.tsv
```

Back up the `data/` folder regularly. For formal long-term use, copy it before upgrades and after each event.

Suggested simple backup:

```text
copy data backup-data-YYYYMMDD
```

For Linux:

```text
cp -r data backup-data-YYYYMMDD
```

## Notes Before Production

- Change the default admin password.
- Keep a copy of the customer-approved bilingual copy.
- Confirm whether the customer accepts public IP access or wants a domain.
- If using HTTPS, configure Nginx and a certificate before sharing the final QR code.
