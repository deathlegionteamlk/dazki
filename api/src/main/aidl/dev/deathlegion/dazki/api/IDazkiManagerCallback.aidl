// IDazkiManagerCallback.aidl
//
// Callback the manager app registers with the server. The server uses
// it to push status updates and to relay permission requests from
// client apps back into the manager UI.

package dev.deathlegion.dazki.api;

interface IDazkiManagerCallback {
    // Called when a client app invoked IDazkiService.requestPermission.
    // The manager shows a dialog and calls back into the server with the
    // result through IDazkiService (or just updates its in-memory list).
    void onPermissionRequested(String packageName, int uid);

    // Called when the server is about to exit, so the manager can update
    // its status pill.
    void onServerExited(int code);
}
