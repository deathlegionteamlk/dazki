package dev.deathlegion.dazki.api

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import java.io.File
import java.lang.reflect.Method

/**
 * Public entry point for client apps. Wraps the binder exposed by the
 * Dazki server and hides the ServiceManager lookup behind a small
 * Kotlin surface.
 *
 * Typical usage from a third-party app:
 *
 *     Dazki.init(applicationContext)
 *     if (!Dazki.isServiceRunning()) {
 *         // show "start Dazki from the manager app" message
 *     }
 *     if (!Dazki.isAllowed()) {
 *         Dazki.requestPermission(this)
 *     }
 *     val service = Dazki.getService()
 *     val packages = service.listPackages(0)
 *
 * The class is safe to call from any thread. Binder calls block.
 */
object Dazki {

    private const val TAG = "Dazki"

    /** Binder name the server registers under in ServiceManager. */
    const val SERVICE_NAME = "dazki_service"

    /** Wire protocol version. Must match IDazkiService.getVersion(). */
    const val VERSION = 1

    /** Manager app package, used to launch the permission request UI. */
    const val MANAGER_PACKAGE = "dev.deathlegion.dazki.manager"

    /** Activity in the manager app that handles permission requests. */
    const val PERMISSION_ACTIVITY = "dev.deathlegion.dazki.manager.PermissionRequestActivity"

    /** Path the manager writes the starter script to, for reference only. */
    const val STARTER_PATH = "/data/local/tmp/dazki-starter.sh"

    @Volatile private var cachedBinder: IBinder? = null

    private lateinit var appContext: Context

    /**
     * Must be called once from Application.onCreate. The Dazki API keeps
     * a reference to the application context so it can launch the manager
     * permission activity and query package signatures.
     */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** True when the server process is up and the binder is reachable. */
    fun isServiceRunning(): Boolean {
        return try {
            getServiceRaw() != null
        } catch (e: Throwable) {
            Log.w(TAG, "isServiceRunning failed", e)
            false
        }
    }

    /**
     * True when the calling app is on the server's allow list.
     * Returns false when the service is not running.
     */
    fun isAllowed(): Boolean {
        val binder = getServiceRaw() ?: return false
        return try {
            val service = IDazkiService.Stub.asInterface(binder)
            service.isCallingAppAllowed
        } catch (e: RemoteException) {
            Log.w(TAG, "isAllowed failed", e)
            false
        }
    }

    /**
     * Returns a connected IDazkiService. Throws IllegalStateException when
     * the server is not running, and SecurityException when the calling
     * app is not allowed.
     */
    fun getService(): IDazkiService {
        val binder = getServiceRaw()
            ?: throw IllegalStateException("Dazki service is not running. Start it from the manager app.")
        val service = IDazkiService.Stub.asInterface(binder)
        if (!service.isCallingAppAllowed) {
            throw SecurityException("Calling app is not allowed to use Dazki. Call requestPermission first.")
        }
        return service
    }

    /**
     * Launches the manager app's permission request activity. The user
     * approves or denies inside the manager. The result is delivered
     * back through the server binder on the next call.
     *
     * Returns true when the intent was resolvable, false otherwise.
     */
    fun requestPermission(activity: Context): Boolean {
        val intent = Intent().apply {
            component = ComponentName(MANAGER_PACKAGE, PERMISSION_ACTIVITY)
            putExtra(PermissionRequestExtra.EXTRA_PACKAGE_NAME, activity.packageName)
            putExtra(PermissionRequestExtra.EXTRA_CALLER_UID, android.os.Process.myUid())
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val pm = activity.packageManager
        if (intent.resolveActivity(pm) == null) {
            Log.w(TAG, "Manager app is not installed. Cannot request permission.")
            return false
        }
        activity.startActivity(intent)
        return true
    }

    /**
     * Returns the raw IBinder that the server registered in ServiceManager.
     * Uses reflection because ServiceManager.getService is hidden from
     * the SDK. Returns null when the service is not registered.
     */
    private fun getServiceRaw(): IBinder? {
        cachedBinder?.let { existing ->
            if (existing.isBinderAlive) return existing
            cachedBinder = null
        }
        val binder = peekServiceManagerBinder(SERVICE_NAME) ?: return null
        if (!binder.isBinderAlive) return null
        cachedBinder = binder
        return binder
    }

    private fun peekServiceManagerBinder(name: String): IBinder? {
        return try {
            val clazz = Class.forName("android.os.ServiceManager")
            val method: Method = clazz.getMethod("getService", String::class.java)
            method.invoke(null, name) as? IBinder
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "ServiceManager not available on this Android build", e)
            null
        } catch (e: ReflectiveOperationException) {
            Log.w(TAG, "ServiceManager.getService failed", e)
            null
        }
    }

    /** Human-readable summary of the Android version and ABI. Useful in support emails. */
    fun platformInfo(): String {
        val abi = Build.SUPPORTED_ABIS.joinToString(",")
        return "Android ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT}), abi=$abi, device=${Build.MODEL}"
    }

    /** Clears the cached binder. Call this when you suspect the server restarted. */
    fun invalidate() {
        cachedBinder = null
    }
}

/** Extras used by the permission request intent. */
object PermissionRequestExtra {
    const val EXTRA_PACKAGE_NAME = "dev.deathlegion.dazki.extra.PACKAGE_NAME"
    const val EXTRA_CALLER_UID = "dev.deathlegion.dazki.extra.CALLER_UID"
    const val EXTRA_RESULT_GRANTED = "dev.deathlegion.dazki.extra.RESULT_GRANTED"
}

/** A small set of PackageManager flag constants that apps cannot pass directly. */
object DazkiFlags {
    const val MATCH_UNINSTALLED_PACKAGES = 0x2000
    const val MATCH_DISABLED_COMPONENTS = 0x200
}
