# 006: Demo and Deployment Path

Status: done

## Goal

Prepare local demo, temporary customer scan test, and formal deployment guidance.

## Scope

- Local run documentation.
- Demo script for recording a video.
- Temporary public tunnel guidance.
- Customer server deployment notes for an Alibaba Cloud Hong Kong node.
- Clarify public IP versus domain plus HTTPS.

## Acceptance Criteria

- Given the app is complete enough for demo, when the user follows `README.md`, then they can start the local frontend and backend.
- Given the local app is running, when the user follows the demo script, then they can show admin login, event creation, QR/link sharing, participant registration, draw, big-screen result, void/redraw, and export.
- Given the user wants to record a demo video, then the documentation lists the recommended demo sequence and sample test data.
- Given the user wants a customer scan test before formal deployment, then the documentation explains how to expose the local app through a temporary public tunnel and how to generate/share the temporary QR code.
- Given the user uses a temporary tunnel, then the documentation clearly says it is for demo only and depends on the local machine staying online.
- Given the customer provides an Alibaba Cloud Hong Kong server, then the deployment checklist states the recommended baseline configuration and required software.
- Given the customer only has a server public IP, then the deployment notes explain that IP access can work for testing but a real domain plus HTTPS is better for long-term formal use.
- Given formal deployment is complete, then the checklist includes verification of admin login, registration link, QR code, submission, draw, export, and backup/data-retention notes.

## Implementation Notes

- Demo guide: `docs/demo.md`.
- Deployment guide: `docs/deployment.md`.
- Local verification helper: `scripts\verify-local.cmd`.
- Temporary scan testing is documented with Cloudflare Tunnel, but it is explicitly marked as demo-only.
- Formal deployment notes cover Alibaba Cloud Hong Kong baseline configuration and the tradeoff between public IP and domain plus HTTPS.

## Verification

- `docs/demo.md` includes local start instructions.
- `docs/demo.md` includes demo data and a recording sequence.
- `docs/demo.md` explains temporary public tunnel use and its limits.
- `docs/deployment.md` states recommended Alibaba Cloud Hong Kong server baseline.
- `docs/deployment.md` explains public IP access versus domain plus HTTPS.
- `docs/deployment.md` includes formal deployment verification checklist.
- `docs/deployment.md` includes data backup and retention notes.
- `scripts\verify-local.cmd` checks health endpoint and home page after the app is running.
- `scripts\verify-local.cmd` passed against a temporary local server.
