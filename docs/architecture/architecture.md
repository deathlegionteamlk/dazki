# Architecture

dazki has four runtime components and four source modules. This
document explains how they fit together.

## Runtime components

### 1. dazki server process

A Java process started by `app_process` with the manager APK as
its classpath. Runs as shell uid (2000) when started from ADB, or
root (0) when started via su. The server's main class is
`dev.deathlegion.dazki.server.DazkiServerMain`.

On startup the server:

1. Parses its command line arguments.
2. Creates a system Context through `ActivityThread.systemMain()`.
3. Loads the allow list from `/data/local/tmp/dazki-allowlist.txt`.
4. Prepares the main Looper.
5. Instantiates `DazkiService` and registers it as a binder in
   `ServiceManager` under the name `dazki_service`.
6. Calls `Looper.loop()` and blocks until shutdown.

The server never touches the network. It only exposes a binder.

### 2. dazki manager app

A normal Android app installed from an APK. The user opens it to
see the service status, start the service, stop it, and manage the
allow list.

The manager talks to the server through the binder (when the
server is running) and through a localhost TCP socket (when an AI
session is paired). The TCP socket is on port 7654 and is
localhost only.

The manager also writes the starter script that the user pushes
to the device with `adb push`.

### 3. dazki client apps

Any app that depends on the `:api` module. The client app calls
`Dazki.init()` in its Application.onCreate, then `Dazki
.isServiceRunning()`, `Dazki.requestPermission()`, and `Dazki
.getService()`.

The client gets the binder by calling
`ServiceManager.getService("dazki_service")` through reflection.
The reflection is needed because `ServiceManager` is hidden from
the SDK.

### 4. AI host

The machine running the AI assistant (Claude, ChatGPT, etc.). The
AI host runs the `dazki.py` CLI from the Claude skill. The CLI
tunnels through adb to the manager's TCP socket and sends JSON RPC
frames.

## Source modules

### :api

Android library. Contains:

- `IDazkiService.aidl` and `IDazkiManagerCallback.aidl`: the wire
  protocol.
- `Dazki.kt`: the public Kotlin entry point.
- `PermissionRequestExtra.kt`: intent extras for the permission
  request flow.
- `DazkiFlags.kt`: PackageManager flag constants.

Third party apps depend on this module. Keep it small.

### :hidden-api-stubs

Android library. Contains compile-time stubs for `ServiceManager`
and `SystemProperties`. At runtime the real platform classes are
used. The stubs exist only so the manager and server modules
compile against the SDK.

### :manager

Android app. Contains:

- `manager/`: UI code (MainActivity, PermissionRequestActivity,
  DazkiManagerApp, DazkiManagerRepository).
- `server/`: server code (DazkiServerMain, DazkiService,
  ServerArgs).
- `assets/dazki-starter.sh`: the starter script the manager fills
  in and the user runs via adb.

### :sample

Android app. Demonstrates the full developer flow.

## Auxiliary modules (not in the build yet)

### :networking

Plain Kotlin module with the RPC server, dispatcher, session
store, rate limiter, and audit log. The manager app will depend on
this module once the RPC server is wired in.

### :ai-connectors

Plain Kotlin module with the AI connector interfaces and the
Claude, OpenAI, Gemini, and local LLM implementations.

### :permission-system

Plain Kotlin module with Capability, Grant, GrantStore, and
PermissionDecider.

### :plugin-system

Plain Kotlin module with the DazkiPlugin interface and
PluginLoader.

These modules are not yet included in `settings.gradle.kts` and
therefore are not built. They are source only at this point.

## Wire protocol

### Client to server (binder)

The client calls methods on `IDazkiService`. Each method takes
plain Kotlin types and returns plain Kotlin types. The binder
marshals them.

The server checks the calling uid against the allow list before
every privileged call. If the check fails, the server throws
`SecurityException`.

### AI host to manager (TCP)

Length-prefixed JSON over a localhost TCP socket. Frame format:

```
0x00 | 4 bytes big-endian length | JSON payload
```

The JSON payload is:

```json
{
  "token": "...",
  "method": "packages.list",
  "args": { "flags": 0 },
  "seq": 1234567890
}
```

The reply is the same frame format with this JSON:

```json
{
  "ok": true,
  "seq": 1234567890,
  "result": { "packages": ["com.example", ...] }
}
```

Or on error:

```json
{
  "ok": false,
  "seq": 1234567890,
  "error": "scope denied: packages.read"
}
```

## Permission model

Three layers:

1. **App allow list.** Stored in SharedPreferences in the manager
   app. The server loads it on startup. Only apps on the list can
   call the binder at all.
2. **Capability grants.** Stored in the GrantStore. Each grant
   gives a principal (app or session) a capability (like
   `packages.read`). The server checks the grant before each call.
3. **AI session scopes.** Stored in the SessionStore. Each paired
   AI session has a set of scopes. The RPC dispatcher checks the
   scope before forwarding the call.

The kill switch clears all three layers at once.

## Audit log

Every privileged call lands in
`/data/local/tmp/dazki-audit.jsonl`. Each line is one JSON object:

```json
{
  "ts": "2026-07-12T11:23:45Z",
  "token": "a1b2c3d4...",
  "method": "packages.list",
  "args": { "flags": 8192 },
  "ok": true,
  "latency_ms": 12
}
```

The log is append only. The server rotates it when it reaches 10
MiB by keeping the last 50,000 lines.

## Threat model

- **Malicious app on the device.** Cannot call dazki unless the
  user approved it. Even if approved, can only call methods its
  capability grants allow.
- **Malicious AI host.** Cannot reach the device without adb
  access. Even with adb, must present a valid token. The token is
  revoked by the kill switch.
- **Local attacker with shell access.** Can read the audit log and
  the allow list. Can kill the server. Cannot escalate to root
  unless the device is rooted.
- **Remote attacker.** Cannot reach the server. The RPC socket is
  localhost only. The binder is not exposed.
