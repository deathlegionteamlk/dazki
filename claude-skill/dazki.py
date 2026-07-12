#!/usr/bin/env python3
"""
dazki CLI.

A small command line tool that the Claude skill calls to talk to a
paired Android device through the dazki service. The tool opens a
WebSocket to localhost:7654 (tunneled through adb) and sends JSON
RPC frames. Every call shows up in the device audit log.

Usage:
    dazki pair --device <serial>
    dazki ping --device <serial>
    dazki packages list --device <serial> [--flags N]
    dazki packages force-stop --device <serial> --pkg <name>
    dazki settings get --device <serial> --namespace <global|secure|system> --key <k>
    dazki settings put --device <serial> --namespace <...> --key <k> --value <v>
    dazki logcat --device <serial> [--pid N] [--lines N]
    dazki shell --device <serial> --cmd "<shell command>"
    dazki files pull --device <serial> --remote <path> --local <path>
    dazki files push --device <serial> --local <path> --remote <path>
    dazki audit --device <serial> [--since Nh]
    dazki unpair --device <serial>

The tool is single-file on purpose so the skill can vendor it.
"""

import argparse
import hashlib
import json
import os
import secrets
import socket
import subprocess
import sys
import time
from pathlib import Path

DEFAULT_PORT = 7654
CONFIG_DIR = Path.home() / ".dazki"
CONFIG_FILE = CONFIG_DIR / "tokens.json"


def adb(*args: str, check: bool = True) -> str:
    """Run an adb command and return stdout."""
    cmd = ["adb", *args]
    result = subprocess.run(cmd, capture_output=True, text=True)
    if check and result.returncode != 0:
        sys.stderr.write(f"adb failed: {result.stderr.strip()}\n")
        sys.exit(2)
    return result.stdout.strip()


def ensure_forward(serial: str, port: int = DEFAULT_PORT) -> None:
    """Set up adb port forwarding to the device's WebSocket server."""
    adb("-s", serial, "forward", f"tcp:{port}", f"tcp:{port}", check=False)


def load_tokens() -> dict:
    if not CONFIG_FILE.exists():
        return {}
    return json.loads(CONFIG_FILE.read_text())


def save_tokens(tokens: dict) -> None:
    CONFIG_DIR.mkdir(parents=True, exist_ok=True)
    CONFIG_FILE.write_text(json.dumps(tokens, indent=2))


def call_rpc(serial: str, method: str, args: dict, expect_reply: bool = True) -> dict | None:
    """Send one JSON RPC frame and read the response."""
    tokens = load_tokens()
    token = tokens.get(serial)
    if not token:
        sys.stderr.write(f"no token for {serial}. run: dazki pair --device {serial}\n")
        sys.exit(3)

    ensure_forward(serial)
    payload = json.dumps({
        "token": token,
        "method": method,
        "args": args,
        "seq": int(time.time() * 1000),
    }).encode("utf-8")

    frame = b"\x00" + len(payload).to_bytes(4, "big") + payload

    sock = socket.create_connection(("127.0.0.1", DEFAULT_PORT), timeout=30)
    try:
        sock.sendall(frame)
        if not expect_reply:
            return None
        header = recv_exact(sock, 5)
        if not header:
            return {"ok": False, "error": "no reply"}
        length = int.from_bytes(header[1:5], "big")
        body = recv_exact(sock, length)
        return json.loads(body.decode("utf-8"))
    finally:
        sock.close()


def recv_exact(sock: socket.socket, n: int) -> bytes:
    buf = bytearray()
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            break
        buf.extend(chunk)
    return bytes(buf)


def cmd_pair(args: argparse.Namespace) -> None:
    token = secrets.token_hex(32)
    code = "".join(str(int(c, 16)) for c in token[:6])[:6]
    tokens = load_tokens()
    tokens[args.device] = token
    save_tokens(tokens)
    # Send a pair request so the device knows a session is trying to
    # pair. The user types the code in the manager app to confirm.
    call_rpc(args.device, "session.pair", {"code": code}, expect_reply=True)
    print(f"token:   {token}")
    print(f"code:    {code}")
    print(f"device:  {args.device}")
    print("Open dazki manager on the phone, tap Pair new session, type the code.")


def cmd_ping(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "session.ping", {})
    if reply and reply.get("ok"):
        print(json.dumps(reply.get("result", {}), indent=2))
    else:
        sys.stderr.write(f"ping failed: {reply}\n")
        sys.exit(1)


def cmd_packages_list(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "packages.list", {"flags": args.flags})
    if reply and reply.get("ok"):
        for p in reply.get("result", {}).get("packages", []):
            print(p)
    else:
        sys.stderr.write(f"list failed: {reply}\n")
        sys.exit(1)


def cmd_packages_force_stop(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "packages.forceStop", {"pkg": args.pkg})
    print(json.dumps(reply, indent=2))


def cmd_settings_get(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "settings.get", {
        "namespace": args.namespace,
        "key": args.key,
    })
    if reply and reply.get("ok"):
        print(reply.get("result", {}).get("value", ""))
    else:
        sys.stderr.write(f"get failed: {reply}\n")
        sys.exit(1)


def cmd_settings_put(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "settings.put", {
        "namespace": args.namespace,
        "key": args.key,
        "value": args.value,
    })
    print(json.dumps(reply, indent=2))


def cmd_logcat(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "logcat.dump", {
        "pid": args.pid,
        "lines": args.lines,
    })
    if reply and reply.get("ok"):
        print(reply.get("result", {}).get("text", ""))
    else:
        sys.stderr.write(f"logcat failed: {reply}\n")
        sys.exit(1)


def cmd_shell(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "shell.exec", {"cmd": args.cmd})
    if reply and reply.get("ok"):
        print(reply.get("result", {}).get("stdout", ""))
        if reply.get("result", {}).get("stderr"):
            sys.stderr.write(reply["result"]["stderr"])
    else:
        sys.stderr.write(f"shell failed: {reply}\n")
        sys.exit(1)


def cmd_files_pull(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "files.read", {"path": args.remote})
    if not reply or not reply.get("ok"):
        sys.stderr.write(f"pull failed: {reply}\n")
        sys.exit(1)
    import base64
    data = base64.b64decode(reply["result"].get("data", ""))
    Path(args.local).write_bytes(data)
    print(f"wrote {len(data)} bytes to {args.local}")


def cmd_files_push(args: argparse.Namespace) -> None:
    import base64
    data = Path(args.local).read_bytes()
    reply = call_rpc(args.device, "files.write", {
        "path": args.remote,
        "data": base64.b64encode(data).decode("ascii"),
    })
    print(json.dumps(reply, indent=2))


def cmd_audit(args: argparse.Namespace) -> None:
    reply = call_rpc(args.device, "audit.dump", {"since_hours": args.since})
    if reply and reply.get("ok"):
        for line in reply.get("result", {}).get("entries", []):
            print(json.dumps(line))
    else:
        sys.stderr.write(f"audit failed: {reply}\n")
        sys.exit(1)


def cmd_unpair(args: argparse.Namespace) -> None:
    call_rpc(args.device, "session.unpair", {}, expect_reply=False)
    tokens = load_tokens()
    tokens.pop(args.device, None)
    save_tokens(tokens)
    print(f"unpaired {args.device}")


def build_parser() -> argparse.ArgumentParser:
    p = argparse.ArgumentParser(prog="dazki", description="dazki device bridge CLI")
    sub = p.add_subparsers(dest="cmd", required=True)

    sp = sub.add_parser("pair", help="pair a new device")
    sp.add_argument("--device", required=True)
    sp.set_defaults(func=cmd_pair)

    sp = sub.add_parser("ping", help="ping a paired device")
    sp.add_argument("--device", required=True)
    sp.set_defaults(func=cmd_ping)

    sp = sub.add_parser("packages", help="package manager operations")
    sps = sp.add_subparsers(dest="sub", required=True)
    sl = sps.add_parser("list")
    sl.add_argument("--device", required=True)
    sl.add_argument("--flags", type=lambda x: int(x, 0), default=0)
    sl.set_defaults(func=cmd_packages_list)
    sf = sps.add_parser("force-stop")
    sf.add_argument("--device", required=True)
    sf.add_argument("--pkg", required=True)
    sf.set_defaults(func=cmd_packages_force_stop)

    sp = sub.add_parser("settings", help="read or write Android settings")
    sps = sp.add_subparsers(dest="sub", required=True)
    sg = sps.add_parser("get")
    sg.add_argument("--device", required=True)
    sg.add_argument("--namespace", required=True, choices=["global", "secure", "system"])
    sg.add_argument("--key", required=True)
    sg.set_defaults(func=cmd_settings_get)
    spu = sps.add_parser("put")
    spu.add_argument("--device", required=True)
    spu.add_argument("--namespace", required=True, choices=["global", "secure", "system"])
    spu.add_argument("--key", required=True)
    spu.add_argument("--value", required=True)
    spu.set_defaults(func=cmd_settings_put)

    sp = sub.add_parser("logcat", help="dump logcat")
    sp.add_argument("--device", required=True)
    sp.add_argument("--pid", type=int, default=0)
    sp.add_argument("--lines", type=int, default=200)
    sp.set_defaults(func=cmd_logcat)

    sp = sub.add_parser("shell", help="run a shell command on the device")
    sp.add_argument("--device", required=True)
    sp.add_argument("--cmd", required=True)
    sp.set_defaults(func=cmd_shell)

    sp = sub.add_parser("files", help="push or pull files in /data/local/tmp")
    sps = sp.add_subparsers(dest="sub", required=True)
    spl = sps.add_parser("pull")
    spl.add_argument("--device", required=True)
    spl.add_argument("--remote", required=True)
    spl.add_argument("--local", required=True)
    spl.set_defaults(func=cmd_files_pull)
    spp = sps.add_parser("push")
    spp.add_argument("--device", required=True)
    spp.add_argument("--local", required=True)
    spp.add_argument("--remote", required=True)
    spp.set_defaults(func=cmd_files_push)

    sp = sub.add_parser("audit", help="dump the audit log")
    sp.add_argument("--device", required=True)
    sp.add_argument("--since", type=int, default=24, help="hours back, default 24")
    sp.set_defaults(func=cmd_audit)

    sp = sub.add_parser("unpair", help="unpair a device")
    sp.add_argument("--device", required=True)
    sp.set_defaults(func=cmd_unpair)

    return p


def main() -> None:
    args = build_parser().parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
