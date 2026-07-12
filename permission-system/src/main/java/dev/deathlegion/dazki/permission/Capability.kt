package dev.deathlegion.dazki.permission

/**
 * One capability an app or session may hold. Capabilities are
 * hierarchical: a capability ending in ".*" implies all capabilities
 * under that prefix.
 *
 * Example:
 *   packages.read         read package info
 *   packages.write        modify package state
 *   settings.global.read  read Settings.Global
 *   settings.global.write write Settings.Global
 *   settings.secure.read  read Settings.Secure
 *   settings.secure.write write Settings.Secure
 *   shell.exec            run shell commands as server uid
 *   logcat.read           dump logcat
 *   files.read            read files under /data/local/tmp
 *   files.write           write files under /data/local/tmp
 */
data class Capability(val name: String) {
    fun implies(other: Capability): Boolean {
        if (name == other.name) return true
        if (name.endsWith(".*")) {
            val prefix = name.dropLast(1)
            return other.name.startsWith(prefix)
        }
        return false
    }
}

object Capabilities {
    val PACKAGES_READ = Capability("packages.read")
    val PACKAGES_WRITE = Capability("packages.write")
    val SETTINGS_GLOBAL_READ = Capability("settings.global.read")
    val SETTINGS_GLOBAL_WRITE = Capability("settings.global.write")
    val SETTINGS_SECURE_READ = Capability("settings.secure.read")
    val SETTINGS_SECURE_WRITE = Capability("settings.secure.write")
    val SETTINGS_SYSTEM_READ = Capability("settings.system.read")
    val SETTINGS_SYSTEM_WRITE = Capability("settings.system.write")
    val SHELL_EXEC = Capability("shell.exec")
    val LOGCAT_READ = Capability("logcat.read")
    val FILES_READ = Capability("files.read")
    val FILES_WRITE = Capability("files.write")
    val ALL = Capability("*")

    val DEFAULT_FOR_APP = setOf(
        PACKAGES_READ,
        SETTINGS_GLOBAL_READ,
        SETTINGS_SECURE_READ,
    )

    val DEFAULT_FOR_AI = setOf(
        PACKAGES_READ,
        SETTINGS_GLOBAL_READ,
        LOGCAT_READ,
    )
}
