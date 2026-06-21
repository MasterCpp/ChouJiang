# 001: Project Scaffold

Status: done

## Goal

Create the initial application scaffold for the H5 lucky draw system.

## Scope

- Choose and initialize the first-version stack.
- Add frontend, backend, and database structure.
- Add local development run instructions.
- Keep the scaffold minimal and aligned with `CONTEXT.md`.

## Acceptance Criteria

- Given a fresh checkout of the repo, when the documented setup command is run, then required dependencies are installed or clearly reported as missing.
- Given setup has completed, when the documented start command is run, then the app starts locally without uncaught startup errors.
- Given the app is running locally, when the browser opens the documented local URL, then a basic landing page or health page is visible.
- Given the app is running locally, when the backend health endpoint is requested, then it returns a successful response.
- Given the repo is opened by a new developer, when they read `README.md`, then they can find setup, start, test, and project-structure instructions.
- Given the project is scaffolded, when files are inspected, then the structure has clear places for frontend, backend, database/configuration, and documentation.
- Given no production server is available, when the app runs locally, then it does not require a paid cloud service or formal domain.

## Notes

The expected production direction is Vue3 frontend plus Java backend, but final scaffolding should match the actual implementation constraints in this workspace.

## Implementation Notes

- Initial scaffold uses Java 17 standard library HTTP server and static H5 files.
- This avoids npm execution-policy issues and external dependency downloads for the first runnable baseline.
- Future issues can replace or extend this with Vue3/Spring Boot if the project needs a heavier framework.

## Verification

- `scripts\build.cmd` completed successfully.
- Temporary local server verified with `java -cp backend\out com.jsys.App 8080`.
- `http://127.0.0.1:8080/api/health` returned `status: ok`.
- `http://127.0.0.1:8080/` returned HTTP 200 and the landing page content.
