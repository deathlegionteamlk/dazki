# Demo Android sandbox APK guide

This guide walks through installing and using the dazki debug APK
in an Android sandbox (an emulator or a rooted physical device). It
covers the full flow from install to first API call.

## What you need

- An Android device or emulator running Android 8.0 (API 26) or
  newer. Android 11+ is recommended because the demo relies on
  hidden APIs that work better on newer versions.
- The dazki manager debug APK: `manager-debug.apk`.
- The dazki sample debug APK: `sample-debug.apk`.
- A PC with `adb` installed.
- Optionally, a rooted device. The demo works without root, but
  some calls (like writing to certain Settings.Global keys) only
  work as root.

## Step 1: Start the emulator

If you do not have a real device, start an Android Virtual Device
(AVD) from Android Studio. Pick a Pixel device running Android 14
(API 34). The emulator is rooted by default, which makes the demo
easier.

Wait for the emulator to fully boot. You should see the home screen
before continuing.

## Step 2: Install the APKs

```sh
adb install -r manager-debug.apk
adb install -r sample-debug.apk
```

If Play Protect blocks the install, see
[play-protect-fix.md](play-protect-fix.md) for how to dismiss the
warning.

Verify the install:

```sh
adb shell pm list packages | grep dazki
```

You should see:

```
package:dev.deathlegion.dazki.manager.debug
package:dev.deathlegion.dazki.sample
```

## Step 3: Open the manager app

Launch the dazki manager from the launcher. The icon is a blue
shield with a "D" inside.

The main screen shows:

- A status card at the top: "Service stopped".
- Three buttons: Start, Stop, Refresh.
- An Android version notes card.
- An empty "Allowed apps" list.

## Step 4: Start the service

Tap Start. A bottom sheet opens with two options: ADB and root.

For the sandbox demo, use ADB. Copy the two commands from the
bottom sheet into a terminal on your PC:

```sh
adb push dazki-starter.sh /data/local/tmp/
adb shell sh /data/local/tmp/dazki-starter.sh &
```

The `&` at the end runs the server in the background. The terminal
will print log lines from the server. Do not close the terminal
yet.

The manager's status card should flip to "Service running" within
a second. The "Server uid" line should read `2000 (shell)`.

## Step 5: Use the sample app

Launch the dazki sample app from the launcher.

The sample shows four cards:

- Service running: false (tap Refresh)
- This app allowed: false
- Platform: Android 14 (sdk 34), abi=x86_64, device=Pixel 6
- (output area)

Tap Refresh. The first two cards should now read true for the
service and false for the app.

Tap "Request permission". The manager app opens with a permission
dialog showing the sample's package name and uid. Tap Approve.

Switch back to the sample app. Tap Refresh again. The "This app
allowed" card should now read true.

Tap "Call listPackages(MATCH_UNINSTALLED_PACKAGES)". The output
area should show:

```
listPackages returned 152 entries
first 5: com.android.settings, com.android.systemui, ...
```

(The exact number and names depend on the device.)

## Step 6: Stop the service

In the manager app, tap Stop. The status card flips back to
"Service stopped". In the terminal where you ran the starter
script, you should see the server's exit log line.

If the manager's Stop button does not work (for example because
the server died on its own), kill the server directly:

```sh
adb shell pkill -f DazkiServerMain
```

## Step 7: Inspect the audit log

Every call the sample app made landed in the audit log. Pull it:

```sh
adb pull /data/local/tmp/dazki-audit.jsonl
```

Open it with a text editor or with `jq`:

```sh
jq . dazki-audit.jsonl
```

You should see one entry per call, with the token (truncated to
the first 8 characters), the method, the arguments, the result,
and the latency in milliseconds.

## Troubleshooting

### "Service not running" after Step 4

The starter script failed. Check the terminal where you ran it.
The most common cause is the manager APK path being wrong. The
manager fills in the path automatically, but if you moved the APK
after installing, the path may be stale. Reinstall the APK and
press Start again.

### "scope denied" in the sample app

The sample app called a method that its token does not allow. The
default token only has `packages.read`. Tap "Request permission"
in the sample app to upgrade.

### "Binder call failed" in the sample app

The server crashed. Look at the audit log for the last successful
call. The crash probably happened in the next call. Common causes:
the hidden API was removed on this Android version, or the
package name passed in does not exist.

### The terminal shows "app_process: command not found"

You are running the starter script on a device that does not have
app_process in PATH. This happens on some custom ROMs. Fix: change
the starter script to use the full path `/system/bin/app_process`.

## Next steps

After the demo works on the sandbox, try:

- Writing a real client app that uses the dazki API to read a
  setting that normal apps cannot read.
- Pairing the dazki Claude skill with the sandbox device and
  asking Claude to list the installed packages through natural
  language.
- Adding a new RPC method to the server (see
  `docs/architecture/adding-rpc-methods.md`).

## Reference

The dazki source code is in the `dazki/` directory of the project
repo. See `README.md` at the repo root for the overall
architecture and module layout.
