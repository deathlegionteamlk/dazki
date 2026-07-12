#!/system/bin/sh
#
# Dazki starter script.
#
# Written by the manager app to /data/local/tmp/dazki-starter.sh and
# then run via `adb shell sh /data/local/tmp/dazki-starter.sh` or
# `su -c sh /data/local/tmp/dazki-starter.sh`.
#
# The script invokes app_process with the manager APK as classpath so
# the JVM can load DazkiServerMain. The server runs as the same uid
# as the caller of this script: shell (2000) when started via ADB, or
# root (0) when started via su.
#
# Replace the placeholders below before running:
#   __APK_PATH__     full path to the installed manager APK
#   __DATA_DIR__     manager app data directory, e.g. /data/data/dev.deathlegion.dazki.manager.debug
#   __ALLOW_LIST__   path to the allow list file (server reads + writes)
#
# After editing, the manager runs:
#   sh /data/local/tmp/dazki-starter.sh

APK_PATH="__APK_PATH__"
DATA_DIR="__DATA_DIR__"
ALLOW_LIST="__ALLOW_LIST__"

if [ -z "$APK_PATH" ]; then
    echo "Starter script was not configured. Open the Dazki manager app and press Start."
    exit 1
fi

# app_process needs the APK on its classpath to find DazkiServerMain.
# -Djava.class.path sets the bootstrap classpath for app_process.
# The second arg "/system/bin" is the working directory (required by
# app_process even though we do not use it).
exec app_process -Djava.class.path="$APK_PATH" /system/bin dev.deathlegion.dazki.server.DazkiServerMain \
    --apk="$APK_PATH" \
    --data-dir="$DATA_DIR" \
    --allow-list="$ALLOW_LIST" \
    --debug
