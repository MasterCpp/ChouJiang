# Chinese-instance multi-account deployment checklist

This checklist applies only to the Chinese/bilingual runtime at `/opt/jsys`. Do not copy its database, environment variables, or service changes into the separate English runtime at `/opt/jsys-en`.

## Before migration

1. Schedule a short write pause for the Chinese instance.
2. Back up the complete Chinese `data/` directory, including `events.tsv`, `submissions.tsv`, `winners.tsv`, and `operations.tsv`. Keep the backup outside `/opt/jsys` and verify that it can be read.
3. Confirm the English service and its `data/` directory are not targets of this release.
4. On the first start only, place the following values in the protected Chinese service environment. Do not put them in source files, browser code, or this document.

   - `CHINA_ACCOUNT_EMAIL`, `CHINA_ACCOUNT_PASSWORD`: credentials for the migrated `中国账号` Owner.
   - `PLATFORM_ADMIN_EMAIL`, `PLATFORM_ADMIN_PASSWORD`: credentials for the internal Platform Administrator.

   Passwords must be 8–128 characters. After initial migration/seeding, the database retains password hashes and later restarts do not overwrite them.

## Migration and checks

1. Build the release with the bundled SQLite JDBC dependency.
2. Start the Chinese service once. The process creates `data/jsys.db`, imports legacy TSV records into the `中国账号` workspace, and records one-time migration metadata.
3. Verify `GET /api/health` returns HTTP 200.
4. Sign in with the China Owner and compare activity, registration, winner, and operation-record counts against the backup TSV files.
5. Open one historical registration, results, and screen URL. Each must resolve to its migrated activity.
6. Open the unlinked internal URL `/platform`; use the Platform Administrator credentials to confirm only workspace name, owner email, and status are displayed.
7. Create two disposable Branch Accounts and verify that neither can list or request the other's activity ID. Disable one in `/platform`, verify its Owner session and public activity URL become unavailable, then re-enable it.
8. In a browser, switch between Chinese and English on the Owner, registration, results, screen, and `/platform` pages. Confirm activity titles, questions, workspace names, and participant content remain unchanged.
9. Run the automated local regression suite before formal deployment:

   ```cmd
   scripts\test.cmd
   ```

10. Run the established 100-request read and unique-registration concurrency checks against a disposable Chinese activity. The Chinese runtime uses the same 16-worker / 256-queue bounded executor as the English runtime, but this release requires its own recorded run.

## Failure rollback

If the first migration fails validation or data counts do not match:

1. Stop the Chinese service; do not delete or edit the TSV backup.
2. Preserve the generated `data/jsys.db` for diagnosis rather than overwriting it.
3. Restore the complete backed-up Chinese `data/` directory and the prior release artifact, then start the prior service version.
4. Verify `/api/health` and one historical registration URL before reopening the system.

Do not use `git reset --hard` on a customer server as a rollback mechanism.
