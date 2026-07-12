package dev.deathlegion.dazki.plugin

import java.io.File
import java.net.URLClassLoader

/**
 * Loads plugins from DEX files. Each plugin lives in its own DEX
 * under /data/local/tmp/dazki-plugins/<id>.dex. The class listed in
 * META-INF/dazki-plugin.txt is the entry point.
 *
 * Each plugin gets its own URLClassLoader so plugins cannot see each
 * other's classes. They can only see the dazki-plugin-api jar (which
 * the server shares through its own classloader).
 */
class PluginLoader(private val pluginDir: File) {

    private val loaded = mutableMapOf<String, DazkiPlugin>()

    fun loadAll(): List<DazkiPlugin> {
        if (!pluginDir.exists()) return emptyList()
        val dexFiles = pluginDir.listFiles { f -> f.name.endsWith(".dex") } ?: return emptyList()
        for (dex in dexFiles) {
            try {
                val plugin = loadOne(dex)
                loaded[plugin.id] = plugin
            } catch (e: Throwable) {
                System.err.println("Failed to load plugin ${dex.name}: ${e.message}")
            }
        }
        return loaded.values.toList()
    }

    private fun loadOne(dex: File): DazkiPlugin {
        val classloader = URLClassLoader(arrayOf(dex.toURI().toURL()), javaClass.classLoader)
        val entryClassName = readEntryClass(dex) ?: throw IllegalStateException("no entry class in ${dex.name}")
        val cls = classloader.loadClass(entryClassName)
        return cls.getDeclaredConstructor().newInstance() as DazkiPlugin
    }

    /**
     * Reads the entry class name from a sibling .txt file. The file
     * contains one line: the fully qualified class name. We use a
     * sibling file because reading META-INF from a DEX requires more
     * code than worth it for this small project.
     */
    private fun readEntryClass(dex: File): String? {
        val meta = File(dex.parentFile, dex.nameWithoutExtension + ".txt")
        if (!meta.exists()) return null
        return meta.readText().trim().takeIf { it.isNotBlank() }
    }

    fun get(id: String): DazkiPlugin? = loaded[id]
    fun all(): List<DazkiPlugin> = loaded.values.toList()
}
