# FAQ

Frequent questions about dazki. If your question is not here, file
an issue in the project repo.

## General

### Is dazki like Shizuku?

Yes. dazki borrows the same idea: run a privileged Java process
started from ADB or root, expose a binder, let approved apps call
through it. The code is independent of Shizuku and the API is
smaller. Shizuku is more mature; dazki is easier to read.

### Does dazki need root?

No. The server runs as shell uid (2000) when started from ADB. That
is enough for most system APIs. Root is needed only for things like
writing to read-only Settings.Secure keys or for some OEM-specific
hidden APIs.

### Does dazki work on iOS?

No. dazki is Android only.

### Can dazki bypass app sandboxing?

Partially. The server can call system APIs that the calling app
could not call directly. It cannot read another app's private files
unless those files are world readable, which is rare on modern
Android. dazki is a privilege escalator for system APIs, not a
sandbox escape.

## Security

### Is dazki safe to install?

The debug APK is signed with the standard Android debug key. Anyone
could have built it. If you do not trust the build, build it
yourself from source.

### Can a malicious app use dazki without my knowledge?

No. The manager app shows every app on the allow list. An app can
only call dazki if you approved it. The kill switch revokes every
app at once.

### Can a malicious AI use dazki?

Only if you paired it. Pairing requires typing a 6 digit code into
the manager app. The AI cannot pair itself.

### What happens if I lose my phone?

The dazki server only listens on localhost. The AI host has to be
on the same machine or tunnel through adb. A remote attacker cannot
reach the server without adb access.

### Where is the audit log?

`/data/local/tmp/dazki-audit.jsonl` on the device. Pull it with
`adb pull`. Every privileged call is logged.

## Compatibility

### Which Android versions are supported?

API 26 (Android 8.0) and newer. Older versions are not tested.

### Does dazki work on MIUI, One UI, ColorOS, etc.?

Mostly. Some OEM ROMs patch ServiceManager to reject shell uid. On
those ROMs the server cannot register its binder and the manager
shows "Failed to register binder" in the log. The fix is to start
the server as root.

### Does dazki work on Android 14+?

Yes, with the caveats listed in the manager's "Android version
notes" card. Several hidden APIs that worked on Android 13 no
longer work on 14. The server catches the SecurityException and
returns an error to the client.

### Does dazki work on tablets?

Yes. The UI is single pane on phones and tablets. There is no
two-pane layout yet.

## Building

### The build fails with "jlink does not exist"

You are running a JRE, not a JDK. Install a full JDK (OpenJDK 17 or
21) and try again.

### The build fails with "ServiceManager not found"

The hidden-api-stubs module is missing. Run `./gradlew
:hidden-api-stubs:assembleDebug` to build it first.

### The APK is rejected by Play Protect

See [play-protect-fix.md](play-protect-fix.md).

### Can I build dazki in Android Studio?

Yes. Open the `dazki/` folder in Android Studio Giraffe or newer.
The IDE will sync the Gradle project and you can run the manager
and sample modules from the Run menu.

## Development

### How do I add a new RPC method?

1. Add the method to `IDazkiService.aidl` in the api module.
2. Implement it in `DazkiService.kt` in the manager module.
3. Add a capability for it in `Capabilities.kt` in the permission-
   system module.
4. Add a handler in `RpcDispatcher.kt` in the networking module if
   the AI should be able to call it.
5. Add a CLI subcommand in `dazki.py` in the claude-skill module.

### How do I add a new AI connector?

Implement the `AiConnector` interface in the ai-connectors module.
Register it in `AiConnectorRegistry.kt`. The manager app picks it
up from the Settings screen.

### How do I write a plugin?

See `examples/hello-plugin/` in the repo. A plugin is a small
Kotlin object that implements `DazkiPlugin`. Drop the compiled DEX
file in `/data/local/tmp/dazki-plugins/` and restart the server.
