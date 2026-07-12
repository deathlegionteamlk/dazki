package dev.deathlegion.dazki.permission

/**
 * One grant of a capability to a principal. A principal is either an
 * Android app (uid + package name) or an AI session (token).
 *
 * Grants are persisted by the manager app. The server loads them on
 * startup and reloads when the manager calls refreshGrants().
 */
data class Grant(
    val principal: Principal,
    val capability: Capability,
    val grantedAtMs: Long,
    val grantedBy: String,
    val expiresAtMs: Long = 0L,
)

sealed class Principal {
    data class App(val uid: Int, val packageName: String) : Principal()
    data class Session(val token: String) : Principal()
}
