package dev.deathlegion.dazki.net

/**
 * Wire format for one RPC frame. The frame is length-prefixed JSON
 * over a WebSocket text frame:
 *
 *   0x00 | 4 bytes big-endian length | JSON payload
 *
 * The 0x00 type byte lets us extend the protocol later (binary
 * frames, compressed frames, etc.) without breaking existing
 * clients.
 */
data class RpcFrame(
    val token: String,
    val method: String,
    val args: Map<String, Any?>,
    val seq: Long,
)

data class RpcReply(
    val ok: Boolean,
    val result: Map<String, Any?> = emptyMap(),
    val error: String? = null,
    val seq: Long = 0L,
)

/** Capability scopes a token may hold. */
object Scopes {
    const val PACKAGES_READ = "packages.read"
    const val PACKAGES_WRITE = "packages.write"
    const val SETTINGS_READ = "settings.read"
    const val SETTINGS_WRITE = "settings.write"
    const val SHELL_EXEC = "shell.exec"
    const val LOGCAT_READ = "logcat.read"
    const val FILES_READ = "files.read"
    const val FILES_WRITE = "files.write"

    val ALL = setOf(
        PACKAGES_READ, PACKAGES_WRITE,
        SETTINGS_READ, SETTINGS_WRITE,
        SHELL_EXEC, LOGCAT_READ,
        FILES_READ, FILES_WRITE,
    )

    val DEFAULT = setOf(PACKAGES_READ, SETTINGS_READ)
}
