package dev.deathlegion.dazki.ai

/**
 * Connector interface that every AI provider implements. A connector
 * turns a high level intent (from the user's AI assistant) into a
 * sequence of dazki RPC calls, and turns the RPC results back into a
 * natural language reply for the AI.
 *
 * The manager app loads exactly one connector at a time based on the
 * user's choice in Settings. The default is the Claude connector.
 */
interface AiConnector {

    /** Display name shown in the manager UI. */
    val displayName: String

    /**
     * Takes a natural language prompt plus a device session and
     * returns a reply the AI can show the user. Throws on transport
     * errors. Returns an error string inside the reply when the
     * device rejects the call (bad scope, rate limited, etc).
     */
    suspend fun converse(
        prompt: String,
        session: DeviceSession,
        history: List<ChatTurn>,
    ): String
}

/** One turn of conversation. role is "user" or "assistant". */
data class ChatTurn(val role: String, val content: String)

/** Active device session for an AI conversation. */
data class DeviceSession(
    val deviceSerial: String,
    val token: String,
    val scopes: Set<String>,
    val rpcEndpoint: String,
)
