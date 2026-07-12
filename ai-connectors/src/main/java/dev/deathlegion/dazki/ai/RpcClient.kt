package dev.deathlegion.dazki.ai

/**
 * Minimal RPC client the connectors use to talk to the dazki server.
 * The real implementation lives in the manager app and uses OkHttp
 * WebSocket against the localhost tunnel. This interface keeps the
 * ai-connectors module free of Android dependencies.
 */
interface RpcClient {
    suspend fun call(session: DeviceSession, method: String, args: Map<String, Any?>): Map<String, Any?>
}
