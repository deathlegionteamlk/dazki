package dev.deathlegion.dazki.net

/**
 * Token validator. Checks the token is known, not revoked, and
 * within its rate limit. Returns the session when valid, null
 * otherwise.
 *
 * The validator is split out from the WebSocket server so it can
 * be unit tested without spinning up a real socket.
 */
class TokenValidator(
    private val sessions: SessionStore,
    private val rateLimiter: RateLimiter,
) {
    fun validate(token: String, limit: Int = 60): AiSession? {
        val session = sessions.findByToken(token) ?: return null
        if (!rateLimiter.check(token, limit)) return null
        return session
    }
}
