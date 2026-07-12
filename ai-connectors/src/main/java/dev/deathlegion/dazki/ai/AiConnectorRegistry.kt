package dev.deathlegion.dazki.ai

/**
 * Looks up a connector by name. The manager app uses this factory to
 * switch connectors from Settings.
 */
object AiConnectorRegistry {
    fun create(name: String, rpc: RpcClient): AiConnector = when (name.lowercase()) {
        "claude", "anthropic" -> ClaudeConnector(rpc)
        "openai", "chatgpt" -> OpenAiConnector(rpc)
        "gemini" -> GeminiConnector(rpc)
        "local", "llama" -> LocalLlmConnector(rpc)
        else -> ClaudeConnector(rpc)
    }

    val available: List<String> = listOf("Claude", "OpenAI", "Gemini", "Local LLM (llama.cpp)")
}
