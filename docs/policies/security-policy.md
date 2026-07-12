# Security policy

dazki gives approved apps and AI sessions access to Android system
APIs that normal apps cannot call. This document explains the
threat model and the rules the codebase follows to keep that
access safe.

## Threat model

| threat                              | mitigation                                        |
|-------------------------------------|---------------------------------------------------|
| Malicious app calls dazki           | Allow list. User must approve each app.           |
| Malicious app escalates scope       | Capability grants. Each grant names one capability.|
| Malicious AI session                | Pairing flow with 6 digit code. Token scopes.     |
| Replay attack on RPC frames         | Sequence numbers. Out of order frames rejected.   |
| Brute force on pairing code         | Code expires after 60 seconds. 3 wrong attempts lock out. |
| Audit log tampering                 | Append only. Owner pulls and archives separately. |
| RPC socket reached from outside     | Socket binds to 127.0.0.1 only. adb forward required. |
| Server runs as root by accident     | Manager asks the user to confirm root start.      |
| Token leak via logcat               | Token is never logged. Only first 8 hex chars in audit. |

## Rules the codebase follows

1. **No token in logs.** The server logs only the first 8 hex
   characters of the token. The full token appears only in the
   in-memory session object and in the persisted session store
   (which lives in SharedPreferences, not in a log file).

2. **Pairing code expires.** The 6 digit code is valid for 60
   seconds. After 3 wrong attempts, the code is invalidated and
   the user must restart the pairing flow.

3. **Rate limit per token.** Default 60 calls per minute. The
   manager can raise the limit per session. The limit is enforced
   before the dispatcher runs the handler.

4. **Scope check per call.** Every handler declares the scopes it
   requires. The dispatcher checks the session has at least one
   of them before running the handler.

5. **Audit every call.** Every privileged call lands in the audit
   log with the token, method, arguments, result, and latency.
   Failed calls are audited too.

6. **Kill switch.** The manager has a red Kill switch button that
   revokes every paired token at once. The button is always
   visible at the top of the manager UI.

7. **Localhost only.** The RPC server binds to 127.0.0.1. The AI
   host has to use `adb forward tcp:7654 tcp:7654` to reach it.
   No remote attacker can connect.

8. **No shell.exec by default.** The shell.exec scope is the most
   dangerous. It is never granted by default. The user must
   explicitly toggle it on per session.

9. **File path validation.** The files.read and files.write
   handlers reject any path containing `..` and any path outside
   `/data/local/tmp`. The check is in the handler, not in the
   caller.

10. **Server uid check on shutdown.** Only the uid that called
    `registerManager` can call `shutdown`. This prevents a client
    app from killing the server.

## Reporting a vulnerability

See [security.md](../tutorials/security.md) for the disclosure
process.
