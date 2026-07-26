# 014: Remove Login Credential Prefill

## Status

done

## Goal

Ensure the public administrator login page does not display any prefilled username or password.

## Background

The login form prefilled `admin` and `admin123`, making it appear that the current production administrator credentials were exposed. The English instance uses its configured environment-file credentials, but the visible prefill was misleading and unnecessary.

## Scope

- Remove login-form credential prefill values only.
- Preserve the existing administrator credential loading and deployment behavior.

## Implementation Notes

Keep username and password autocomplete attributes so a user's own browser password manager can still offer a saved credential.

## Acceptance Criteria

- The login HTML contains no username or password value attribute.
- The existing configured credential loading remains unchanged.
- Front-end syntax check passes.

## Verification

- Login form source contains no `value` attribute on either credential input.
- `node --check frontend/public/main.js` completed successfully.
