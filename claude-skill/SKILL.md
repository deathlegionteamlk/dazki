---
name: dazki-device-bridge
version: 1.0.0
description: |
  Connect Claude (or any AI assistant that can run shell commands) to a
  rooted Android device through the dazki privileged service. Lets the
  AI list installed apps, force stop packages, read and write settings,
  inspect logcat, run shell snippets as root, and pull or push files
  from /data/local/tmp, without giving the AI a raw adb shell. Every
  call goes through the dazki binder and is recorded in the audit log.
license: MIT
compatibility: any-agent
allowed-tools:
  - Bash
  - Read
  - Write
  - Edit
  - Grep
  - Glob
  - WebFetch
---

# dazki device bridge

A skill that lets an AI assistant talk to a rooted Android device
through the dazki privileged service. The AI never gets a raw adb
shell. Instead it calls typed RPC methods on the dazki binder, and
every call lands in an audit log the device owner can review.

## When to use this skill

Use this skill when the user asks the AI to do one of these on their
phone:

- list installed apps, including ones hidden from regular apps
- force stop a misbehaving app
- read or write Settings.Global, Settings.Secure, Settings.System
- dump logcat for a specific package or pid
- run a shell command as root (rooted devices only)
- copy a file between the host and /data/local/tmp on the device
- show the current dazki server status and allow list
- pair a new device to the AI session

Do not use this skill for anything that does not touch a real
Android device. The skill refuses to run if the dazki service is not
reachable.

## Prerequisites on the device

1. dazki manager app installed (debug build at first).
2. dazki service started from ADB or root using the starter script
   that the manager app generates.
3. The AI's session token added to the allow list through the manager
   app. The token is a 32 byte hex string that the AI generates once
   and the user pastes into the manager.

## Prerequisites on the AI side

The AI must have a working adb binary on PATH and the device must
show up in `adb devices`. The skill shells out to adb only for
device discovery and for the WebSocket tunnel that carries the RPC
traffic. The AI never runs raw adb shell commands.

## Pairing flow

The pairing flow exchanges a token between the AI session and the
device:

1. The AI calls `dazki pair --device <serial>`. It prints a 32 byte
   hex token and a 6 digit pairing code.
2. The user opens the dazki manager app on the phone, taps Pair new
   session, and types the 6 digit code.
3. The manager app writes the token to its allow list and tells the
   server.
4. The AI confirms the pairing by calling `dazki ping --device
   <serial>`. The server returns the device model and Android
   version.

Once paired, the AI can call any RPC method. The audit log records
the calling token, the method, the arguments, and the timestamp.

## Capability scoping

The user can scope what a token is allowed to do. The scopes are:

- `packages.read`    list packages and read package info
- `packages.write`   force stop, enable, disable components
- `settings.read`    read Settings.Global, Secure, System
- `settings.write`   write Settings.Global, Secure, System
- `shell.exec`       run shell commands as the server uid
- `logcat.read`      dump logcat
- `files.read`       read files under /data/local/tmp
- `files.write`      write files under /data/local/tmp

By default a freshly paired token gets `packages.read` and
`settings.read` only. The user upgrades scopes from the manager app.

## Kill switch

The manager app has a red Kill switch button that revokes every
paired token at once. The AI session stops working immediately.
The user re-pairs when they want to give access back.

## Example commands

```
dazki pair --device R3CR70XXXXX
dazki ping --device R3CR70XXXXX
dazki packages list --device R3CR70XXXXX --flags 0x2000
dazki packages force-stop --device R3CR70XXXXX --pkg com.example.app
dazki settings get --device R3CR70XXXXX --namespace global --key screen_off_timeout
dazki settings put --device R3CR70XXXXX --namespace global --key screen_off_timeout --value 30000
dazki logcat --device R3CR70XXXXX --pid 12345 --lines 200
dazki shell --device R3CR70XXXXX --cmd "pm list packages -3"
dazki files pull --device R3CR70XXXXX --remote /data/local/tmp/x.txt --local ./x.txt
dazki files push --device R3CR70XXXXX --local ./y.txt --remote /data/local/tmp/y.txt
dazki audit --device R3CR70XXXXX --since 1h
dazki unpair --device R3CR70XXXXX
```

## How it talks to the device

The skill opens a WebSocket from the AI host to a small HTTP server
that the dazki manager app exposes on localhost only (port 7654).
The WebSocket frames carry length-prefixed JSON RPC messages. Each
message includes the token, the method, the arguments, and a
monotonic sequence number. The server verifies the token, checks
the scope, runs the call, and writes the audit entry.

The WebSocket is tunneled through adb so the AI host never needs
network access to the phone. The skill runs `adb forward tcp:7654
tcp:7654` once before the first call.

## Audit log

Every call lands in `/data/local/tmp/dazki-audit.jsonl` on the
device. Each line is one JSON object:

```json
{"ts":"2026-07-12T11:23:45Z","token":"a1b2...","method":"packages.list","args":{"flags":8192},"ok":true,"latency_ms":12}
```

The user can pull the audit log with `dazki audit --device ...` and
read it in any text editor. Lines are append only. The server never
overwrites them.

## Limits

- Shell commands run as the server uid (shell 2000 or root 0), not
  as the AI's choice of uid. The user picks at server start time.
- Files outside /data/local/tmp are off limits even with
  `files.read`. The server rejects any path containing `..`.
- Rate limit is 60 calls per minute per token by default. The user
  can raise it from the manager app.
- WebSocket frames are capped at 1 MiB. Larger payloads must be
  chunked or moved through the files endpoints.

## Troubleshooting

- `device not found`: run `adb devices` and make sure the serial is
  listed.
- `service not running`: open the dazki manager app and press Start.
- `token rejected`: re-run `dazki pair`. The token may have been
  revoked.
- `scope denied`: ask the user to upgrade the token scope from the
  manager app.
- `port forward failed`: kill stale adb sessions with `adb kill-
  server` and retry.

## Reference

This skill is part of the dazki project by death legion team. The
full source lives under the `dazki/` directory of the project repo.
See `docs/architecture/` for the wire format and `docs/policies/`
for the security model.
