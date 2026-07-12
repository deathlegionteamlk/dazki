#!/bin/sh
# Start the dazki server on the device.
# Usage: ./tools/start-server.sh [--root]
set -e
ROOT=0
if [ "$1" = "--root" ]; then
    ROOT=1
fi
# Locate the manager APK on the device.
APK=$(adb shell pm path dev.deathlegion.dazki.manager.debug | head -1 | sed 's/package://')
if [ -z "$APK" ]; then
    echo "Manager APK not found. Install it first."
    exit 1
fi
DATA_DIR=$(adb shell run-as dev.deathlegion.dazki.manager.debug sh -c 'echo $HOME' 2>/dev/null || echo "/data/data/dev.deathlegion.dazki.manager.debug")
ALLOW_LIST="/data/local/tmp/dazki-allowlist.txt"
# Build the starter script locally and push it.
cat > /tmp/dazki-starter.sh <<EOF
#!/system/bin/sh
exec app_process -Djava.class.path=$APK /system/bin dev.deathlegion.dazki.server.DazkiServerMain \\
    --apk=$APK \\
    --data-dir=$DATA_DIR \\
    --allow-list=$ALLOW_LIST \\
    --debug
EOF
adb push /tmp/dazki-starter.sh /data/local/tmp/dazki-starter.sh
if [ "$ROOT" = "1" ]; then
    adb shell "su -c 'sh /data/local/tmp/dazki-starter.sh' &"
else
    adb shell "sh /data/local/tmp/dazki-starter.sh &"
fi
echo "Server started. Check the manager app for status."
