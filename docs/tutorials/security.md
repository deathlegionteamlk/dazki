# Security disclosure policy

dazki gives approved apps and AI sessions access to system APIs
that normal apps cannot call. A security bug in dazki can leak that
access to attackers. This document explains how to report a
security bug and what to expect.

## Reporting a vulnerability

Email security@deathlegion.team with:

- A description of the bug.
- The steps to reproduce it.
- The impact (what an attacker can do).
- The dazki version and Android version you tested on.

Do not open a public GitHub issue for security bugs. We will open
one after the fix is released.

## What counts as a security bug

- Any way for an app or AI session to call dazki without being on
  the allow list.
- Any way for an app to escalate its scope (for example, a
  packages.read app getting packages.write).
- Any way to bypass the rate limiter.
- Any way to read or modify the audit log without root.
- Any way to crash the server remotely.
- Any way for the RPC socket to be reached from outside localhost.
- Any way for a paired AI session to keep working after the user
  pressed the kill switch.

## What does NOT count

- The server running as shell uid. That is by design.
- The manager app showing the allow list in plaintext. That is by
  design.
- The audit log being readable by root. That is by design.
- The debug APK being signed with the debug key. Use the release
  build in production.
- The RPC protocol being plain TCP without TLS. The socket is
  localhost only and tunneled through adb, which is already
  trusted.

## Response timeline

- We acknowledge receipt within 48 hours.
- We confirm or reject the report within 7 days.
- If confirmed, we fix the bug within 30 days for high severity
  and 90 days for medium severity.
- We publish a fixed release and credit the reporter in the
  changelog (unless they prefer to remain anonymous).

## Reward

We do not offer a monetary bounty. We will credit you in the
changelog and in the security advisories page.
