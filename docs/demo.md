# Demo Guide

This guide is for recording a customer demo video or walking a customer through a temporary scan test.

## Start Locally

From the project root:

```text
scripts\start-dev.cmd
```

Open:

```text
http://127.0.0.1:8080/
```

Login:

```text
username: admin
password: admin123
```

## Demo Data

Create an event with:

```text
Event title:
Tech Sharing Lucky Draw / 技术分享抽奖

Satisfaction question:
您对今日主题分享的整体满意程度？ / Overall satisfaction with today's sharing?

Single-choice question:
您今天最满意哪方面的分享？ / Which topic are you most satisfied with today?

Single-choice options:
Flash 技术趋势与 Roadmap 更新 / Flash technology trends and roadmap
DRAM 产业现状与未来产品蓝图 / DRAM industry and product roadmap
GD32 MCU 产品全景呈现与开发生态支持 / GD32 MCU portfolio and ecosystem

Free-text question:
您期待后续哪方面的深入交流？ / What would you like to discuss further?

Privacy notice:
提交信息仅用于本场活动抽奖和会后沟通。/ Your information is used only for this event lucky draw and follow-up communication.

Winning count:
1

Status:
active
```

Register at least two participants:

```text
Alice / Engineer / alice@example.com
Bob / Manager / bob@example.com
```

## Video Sequence

1. Open the admin page and log in.
2. Create an event with the demo data above.
3. Show the generated registration link, result link, and big-screen link.
4. Open the registration link as a participant.
5. Submit one participant.
6. Show duplicate email rejection for the same event if needed.
7. Submit another participant with a different email.
8. Return to admin and view submissions.
9. Open the big-screen link in another browser window.
10. Click "Start Draw" in admin.
11. Show the big-screen rolling animation and final winner.
12. Open the public result link and show the winner list.
13. Void the winner and redraw.
14. Show operation records.
15. Export the event data and open the CSV in Excel or WPS.

## Temporary Customer Scan Test

For a remote customer scan test, the local URL `http://127.0.0.1:8080/` is not enough. The customer phone cannot access your local machine directly.

Use a temporary public tunnel only for demo:

```text
cloudflared tunnel --url http://127.0.0.1:8080
```

Cloudflare will print a temporary `https://...trycloudflare.com` URL. Use that public URL to build the customer-facing links:

```text
https://temporary-url.trycloudflare.com/join/{eventId}
https://temporary-url.trycloudflare.com/results/{eventId}
https://temporary-url.trycloudflare.com/screen/{eventId}
```

Then generate a QR code from the registration URL with any QR generator.

Important limits:

- This is for demo only.
- Your local computer must stay on.
- The local app must keep running.
- The tunnel must keep running.
- If the computer sleeps, disconnects, or the tunnel exits, the customer link stops working.
- Formal delivery should use a server address, not a local tunnel.
