<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:0F0C29,50:302B63,100:24243E&height=200&section=header&text=dazki&fontSize=70&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=A%20binder%20bridge%20between%20AI%20assistants%20and%20rooted%20Android&descAlignY=58&descSize=16" width="100%"/>

<img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=20&pause=1000&color=8A2BE2&center=true&vCenter=true&width=600&lines=Privileged+service+for+rooted+Android;Typed+binder+calls%2C+not+shelled-out+commands;ADB+or+root+startup%2C+no+system+app+required;Built+from+scratch%2C+no+fork." alt="Typing SVG" />

<br/>

[![Kotlin](https://img.shields.io/badge/Kotlin-81.3%25-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-Root%2FADB-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![AIDL](https://img.shields.io/badge/AIDL-Binder%20IPC-00599C?style=for-the-badge)](https://developer.android.com/develop/background-work/services/aidl)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.0.0-blueviolet?style=for-the-badge)](https://github.com/deathlegionteamlk/dazki/releases)

[![Build](https://img.shields.io/github/actions/workflow/status/deathlegionteamlk/dazki/build.yml?style=flat-square&label=build&logo=github)](../../actions)
[![Lint](https://img.shields.io/github/actions/workflow/status/deathlegionteamlk/dazki/lint.yml?style=flat-square&label=lint&logo=android)](../../actions)
[![Stars](https://img.shields.io/github/stars/deathlegionteamlk/dazki?style=flat-square&color=gold)](../../stargazers)
[![Issues](https://img.shields.io/github/issues/deathlegionteamlk/dazki?style=flat-square)](../../issues)

</div>

---

## What this actually is

Android apps that need a system-level API normally have two bad options: request a permission the user has to grant by hand, or shell out to a root command and parse whatever text comes back. Both are slow, both are fragile, both fall apart the moment an OEM changes something.

dazki is a third option. Start it once through ADB or root, and it runs as a privileged service that exposes system APIs over a typed AIDL binder. Your app talks to it like any other bound service — real method calls, real types, no `Runtime.exec()`, no scraping stdout. It's an original implementation, built from the ground up for this project rather than adapted from an existing tool.

## Layout

```
dazki/
├── manager/              # the app users install to start/manage the service
├── permission-system/    # who's allowed to call what
├── plugin-system/        # load capabilities as plugins instead of baking them in
├── networking/           # transport layer for client <-> service calls
├── hidden-api-stubs/     # compile-time stubs for restricted Android APIs
├── ai-connectors/        # hooks for AI assistants to drive the service
├── claude-skill/         # a Claude skill that speaks dazki's API
├── api/                  # the public interface apps build against
├── examples/hello-plugin/# minimal plugin, start reading here
├── sample/               # a working sample app
├── tools/                # dev-side utilities
├── tutorials/            # walkthroughs
└── tests/
```

If you're trying to understand the project fast, `examples/hello-plugin` is the shortest path — it's smaller than the docs.

## Getting it running

You need a rooted device or a machine with ADB access to the target device. There's no Play Store install path — this isn't that kind of app.

```bash
git clone https://github.com/deathlegionteamlk/dazki.git
cd dazki
./gradlew build
```

Install the manager app, start the service through it (root or `adb shell`), then point your client app at the binder interface exposed under `api/`. Full steps are in `docs/` and `tutorials/` — worth reading before you assume the API surface, since it's still v1.0.0 and can move.

## Why root instead of a permission

Because some things Android just doesn't expose to normal apps, permission or not. dazki's bet is that a privileged service you explicitly start is a more honest trade-off than an app quietly requesting broader access than it needs. The audit-log piece exists so that trade-off is checkable, not just assumed.

## Stack

Kotlin does the heavy lifting (81%), with Python for tooling, Shell for the ADB/root startup path, AIDL for the actual binder contracts, and a small sliver of Java. Nothing exotic — the interesting part is the architecture, not the language mix.

## Contributing

Bug reports and feature requests go through the [issue templates](.github/ISSUE_TEMPLATE). Security issues do **not** go through GitHub issues — see [SECURITY.md](SECURITY.md) for the email contact. Everything else, including how PRs get reviewed, is in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

MIT. See [LICENSE](LICENSE).

---

<div align="center">

Built by **Death Legion Team**

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:24243E,50:302B63,100:0F0C29&height=100&section=footer" width="100%"/>

</div>
