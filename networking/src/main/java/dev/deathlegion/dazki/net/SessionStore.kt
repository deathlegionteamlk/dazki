package dev.deathlegion.dazki.net

import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory session store. The manager app loads persisted sessions
 * from SharedPreferences on startup and saves changes back through
 * the persist() callback.
 *
 * Token lookups are O(1). Pairing codes are also kept in a map so
 * the pairing flow can resolve a 6 digit code to a token in one
 * step.
 */
class SessionStore(
    private val onPersist: (List<AiSession>) -> Unit,
) {
    private val byToken = ConcurrentHashMap<String, AiSession>()
    private val byCode = ConcurrentHashMap<String, AiSession>()

    fun add(session: AiSession) {
        byToken[session.token] = session
        byCode[session.pairingCode] = session
        persist()
    }

    fun findByToken(token: String): AiSession? = byToken[token]

    fun findByCode(code: String): AiSession? = byCode[code]

    fun confirm(code: String): AiSession? {
        val session = byCode.remove(code) ?: return null
        byToken[session.token] = session
        persist()
        return session
    }

    fun revoke(token: String) {
        byToken.remove(token)
        byCode.entries.removeIf { it.value.token == token }
        persist()
    }

    fun revokeAll() {
        byToken.clear()
        byCode.clear()
        persist()
    }

    fun all(): List<AiSession> = byToken.values.toList()

    private fun persist() {
        onPersist(all())
    }
}
