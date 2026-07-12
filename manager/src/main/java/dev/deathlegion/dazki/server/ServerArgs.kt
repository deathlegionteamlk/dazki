package dev.deathlegion.dazki.server

import android.content.Context
import java.io.File

/**
 * Parsed command line arguments passed to DazkiServerMain. The starter
 * script (see assets/dazki-starter.sh) fills these in.
 *
 * apkPath      : path to the manager APK, used as the app_process classpath
 * dataDir      : /data/data/dev.deathlegion.dazki.manager.debug (or release)
 * allowListFile: file the server reads/writes its allow list to
 * debug        : enables verbose logging
 * systemContext: a Context the server can use to reach Android services
 */
data class ServerArgs(
    val apkPath: String,
    val dataDir: String,
    val allowListFile: String,
    val debug: Boolean,
    val systemContext: Context
) {
    companion object {
        fun parse(args: Array<String>, systemContext: Context): ServerArgs {
            var apkPath = ""
            var dataDir = ""
            var allowListFile = "/data/local/tmp/dazki-allowlist.txt"
            var debug = false
            for (arg in args) {
                when {
                    arg.startsWith("--apk=") -> apkPath = arg.removePrefix("--apk=")
                    arg.startsWith("--data-dir=") -> dataDir = arg.removePrefix("--data-dir=")
                    arg.startsWith("--allow-list=") -> allowListFile = arg.removePrefix("--allow-list=")
                    arg == "--debug" -> debug = true
                }
            }
            require(apkPath.isNotEmpty()) { "--apk= is required" }
            return ServerArgs(apkPath, dataDir, allowListFile, debug, systemContext)
        }
    }
}
