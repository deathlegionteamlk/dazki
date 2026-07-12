package dev.deathlegion.dazki.ai

/** Gemini connector. Same shape as the others. */
class GeminiConnector(
    private val rpc: RpcClient,
) : AiConnector {

    override val displayName: String = "Gemini"

    override suspend fun converse(
        prompt: String,
        session: DeviceSession,
        history: List<ChatTurn>,
    ): String {
        return ClaudeConnector(rpc).converse(prompt, session, history)
    }
}
