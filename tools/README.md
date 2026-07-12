# dazki dev tools

A small collection of scripts for working on dazki. Each script is
self contained. Run `./<script> --help` for usage.

## scripts

| script              | what it does                                          |
|---------------------|-------------------------------------------------------|
| build-apks.sh       | build the manager and sample debug APKs               |
| install-apks.sh     | install both APKs on a connected device               |
| start-server.sh     | push the starter script and start the server          |
| stop-server.sh      | kill the server process                               |
| pull-audit.sh       | pull the audit log from the device                    |
| clear-audit.sh      | delete the audit log on the device                    |
| run-sample-test.sh  | install sample, request permission, run a test call   |
| lint-all.sh         | run Android lint on every module                      |

## build-apks.sh

```sh
./tools/build-apks.sh
```

Outputs:

- `manager/build/outputs/apk/debug/manager-debug.apk`
- `sample/build/outputs/apk/debug/sample-debug.apk`

## install-apks.sh

```sh
./tools/install-apks.sh [serial]
```

`serial` is optional. If you have multiple devices connected, pass
the serial from `adb devices -l`.

## start-server.sh

```sh
./tools/start-server.sh [--root]
```

By default starts the server as shell uid. With `--root`, starts
as root via `su -c`. The script reads the manager's starter script
from the device, fills in the paths, and runs it.

## stop-server.sh

```sh
./tools/stop-server.sh
```

Kills any process whose command line matches `DazkiServerMain`.

## pull-audit.sh

```sh
./tools/pull-audit.sh [output-path]
```

Pulls `/data/local/tmp/dazki-audit.jsonl` from the device. Default
output path is `./dazki-audit.jsonl`.

## clear-audit.sh

```sh
./tools/clear-audit.sh
```

Deletes the audit log on the device. Run this before a fresh test
pass so the audit log only contains entries from the current test.

## run-sample-test.sh

```sh
./tools/run-sample-test.sh
```

End to end test:

1. Installs the sample APK.
2. Starts the server if not running.
3. Launches the sample app.
4. Taps Request permission, Approve.
5. Taps Call listPackages.
6. Pulls the audit log and checks for a successful entry.

Exits 0 on success, 1 on failure.

## lint-all.sh

```sh
./tools/lint-all.sh
```

Runs `./gradlew lint` for every module. Reports are written to
`<module>/build/reports/lint-results-debug.html`.
