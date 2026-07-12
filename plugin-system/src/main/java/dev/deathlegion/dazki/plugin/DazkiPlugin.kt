package dev.deathlegion.dazki.plugin

/**
 * One plugin. A plugin is a small Kotlin object that registers extra
 * RPC methods with the dazki server. The server loads plugins from
 * DEX files dropped into /data/local/tmp/dazki-plugins/.
 *
 * Plugins are isolated: each one runs in its own classloader and
 * cannot touch the server's internals except through the PluginContext
 * passed to onInit.
 */
interface DazkiPlugin {
    /** Plugin id. Must be unique across all loaded plugins. */
    val id: String

    /** Display name shown in the manager UI. */
    val displayName: String

    /** Called once when the server loads the plugin. */
    fun onInit(ctx: PluginContext)

    /** Called once when the server is shutting down. */
    fun onShutdown()

    /** RPC methods this plugin contributes. Method names are prefixed with the plugin id. */
    fun methods(): List<PluginMethod>
}

/** Context the server passes to a plugin. */
interface PluginContext {
    fun log(level: String, msg: String)
    fun audit(method: String, args: Map<String, Any?>, ok: Boolean, latencyMs: Int)
    fun readFile(path: String): ByteArray
    fun writeFile(path: String, data: ByteArray)
}

/** One method exposed by a plugin. */
data class PluginMethod(
    val name: String,
    val scopes: Set<String>,
    val handler: (args: Map<String, Any?>) -> Map<String, Any?>,
)
