# Changelog

All notable changes to dazki are listed here. The format follows
Keep a Changelog (https://keepachangelog.com/en/1.1.0/).

## [1.0.0] - 2026-07-12

First release. Built by death legion team.

### Added

- dazki manager app with status, start, stop, and allow list UI.
- dazki server running as app_process, started from ADB or root.
- IDazkiService binder with listPackages, forceStopPackage,
  getSettingsSecureInt, putSettingsGlobalInt, shutdown,
  registerManager, requestPermission.
- Public Kotlin API in the api module: Dazki.init, isServiceRunning,
  isAllowed, getService, requestPermission.
- Sample app demonstrating the full developer flow.
- Claude skill (SKILL.md) for connecting an AI assistant to the
  device.
- TTS narration script and 10 audio segments for the tutorial.
- Play Protect fix documentation.
- Permission issues fix documentation.
- Android sandbox APK guide.
- FAQ, changelog, contribution guide, security disclosure policy.
- Hidden API stubs for ServiceManager and SystemProperties.
- Permission system module with Capability, Grant, GrantStore,
  PermissionDecider.
- Networking module with RpcServer, RpcDispatcher, SessionStore,
  RateLimiter, AuditLog, TokenValidator.
- AI connectors module with Claude, OpenAI, Gemini, and local LLM
  connectors.
- Plugin system module with DazkiPlugin interface and PluginLoader.
- 18 main folders in the project tree.

### Known limitations

- The server cannot register its binder on OEM ROMs that patch
  ServiceManager to reject shell uid. Workaround: start as root.
- The debug APK trips Play Protect. See
  docs/tutorials/play-protect-fix.md for how to dismiss the warning.
- The RPC server is plain TCP, not real WebSocket. The wire format
  is length-prefixed JSON. This is enough for the volumes an AI
  assistant produces.
- The audit log is capped at 10 MiB. Older entries roll off when
  the cap is reached.
- No unit tests yet. The test folder is empty.

### Security considerations

- The server binds the RPC socket to localhost only. The AI host
  must use adb forward to reach it.
- The kill switch revokes every paired token at once.
- The audit log records every privileged call.
- The default token scope is packages.read and settings.read only.
  The user must explicitly grant shell.exec and other dangerous
  scopes.
