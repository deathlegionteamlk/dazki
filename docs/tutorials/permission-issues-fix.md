# Permission issues fix

dazki needs a handful of permissions to do its job. Some are runtime
permissions (the user must grant them at first run), some are install
time permissions, and some only work when the server is started as
shell uid through ADB. This document covers the most common
permission errors and how to fix them.

## Manager app permissions

The manager app's manifest declares these:

| permission                              | when needed                                   |
|-----------------------------------------|-----------------------------------------------|
| QUERY_ALL_PACKAGES                      | listing installed apps in the picker UI       |
| POST_NOTIFICATIONS (Android 13+)        | showing the "service died" notification       |
| FOREGROUND_SERVICE                      | running the status poller in the foreground   |
| RECEIVE_BOOT_COMPLETED                  | starting the manager on boot (optional)       |
| WAKE_LOCK                               | keeping the device awake during a long RPC    |

The manifest already declares QUERY_ALL_PACKAGES. The others should
be added when the corresponding feature is enabled.

### Runtime permissions

Android 13 added POST_NOTIFICATIONS as a runtime permission. The
manager asks for it the first time the user enables the "notify
when service dies" toggle. The user can deny it; in that case the
toggle does nothing and the manager logs a warning.

### QUERY_ALL_PACKAGES and Play Store

Google Play restricts which apps may declare QUERY_ALL_PACKAGES.
The dazki manager is not on the Play Store (it is sideloaded), so
the restriction does not apply. If you fork dazki and try to ship
to the Play Store, you will need to either drop the permission or
apply for an exemption.

## Server permissions (when started from ADB)

The server process runs as shell uid (2000). Shell uid holds a fixed
set of permissions through the shell role:

- FORCE_STOP_PACKAGES (force stop apps)
- DUMP (dumpsys, logcat)
- SET_DEBUG_APP (attach a debugger)
- SET_PROCESS_LIMIT (override the system process limit)
- WRITE_SECURE_SETTINGS (write to Settings.Secure, with exceptions)
- PACKAGE_USAGE_STATS (read UsageStats, requires user toggle)
- INSTALL_PACKAGES (only via pm, not via the binder API)

Shell uid does NOT hold:

- READ_SMS, READ_CONTACTS, READ_CALL_LOG (privacy scoped)
- ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
- RECORD_AUDIO, CAMERA
- BODY_SENSORS
- Most other hardware scoped permissions

If a client app tries to use dazki to read one of these, the server
returns SecurityException. The manager UI shows the error in the
audit log.

## Server permissions (when started as root)

The server runs as uid 0. It can do anything on the device. This is
why the manager app asks the user to confirm before starting the
server as root, and why the kill switch exists.

## Common errors

### "SecurityException: Permission Denial: forceStopPackage"

The server is running as shell uid and tried to stop a package that
shell cannot see. The package is probably a system app, or it
targets the latest SDK and hides from shell. Fix: stop the package
as root, or stop it through the system's Recent Apps screen.

### "SecurityException: WRITE_SECURE_SETTINGS"

The server tried to write to a Settings.Secure key that is read
only. Android 11 made several previously writable keys read only
even for shell. The list of read only keys varies by OEM. Fix: use
`settings put global` instead when possible, or accept that the
key cannot be changed.

### "SecurityException: not allowed to call addService"

The server tried to register its binder in ServiceManager and the
system refused. This happens on some OEM ROMs that patch
ServiceManager to reject shell uid. Fix: start the server as root,
or use a ROM that allows shell to call addService.

### "SecurityException: PACKAGE_USAGE_STATS requires user grant"

The client app called the UsageStats method. The user has not
toggled "Permit usage access" for dazki in Settings. Fix: open
Settings, Apps, Special access, Usage access, dazki, toggle on.

### "DeadObjectException" from the binder

The server crashed. Check the audit log for the last call before
the crash. The most common cause is calling a hidden API that the
current Android version has removed. Fix: update dazki to a build
that handles the missing API, or downgrade the Android version.

### "IllegalArgumentException: Service not registered"

The client app called `Dazki.getService()` but the server is not
running. Fix: open the dazki manager and press Start.

## Manager permission manifest

The full manifest after adding the optional permissions:

```xml
<manifest>
    <uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
</manifest>
```

`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` is needed so the user can
whitelist dazki from Doze. Without it, the manager's status poller
stops running after a few minutes of screen off time.

## Audit log

Every privileged call lands in `/data/local/tmp/dazki-audit.jsonl`
on the device. The audit entry includes the calling token, the
method, the arguments, the result, and the latency. When a call
fails, the error field explains why.

Pull the audit log with:

```sh
adb pull /data/local/tmp/dazki-audit.jsonl
```

Read it with any JSONL parser, or with `jq`:

```sh
jq '. | select(.ok == false)' dazki-audit.jsonl
```
