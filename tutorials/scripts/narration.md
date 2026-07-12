# dazki tutorial narration

This is the script read out loud by the TTS skill to produce the
audio track for the tutorial video. The script is broken into short
segments so each one maps to a single shot in the storyboard.

Total runtime target: about 4 minutes 30 seconds.

Voice: male, calm, slightly dry. No fake excitement.

---

## Segment 1 (cover, 0:00 to 0:12)

dazki. A bridge between AI assistants and rooted Android devices.
Built by death legion team.

## Segment 2 (problem, 0:12 to 0:35)

Apps that need to call system APIs usually fork a shell process and
run pm, am, or settings commands. That is slow, it leaks shell
output through logcat, and it gives the app a wider attack surface
than it needs.

## Segment 3 (idea, 0:35 to 1:00)

dazki runs a small Java process as shell uid, started from ADB or
root. The process registers a binder in ServiceManager. Approved
apps connect to the binder and call typed methods. No shell, no
fork, no parsing of stdout.

## Segment 4 (architecture, 1:00 to 1:35)

Three layers. The api module is what client apps depend on. The
manager app shows status, the allow list, and start instructions.
The server runs as app_process and forwards binder calls to
PackageManager, ActivityManager, Settings, and friends.

## Segment 5 (install, 1:35 to 2:05)

Install the debug APK with adb install. Open the manager. Press
Start. A bottom sheet shows two commands. Push the starter script
with adb push, then run it with adb shell. The server boots in
under a second and registers its binder.

## Segment 6 (sample app, 2:05 to 2:35)

The sample app calls Dazki.init, then Dazki.isServiceRunning, then
Dazki.requestPermission. The manager app opens a permission dialog.
Press Approve. The sample is now allowed. Press Call
listPackages and watch it return the full installed package list,
including ones hidden from regular apps.

## Segment 7 (Claude skill, 2:35 to 3:05)

Install the dazki Claude skill. Run dazki pair. A six digit code
appears. Type it into the manager app under Pair new session. Now
the AI can call packages list, force stop, settings get, logcat,
and shell exec, all through the typed RPC layer.

## Segment 8 (security, 3:05 to 3:45)

Every call lands in the audit log at data local tmp dazki audit
dot jsonl. The user can scope each token. The kill switch revokes
every token at once. The server binds its RPC socket to localhost
only, so the AI host has to use adb forward to reach it.

## Segment 9 (Play Protect, 3:45 to 4:10)

Play Protect may flag the debug APK because it is signed with a
debug key and requests QUERY_ALL_PACKAGES. To dismiss the warning,
open Play Protect, tap the warning, and tap Install anyway. For
production builds, sign with a real key and tighten the manifest.

## Segment 10 (outro, 4:10 to 4:30)

dazki. Source code, skill, and tutorial in the project folder.
Built by death legion team.
