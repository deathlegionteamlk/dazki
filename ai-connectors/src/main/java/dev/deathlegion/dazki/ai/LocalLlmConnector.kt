package dev.deathlegion.dazki.ai

/**
 * Bridge to a local llama.cpp server. Useful when the user wants to
 * run the AI model on the phone itself instead of calling a hosted
 * API. The connector shells out to the llama.cpp binary through the
 * dazki shell.exec scope.
 */
class LocalLlmConnector(
    private val rpc: RpcClient,
) : AiConnector {

    override val displayName: String = "Local LLM (llama.cpp)"

    override suspend fun converse(
        prompt: String,
        session: DeviceSession,
        history: List<ChatTurn>,
    ): String {
        if ("shell.exec" !in session.scopes) return "scope denied: shell.exec"
        val escaped = prompt.replace("\"", "\\\"")
        val result = rpc.call(
            session, "shell.exec",
            mapOf("cmd" to "llama-cli -m /data/local/tmp/model.gguf -p \"$escaped\" --no-display-prompt -n 256")
        )
        val stdout = (result["stdout"] as? String) ?: ""
        return stdout.trim().ifEmpty { "local LLM returned nothing" }
    }
}
