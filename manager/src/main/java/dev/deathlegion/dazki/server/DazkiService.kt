package dev.deathlegion.dazki.server

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.os.ServiceManager
import android.os.SystemProperties
import android.provider.Settings
import android.util.Log
import dev.deathlegion.dazki.api.IDazkiManagerCallback
import dev.deathlegion.dazki.api.IDazkiService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Implementation of the public IDazkiService binder. Lives inside the
 * privileged server process started by app_process. Each call checks
 * the calling uid against the allow list before forwarding to the
 * underlying Android system service.
 *
 * The server keeps its allow list in memory. The manager app pushes
 * the list on connect and updates it whenever the user grants or
 * revokes an app.
 */
class DazkiService(
    private val serverArgs: ServerArgs
) : IDazkiService.Stub() {

    private val tag = "Dazki/Service"

    /** uid -> package name. Either field grants access. */
    private val allowedUids = ConcurrentHashMap<Int, String>()
    private val allowedPackages = CopyOnWriteArrayList<String>()

    @Volatile
    private var managerCallback: IDazkiManagerCallback? = null

    @Volatile
    private var managerUid: Int = -1

    /** Manager calls this after launching the server, passing the persisted list. */
    fun bootstrapAllowedList(pairs: List<Pair<Int, String>>) {
        pairs.forEach { (uid, pkg) ->
            allowedUids[uid] = pkg
            if (pkg.isNotEmpty()) allowedPackages.add(pkg)
        }
        Log.i(tag, "Bootstrapped allow list with ${pairs.size} entries")
    }

    override fun getVersion(): Int = dev.deathlegion.dazki.api.Dazki.VERSION

    override fun getServerUid(): Int = Process.myUid()

    override fun isCallingAppAllowed(): Boolean {
        val callerUid = getCallingUid()
        val callerPkg = callingPackageName(callerUid)
        if (allowedUids.containsKey(callerUid)) return true
        if (callerPkg != null && allowedPackages.contains(callerPkg)) return true
        Log.i(tag, "Reject: uid=$callerUid pkg=$callerPkg is not on allow list")
        return false
    }

    override fun requestPermission(packageName: String, uid: Int) {
        Log.i(tag, "Permission request from pkg=$packageName uid=$uid")
        try {
            managerCallback?.onPermissionRequested(packageName, uid)
        } catch (e: Throwable) {
            Log.w(tag, "Manager callback failed", e)
        }
    }

    override fun registerManager(callback: IDazkiManagerCallback?) {
        managerCallback = callback
        managerUid = getCallingUid()
        Log.i(tag, "Manager callback registered, uid=$managerUid")
    }

    override fun shutdown(): Boolean {
        val callerUid = getCallingUid()
        if (callerUid != managerUid) {
            Log.w(tag, "shutdown rejected: caller uid=$callerUid is not the manager uid=$managerUid")
            return false
        }
        Log.i(tag, "Shutdown requested by manager. Quitting Looper.")
        try {
            notifyManagerExited(0)
        } finally {
            android.os.Looper.getMainLooper().quit()
        }
        return true
    }

    // ----- privileged operations -----

    override fun listPackages(flags: Int): List<String> {
        if (!isCallingAppAllowed()) {
            throw SecurityException("Caller is not allowed")
        }
        val context = serverArgs.systemContext
        val pm = context.packageManager
        val effectiveFlags = flags or android.content.pm.PackageManager.GET_META_DATA
        val infos = pm.getInstalledPackages(effectiveFlags)
        return infos.map { it.packageName }
    }

    override fun forceStopPackage(packageName: String): Boolean {
        if (!isCallingAppAllowed()) {
            throw SecurityException("Caller is not allowed")
        }
        return try {
            // Shell uid holds FORCE_STOP_PACKAGES through the shell role.
            // On Android 9 and below, forceStopPackage is callable directly.
            // On Android 10+, the same call goes through, but only for
            // packages the shell uid can see.
            val am = serverArgs.systemContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.javaClass.getMethod("forceStopPackage", String::class.java).invoke(am, packageName)
            Log.i(tag, "forceStopPackage ok: $packageName")
            true
        } catch (e: Throwable) {
            Log.w(tag, "forceStopPackage failed for $packageName", e)
            false
        }
    }

    override fun getSettingsSecureInt(key: String, def: Int): Int {
        if (!isCallingAppAllowed()) {
            throw SecurityException("Caller is not allowed")
        }
        return try {
            Settings.Secure.getInt(serverArgs.systemContext.contentResolver, key, def)
        } catch (e: Throwable) {
            Log.w(tag, "getSettingsSecureInt failed for $key", e)
            def
        }
    }

    override fun putSettingsGlobalInt(key: String, value: Int): Boolean {
        if (!isCallingAppAllowed()) {
            throw SecurityException("Caller is not allowed")
        }
        return try {
            Settings.Global.putInt(serverArgs.systemContext.contentResolver, key, value)
        } catch (e: Throwable) {
            Log.w(tag, "putSettingsGlobalInt failed for $key", e)
            false
        }
    }

    private fun callingPackageName(uid: Int): String? {
        return try {
            val pm = serverArgs.systemContext.packageManager
            val names = pm.getPackagesForUid(uid)
            names?.firstOrNull()
        } catch (e: Throwable) {
            null
        }
    }

    /** Called from DazkiServerMain on shutdown. */
    fun notifyManagerExited(code: Int) {
        try {
            managerCallback?.onServerExited(code)
        } catch (_: Throwable) {
        }
    }
}
