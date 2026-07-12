# dazki

[![Android](https://img.shields.io/badge/Android-8.0%2B-34A853?logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-26%2B-brightgreen)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)
[![Release](https://img.shields.io/badge/release-v1.0.0-orange)](https://github.com/deathlegionteamlk/dazki/releases)
[![Tutorial](https://img.shields.io/badge/tutorial-4m51s-FF0000?logo=youtube&logoColor=white)](https://github.com/deathlegionteamlk/dazki/releases/download/v1.0.0/dazki-tutorial.mp4)

> A bridge between AI assistants and rooted Android devices. Lets normal apps call system APIs through a privileged service started with ADB or root, instead of forking slow shell commands. Built by [death legion team](#team).

dazki is modeled on Shizuku. A small Java process runs as `app_process`, registers a binder in `ServiceManager`, and forwards approved calls to Android system services. Client apps depend on a tiny `:api` module and talk to the binder. No shell, no fork, no parsing of stdout.

The same APK ships the manager UI and the server. The manager shows service status, start instructions, the allow list, and a kill switch. The server runs as shell uid (2000) when started from ADB, or root (0) when started via su.

## Why dazki

Apps that need system APIs fork a shell and run `pm`, `am`, or `settings` commands. That is slow, leaks output to logcat, and gives the app a wider attack surface than it needs.

dazki gives those apps a typed RPC instead. The server holds the privileged uid. The app talks to a binder. The user approves each app, scopes each token, and reads every call in the audit log.

## Architecture

```
              app_process (shell uid 2000 or root)
              running DazkiServerMain
                        |
                        | registers binder
                        v
              ServiceManager["dazki_service"]
                        |
        +---------------+----------------+
        |                                |
   manager app                       client app
   (status UI,                       (uses :api
   allow list)                        to call)
```

Three layers. The `:api` module is what client apps depend on. The manager app shows status, the allow list, and start instructions. The server runs as `app_process` and forwards binder calls to PackageManager, ActivityManager, Settings, and friends.

## Modules

| module             | what it is                                                       |
|--------------------|------------------------------------------------------------------|
| `api`              | Android library: AIDL plus `Dazki` Kotlin object. Third-party apps depend on this. |
| `manager`          | Android app: status UI, permission UI, allow list. Also contains the server code. |
| `hidden-api-stubs` | Compile-time stubs for `android.os.ServiceManager`, `SystemProperties`. |
| `sample`           | Demo client app showing the full developer flow.                 |
| `ai-connectors`    | Claude, OpenAI, Gemini, and local LLM connectors.                |
| `networking`       | RPC server, session store, rate limiter, audit log.              |
| `permission-system`| Capability, Grant, GrantStore, PermissionDecider.               |
| `plugin-system`    | DazkiPlugin interface and PluginLoader.                          |
| `tests`            | Unit tests for permission, audit, rate limiter.                  |
| `tools`            | Shell scripts for building, installing, debugging.               |

## Quick start

Install the debug APKs from the [latest release](https://github.com/deathlegionteamlk/dazki/releases/latest):

```sh
adb install -r dazki-manager-debug.apk
adb install -r dazki-sample-debug.apk
```

Open the manager app. Tap Start. A bottom sheet shows two commands. Use the ADB option:

```sh
adb push dazki-starter.sh /data/local/tmp/
adb shell sh /data/local/tmp/dazki-starter.sh &
```

The server boots in under a second. The status card flips to "Service running".

Open the sample app. Tap Request permission. Approve in the manager. Back in the sample, tap Call listPackages. You get the full installed package list, including ones hidden from regular apps.

Watch the 4 minute 51 second tutorial video to see the whole flow:

```sh
curl -L -o dazki-tutorial.mp4 https://github.com/deathlegionteamlk/dazki/releases/download/v1.0.0/dazki-tutorial.mp4
```

## Use the API from another app

Add the `:api` module as a dependency, then:

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Dazki.init(this)
    }
}

// inside an Activity
if (!Dazki.isServiceRunning()) {
    // tell the user to start Dazki from the manager app
    return
}
if (!Dazki.isAllowed()) {
    Dazki.requestPermission(this)
    return
}
val service = Dazki.getService()
val packages = service.listPackages(DazkiFlags.MATCH_UNINSTALLED_PACKAGES)
```

The `requestPermission` call launches the manager app's `PermissionRequestActivity`. The user approves or denies. The manager updates its persisted allow list and tells the server.

## Claude skill (connect any AI to your device)

The `claude-skill/` folder contains a skill manifest that lets Claude (or any AI assistant that can run shell commands) talk to your device through dazki.

Install by copying `claude-skill/SKILL.md` and `claude-skill/dazki.py` into your Claude project. Then:

```sh
python3 dazki.py pair --device R3CR70XXXXX
# prints a 6 digit code. type it into the manager app.
python3 dazki.py packages list --device R3CR70XXXXX
python3 dazki.py settings get --device R3CR70XXXXX --namespace global --key screen_off_timeout
python3 dazki.py logcat --device R3CR70XXXXX --lines 200
```

Every AI call lands in `/data/local/tmp/dazki-audit.jsonl` on the device. The kill switch revokes every paired token at once.

## Permission limits when running from ADB

Shell uid (2000) holds a fixed set of permissions through the shell role. It can do most of what Shizuku does, but not everything:

- `WRITE_SECURE_SETTINGS` is granted to shell on Android 6+. Some `Settings.Global` keys are still read-only even for shell on Android 11+.
- `FORCE_STOP_PACKAGES` is granted to shell. `forceStopPackage` works for packages shell can see, which excludes apps that target the latest SDK and hide from shell on Android 10+.
- `MATCH_UNINSTALLED_PACKAGES` works for shell when listing packages.
- Hidden APIs marked `@SystemApi(client = MODULE_LIBRARIES)` are not callable from shell. Only `@SystemApi` and `@hide` APIs reachable via reflection are.
- Apps targeting Android 11+ can hide their data from shell through `android:forceUriPermissions` and visibleToApps filtering. dazki cannot read those files.
- On Android 14+, several hidden APIs were further restricted from shell. If a call throws `SecurityException`, the manager UI shows the message in the action output.

## Android version support

| SDK  | release | notes |
|------|---------|-------|
| 26   | 8.0     | baseline. Adaptive icons, ServiceManager.addService works for shell. |
| 28   | 9       | hidden API restrictions start. Reflection on `@hide` APIs is greylisted. |
| 29   | 10      | apps can hide data from shell via `forceUriPermissions`. |
| 30   | 11      | some `Settings.Global` keys become read-only for shell. |
| 31   | 12      | PackageManager hides packages from shell unless `MATCH_UNINSTALLED_PACKAGES` is passed. |
| 33   | 13      | notification runtime permissions; new photo picker. |
| 34   | 14      | shell loses access to some hidden APIs that used to work. |

## Build from source

Open the `dazki/` folder in Android Studio (Giraffe or newer) and press Run, or build from the command line:

```sh
cd dazki
./gradlew :manager:assembleDebug :sample:assembleDebug
```

The debug APKs land at:

- `manager/build/outputs/apk/debug/manager-debug.apk`
- `sample/build/outputs/apk/debug/sample-debug.apk`

You need JDK 21 (for `jlink`) and Android SDK 34. See `docs/sandbox-apk-guide.md` for the full walkthrough.

## Security

- The RPC server binds to `127.0.0.1` only. The AI host has to use `adb forward` to reach it.
- Every privileged call is recorded in `/data/local/tmp/dazki-audit.jsonl`.
- The default token scope is `packages.read` and `settings.read` only. The user upgrades scopes from the manager.
- The kill switch revokes every paired token at once.
- Tokens are never logged. Only the first 8 hex characters appear in the audit log.

See [`docs/security-policy.md`](docs/security-policy.md) for the full threat model and [`docs/security.md`](docs/security.md) for the disclosure process.

## Documentation

- [`docs/architecture.md`](docs/architecture.md) - how the pieces fit together
- [`docs/sandbox-apk-guide.md`](docs/sandbox-apk-guide.md) - step by step sandbox demo
- [`docs/play-protect-fix.md`](docs/play-protect-fix.md) - how to dismiss Play Protect warnings
- [`docs/permission-issues-fix.md`](docs/permission-issues-fix.md) - common permission errors and fixes
- [`docs/faq.md`](docs/faq.md) - frequent questions
- [`docs/changelog.md`](docs/changelog.md) - what changed in this release

## Tutorial

The release includes a 4 minute 51 second tutorial video (`dazki-tutorial.mp4`) with 10 segments:

1. intro (9 sec)
2. the problem dazki solves (34 sec)
3. the idea (25 sec)
4. three layer architecture (31 sec)
5. install and start (34 sec)
6. sample app flow (45 sec)
7. Claude skill pairing (29 sec)
8. security model (40 sec)
9. Play Protect fix (33 sec)
10. outro (12 sec)

Subtitles are in `tutorials/captions/captions.srt`. The narration script is in `tutorials/scripts/narration.md`.

## Roadmap

- [ ] Wire `networking`, `ai-connectors`, `permission-system`, `plugin-system` modules into `settings.gradle.kts`
- [ ] Add Macrobenchmark module for binder round trip latency
- [ ] Add v3.1 APK signature scheme for Android 14+
- [ ] Add QR code pairing for AI sessions
- [ ] Add multi-device support in the manager UI
- [ ] Add per-app call statistics dashboard
- [ ] Add Magisk module installer option
- [ ] Add ADB over WiFi pairing helper
- [ ] Add Quick Settings tile to start and stop the server
- [ ] Add widget showing service status

## Team

Built by **death legion team**.

## License

MIT. See [LICENSE](LICENSE).

## Star history

If dazki saved you from forking shell commands, star the repo. It helps other people find it.

Keywords: android, shizuku, adb, root, system apis, binder, service manager, privileged service, app_process, shell uid, claude skill, ai connector, audit log, security, kotlin, manager app, hidden api, reflection, settings.global, settings.secure, package manager, activity manager, force stop, logcat, sandbox, play protect.
