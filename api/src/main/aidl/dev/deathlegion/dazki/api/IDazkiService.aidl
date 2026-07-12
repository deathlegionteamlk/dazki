// IDazkiService.aidl
//
// Wire protocol between client apps and the Dazki privileged server.
// The server registers an instance of this interface under
// "dazki_service" in ServiceManager. Client apps obtain it through
// Dazki.getService() in the api module.

package dev.deathlegion.dazki.api;

import dev.deathlegion.dazki.api.IDazkiManagerCallback;

interface IDazkiService {

    // Server-reported version code. Bump when the wire format changes.
    int getVersion();

    // Shell uid (2000) when started from ADB, or 0 when started as root.
    // Client apps need to know this because some system APIs refuse to
    // run for shell uid even though the call goes through us.
    int getServerUid();

    // True when the calling uid + package combo is on the allow list.
    // The server rejects every privileged call below if this is false.
    boolean isCallingAppAllowed();

    // Asks the manager app to show a permission dialog. The result comes
    // back through IDazkiManagerCallback.onPermissionResult.
    void requestPermission(String packageName, int uid);

    // Manager-only entry point. The manager app calls this once after
    // it has launched the server, so the server can push status updates
    // and permission requests back into the manager process.
    void registerManager(IDazkiManagerCallback callback);

    // Manager-only. Stops the server. The server checks the calling
    // uid against the one that called registerManager and exits if they
    // match. Returns true when the shutdown was accepted.
    boolean shutdown();

    // ----- privileged operations below this line -----

    // Lists installed package names. flags is PackageManager flags such
    // as MATCH_UNINSTALLED_PACKAGES (0x2000) that normal apps cannot pass.
    List<String> listPackages(int flags);

    // Force stops a package. Requires FORCE_STOP_PACKAGES, which the
    // shell uid holds but normal apps do not.
    boolean forceStopPackage(String packageName);

    // Reads a Settings.Secure int value. Some keys are read-restricted
    // to system uid, so going through us widens what apps can read.
    int getSettingsSecureInt(String key, int def);

    // Writes a Settings.Global int value. Restricted to system/shell.
    boolean putSettingsGlobalInt(String key, int value);
}
