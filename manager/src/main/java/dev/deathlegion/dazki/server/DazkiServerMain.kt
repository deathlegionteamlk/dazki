package dev.deathlegion.dazki.server

import android.content.Context
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.Process
import android.util.Log
import dev.deathlegion.dazki.api.Dazki
import java.io.File

/**
 * Entry point of the privileged server process. Started by the starter
 * script via app_process:
 *
 *   app_process -Djava.class.path=<manager.apk> /system/bin DazkiServerMain --apk=<apk> ...
 *
 * The process runs as shell uid (2000) when started from ADB, or as
 * root (0) when started via su. In both cases it can register a binder
 * under "dazki_service" in ServiceManager.
 *
 * The class is loaded from the manager APK because app_process is given
 * the APK as its classpath. The same APK also contains the manager UI,
 * which is a normal Android app launched separately.
 */
object DazkiServerMain {

    private const val TAG = "Dazki/ServerMain"

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            Log.i(TAG, "Dazki server starting, args=${args.joinToString(" ")}")
            Log.i(TAG, "uid=${Process.myUid()} sdk=${Build.VERSION.SDK_INT} abi=${Build.SUPPORTED_ABIS.joinToString(",")}")

            // System context, obtained the same way Shizuku does it. This
            // gives us a PackageManager and ContentResolver that work as
            // the calling uid, which is shell or root.
            val systemContext = createSystemContext()
            val parsed = ServerArgs.parse(args, systemContext)
            if (parsed.debug) {
                Log.i(TAG, "parsed args: $parsed")
            }

            // Bring up the binder. Looper must be prepared before we
            // register, because Binder calls arrive on this thread.
            Looper.prepareMainLooper()

            val service = DazkiService(parsed)
            service.bootstrapAllowedList(loadAllowList(parsed.allowListFile))

            try {
                addServiceViaReflection(Dazki.SERVICE_NAME, service.asBinder())
                Log.i(TAG, "Registered binder '${Dazki.SERVICE_NAME}' in ServiceManager")
            } catch (e: Throwable) {
                Log.e(TAG, "Failed to register binder. Shell uid cannot addService on this Android build.", e)
                throw e
            }

            Log.i(TAG, "Server ready. Waiting for binder calls.")
            Looper.loop()

            Log.i(TAG, "Server exiting.")
            service.notifyManagerExited(0)
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error in server main", t)
            System.exit(1)
        }
    }

    /**
     * Returns a Context that can be used to obtain PackageManager, etc.
     * Uses ActivityThread.systemMain() which is the same path the system
     * server takes. The returned context runs as our uid (shell or root).
     *
     * ActivityThread is hidden from the SDK, so we go through reflection.
     */
    private fun createSystemContext(): Context {
        val clazz = Class.forName("android.app.ActivityThread")
        val systemMain = clazz.getMethod("systemMain")
        val activityThread = systemMain.invoke(null)
        val getSystemContext = clazz.getMethod("getSystemContext")
        return getSystemContext.invoke(activityThread) as Context
    }

    /** Reflection wrapper around ServiceManager.addService, which is hidden. */
    private fun addServiceViaReflection(name: String, binder: IBinder) {
        val clazz = Class.forName("android.os.ServiceManager")
        val method = clazz.getMethod("addService", String::class.java, IBinder::class.java)
        method.invoke(null, name, binder)
    }

    /**
     * Reads the allow list from disk. Format is one entry per line:
     *   <uid> <package-name>
     * Comments start with '#'. Empty lines ignored.
     */
    private fun loadAllowList(path: String): List<Pair<Int, String>> {
        val file = File(path)
        if (!file.exists()) return emptyList()
        return try {
            file.readLines()
                .filter { it.isNotBlank() && !it.startsWith("#") }
                .mapNotNull { line ->
                    val parts = line.trim().split(Regex("\\s+"), limit = 2)
                    if (parts.size == 2) parts[0].toIntOrNull() to parts[1]
                    else null
                }
                .filter { it.first != null }
                .map { it.first!! to it.second }
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to read allow list at $path", e)
            emptyList()
        }
    }
}
