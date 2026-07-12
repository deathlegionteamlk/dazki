# Play Protect fix

Play Protect sometimes flags the dazki debug APK with a warning like
"App not from a trusted developer" or blocks the install entirely.
This document explains why and how to fix it.

## Why Play Protect flags dazki

Three reasons, in order of how often they show up:

1. **Debug signing key.** The debug APK is signed with the standard
   Android debug keystore, which Play Protect treats as untrusted.
   Every debug APK from every developer on Earth produces the same
   signature, so Play Protect cannot tell them apart.

2. **QUERY_ALL_PACKAGES permission.** dazki needs to list every
   installed package so the user can pick which app to allow. Play
   Protect treats this permission as suspicious because most apps do
   not need it.

3. **No Play Store listing.** Sideloaded apps go through extra
   heuristic checks. dazki trips several of them: it uses ADB, it
   spawns a service that registers in ServiceManager, and it talks
   to system APIs through reflection.

## How to dismiss the warning

When Play Protect shows "App not from a trusted developer":

1. Tap "More details".
2. Tap "Install anyway".
3. The install proceeds.

When Play Protect blocks the install entirely ("App blocked"):

1. Open the Play Store app.
2. Tap your profile picture in the top right.
3. Tap "Play Protect".
4. Tap the gear icon in the top right.
5. Toggle "Scan apps with Play Protect" off.
6. Install dazki.
7. Toggle Play Protect back on.

Play Protect will scan dazki on the next pass and may flag it
again. If you trust the build, dismiss the warning each time.

## How to stop the warning from appearing

For a build that does not trip Play Protect, do all of these:

### 1. Sign with a real key

Generate a release keystore:

```sh
keytool -genkey -v -keystore dazki-release.keystore \
    -alias dazki -keyalg RSA -keysize 4096 -validity 10000
```

Then add a signing config to `manager/build.gradle.kts`:

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("../dazki-release.keystore")
            storePassword = System.getenv("DAZKI_KS_PW")
            keyAlias = "dazki"
            keyPassword = System.getenv("DAZKI_KEY_PW")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

Build the release APK:

```sh
export DAZKI_KS_PW=...
export DAZKI_KEY_PW=...
./gradlew :manager:assembleRelease
```

### 2. Strip QUERY_ALL_PACKAGES

`QUERY_ALL_PACKAGES` is the broadest query permission. Android 11+
apps that target SDK 30+ need it to see all installed apps. If you
only need to see specific apps, replace it with a `<queries>
element in the manifest that lists the packages you care about:

```xml
<manifest>
    <queries>
        <package android:name="dev.deathlegion.dazki.sample" />
        <!-- add one line per app you want dazki to see -->
    </queries>
</manifest>
```

The dazki manager keeps `QUERY_ALL_PACKAGES` because the whole
point is to let the user pick from every installed app. If you are
forking dazki for a narrow use case, drop the permission and use
the `<queries>` block.

### 3. Add a Privacy Policy link

Play Protect treats apps without a privacy policy as higher risk.
Add a link in the manager app's About screen to a real privacy
policy page. The policy needs to explain what dazki collects
(nothing, in our case) and what it forwards to system services.

### 4. Submit to Play Protect via App Sampling

Google has a form for developers to submit their sideloaded apps
to Play Protect's allow list. The form is at
https://support.google.com/googleplay/android-developer/contact/playprotectappeals
Fill it out, attach the signed APK, and wait. The review takes a
few weeks.

## What does NOT help

- Renaming the package. Play Protect fingerprints the APK, not the
  package name.
- Changing the version code. Same reason.
- Repackaging with a different tool. Same reason.
- Disabling Play Protect permanently. The warning comes back the
  next time Play Protect scans the device, and disabling Play
  Protect removes a layer of protection the user actually wants.

## Reference

Google documents Play Protect's behavior at
https://support.google.com/googleplay/answer/2812853. The
heuristic flags for sideloaded apps are not public, but the
debug-key and QUERY_ALL_PACKAGES signals are widely reported in
developer forums.
