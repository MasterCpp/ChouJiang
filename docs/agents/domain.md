# Domain Docs

This repo uses a single-context layout.

Primary domain context:

```text
CONTEXT.md
```

Architecture decisions:

```text
docs/adr/
```

Rules for agents:

- Read `CONTEXT.md` before changing product behavior.
- Add an ADR under `docs/adr/` for major architecture decisions, such as database choice, deployment topology, authentication approach, or replacing the backend framework.
- Do not create separate frontend/backend domain contexts unless the project grows enough to need a monorepo-style split.
