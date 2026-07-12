package dev.deathlegion.dazki.ai

/**
 * Claude connector. Translates the AI's natural language prompt into
 * dazki RPC calls. The translation layer is intentionally simple:
 * we look for verbs and package names in the prompt, map them to RPC
 * methods, run them, and write the results back as a short reply.
 *
 * The connector does not call Claude itself. The user's AI host
 * (Claude, ChatGPT, etc.) does that. The connector only bridges the
 * AI's text output to the device.
 */
class ClaudeConnector(
    private val rpc: RpcClient,
) : AiConnector {

    override val displayName: String = "Claude"

    override suspend fun converse(
        prompt: String,
        session: DeviceSession,
        history: List<ChatTurn>,
    ): String {
        val lowered = prompt.lowercase()
        return when {
            "list packages" in lowered || "list apps" in lowered -> {
                if ("packages.read" !in session.scopes) return "scope denied: packages.read"
                val result = rpc.call(session, "packages.list", mapOf("flags" to 0))
                val packages = (result["packages"] as? List<*>) ?: emptyList()
                "Found ${packages.size} packages on ${session.deviceSerial}."
            }

            "force stop" in lowered -> {
                if ("packages.write" !in session.scopes) return "scope denied: packages.write"
                val pkg = extractPackageName(prompt) ?: return "which package do you want to stop?"
                rpc.call(session, "packages.forceStop", mapOf("pkg" to pkg))
                "Stopped $pkg."
            }

            "logcat" in lowered -> {
                if ("logcat.read" !in session.scopes) return "scope denied: logcat.read"
                val result = rpc.call(session, "logcat.dump", mapOf("lines" to 200))
                val text = (result["text"] as? String) ?: ""
                "Last 200 logcat lines:\n$text"
            }

            "ping" in lowered -> {
                val result = rpc.call(session, "session.ping", emptyMap())
                "device ${session.deviceSerial}: $result"
            }

            else -> "I can list packages, force stop, dump logcat, or ping. Ask again."
        }
    }

    /** Pulls the first token that looks like a Java package name from the prompt. */
    private fun extractPackageName(prompt: String): String? {
        val regex = Regex("\\b[a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)+\\b")
        return regex.find(prompt)?.value
    }
}
