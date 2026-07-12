package dev.deathlegion.dazki.manager

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.IBinder
import dev.deathlegion.dazki.api.Dazki
import dev.deathlegion.dazki.api.IDazkiManagerCallback
import dev.deathlegion.dazki.api.IDazkiService
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Single source of truth for the manager UI. Holds the persisted allow
 * list, talks to the server binder, and exposes a small Kotlin API to
 * the ViewModels.
 *
 * This class is process-singleton inside the manager app. The server
 * is a separate process; we talk to it through IDazkiService.
 */
class DazkiManagerRepository(private val context: Context) {

    /** (uid, package) pairs the user has approved. */
    val allowedApps = mutableListOf<Pair<Int, String>>()

    private val prefs = context.getSharedPreferences("dazki_manager", Context.MODE_PRIVATE)

    init {
        loadFromPrefs()
    }

    /** True when the server process is up and reachable through ServiceManager. */
    fun isServiceRunning(): Boolean = Dazki.isServiceRunning()

    /** Returns the server's reported uid, or -1 when the server is not running. */
    fun serverUid(): Int {
        val service = rawService() ?: return -1
        return try {
            service.serverUid
        } catch (e: Throwable) {
            -1
        }
    }

    /**
     * Adds the package to the allow list, persists the change, and
     * updates the live server (if it is running).
     */
    fun allow(uid: Int, packageName: String) {
        synchronized(allowedApps) {
            if (allowedApps.none { it.first == uid && it.second == packageName }) {
                allowedApps.add(uid to packageName)
                saveToPrefs()
            }
        }
    }

    fun revoke(uid: Int, packageName: String) {
        synchronized(allowedApps) {
            allowedApps.removeAll { it.first == uid && it.second == packageName }
            saveToPrefs()
        }
    }

    fun isAllowed(uid: Int, packageName: String): Boolean {
        return synchronized(allowedApps) {
            allowedApps.any { it.first == uid && it.second == packageName }
        }
    }

    /** Lists installed third-party apps for the picker UI. */
    fun installedApps(): List<AppInfo> {
        val pm = context.packageManager
        val flags = PackageManager.GET_META_DATA
        return pm.getInstalledApplications(flags)
            .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
            .map { AppInfo(it.packageName, pm.getApplicationLabel(it).toString(), it.uid, it.sourceDir) }
            .sortedBy { it.label.lowercase() }
    }

    /** Hooks the manager callback into the running server. */
    fun registerWithServer(): Boolean {
        val service = rawService() ?: return false
        return try {
            service.registerManager(managerCallback)
            true
        } catch (e: Throwable) {
            false
        }
    }

    /** Asks the server to exit. Only works after registerWithServer(). */
    fun shutdown(): Boolean {
        val service = rawService() ?: return false
        return try {
            service.shutdown()
        } catch (e: Throwable) {
            false
        }
    }

    private fun rawService(): IDazkiService? {
        return try {
            val binder = Class.forName("android.os.ServiceManager")
                .getMethod("getService", String::class.java)
                .invoke(null, Dazki.SERVICE_NAME) as? IBinder
            binder?.let { IDazkiService.Stub.asInterface(it) }
        } catch (e: Throwable) {
            null
        }
    }

    private fun loadFromPrefs() {
        val raw = prefs.getString("allow_list", null) ?: return
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val uid = obj.getInt("uid")
                val pkg = obj.getString("pkg")
                allowedApps.add(uid to pkg)
            }
        } catch (_: Throwable) {
        }
    }

    private fun saveToPrefs() {
        val arr = JSONArray()
        allowedApps.forEach { (uid, pkg) ->
            arr.put(JSONObject().apply {
                put("uid", uid)
                put("pkg", pkg)
            })
        }
        prefs.edit().putString("allow_list", arr.toString()).apply()
    }

    /**
     * The manager-side callback. The server invokes this when a client
     * app calls requestPermission. We launch the permission activity
     * to ask the user.
     */
    /** Builds the starter shell script with current paths filled in, for the user to copy. */
    fun buildStarterScript(): String {
        val apkPath = resolveManagerApkPath()
        val dataDir = context.filesDir.absolutePath
        val allowList = "/data/local/tmp/dazki-allowlist.txt"
        val template = context.assets.open("dazki-starter.sh").bufferedReader().readText()
        return template
            .replace("__APK_PATH__", apkPath)
            .replace("__DATA_DIR__", dataDir)
            .replace("__ALLOW_LIST__", allowList)
    }

    private fun resolveManagerApkPath(): String {
        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(context.packageName, 0)
            info.sourceDir
        } catch (e: Throwable) {
            "/data/app/${context.packageName}/base.apk"
        }
    }

    private val managerCallback = object : IDazkiManagerCallback.Stub() {
        override fun onPermissionRequested(packageName: String, uid: Int) {
            PermissionRequestActivity.launch(context, packageName, uid)
        }

        override fun onServerExited(code: Int) {
            // The UI polls isServiceRunning so no explicit push needed.
        }
    }
}

data class AppInfo(
    val packageName: String,
    val label: String,
    val uid: Int,
    val sourceDir: String,
)
