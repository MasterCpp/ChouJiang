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

3. Start the app:

```text
java -cp backend/out com.jsys.App 8080
```

4. Open:

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
