# Changelog

All notable changes to dazki are listed here. The format follows Keep a Changelog at https://keepachangelog.com/en/1.1.0/.

## [1.0.0] - 2026-07-12

First release. Built by death legion team.

### Added

- dazki manager app with status, start, stop, and allow list UI.
- dazki server running as app_process, started from ADB or root.
- IDazkiService binder with listPackages, forceStopPackage, getSettingsSecureInt, putSettingsGlobalInt, shutdown, registerManager, requestPermission.
- Public Kotlin API in the api module: Dazki.init, isServiceRunning, isAllowed, getService, requestPermission.
- Sample app demonstrating the full developer flow.
- Claude skill (SKILL.md) for connecting an AI assistant to the device.
- TTS narration script and 10 audio segments for the tutorial.
- Full tutorial video (4 min 51 sec, 1080x1920 MP4).
- Play Protect fix documentation.
- Permission issues fix documentation.
- Android sandbox APK guide.
- FAQ, changelog, contribution guide, security disclosure policy.
- Hidden API stubs for ServiceManager and SystemProperties.
- Permission system module with Capability, Grant, GrantStore, PermissionDecider.
- Networking module with RpcServer, RpcDispatcher, SessionStore, RateLimiter, AuditLog, TokenValidator.
- AI connectors module with Claude, OpenAI, Gemini, and local LLM connectors.
- Plugin system module with DazkiPlugin interface and PluginLoader.
- Dev tools scripts for build, install, start, stop, audit pull.

### Known limitations

- The server cannot register its binder on OEM ROMs that patch ServiceManager to reject shell uid. Workaround: start as root.
- The debug APK trips Play Protect. See docs/play-protect-fix.md for how to dismiss the warning.
- The RPC server is plain TCP, not real WebSocket. The wire format is length-prefixed JSON. This is enough for the volumes an AI assistant produces.
- The audit log is capped at 10 MiB. Older entries roll off when the cap is reached.
- The networking, ai-connectors, permission-system, and plugin-system modules are not yet wired into settings.gradle.kts. They are source only.

### Security considerations

- The server binds the RPC socket to localhost only. The AI host must use adb forward to reach it.
- The kill switch revokes every paired token at once.
- The audit log records every privileged call.
- The default token scope is packages.read and settings.read only. The user must explicitly grant shell.exec and other dangerous scopes.

## [Unreleased]

### Planned

- Wire auxiliary modules into settings.gradle.kts so they ship in the manager APK.
- Add Macrobenchmark module for binder round trip latency.
- Add v3.1 APK signature scheme for Android 14+.
- Add QR code pairing for AI sessions.
- Add multi-device support in the manager UI.
- Add per-app call statistics dashboard.
- Add Magisk module installer option.
- Add ADB over WiFi pairing helper.
- Add Quick Settings tile to start and stop the server.
- Add widget showing service status.
