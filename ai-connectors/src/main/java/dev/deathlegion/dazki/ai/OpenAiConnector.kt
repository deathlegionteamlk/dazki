package dev.deathlegion.dazki.ai

/** OpenAI connector. Same shape as the Claude one. */
class OpenAiConnector(
    private val rpc: RpcClient,
) : AiConnector {

    override val displayName: String = "OpenAI"

    override suspend fun converse(
        prompt: String,
        session: DeviceSession,
        history: List<ChatTurn>,
    ): String {
        return ClaudeConnector(rpc).converse(prompt, session, history)
    }
}
