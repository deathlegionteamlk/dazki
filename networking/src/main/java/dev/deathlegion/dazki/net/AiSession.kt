package dev.deathlegion.dazki.net

import java.util.concurrent.atomic.AtomicLong

/**
 * One paired AI session. Stored in memory and persisted to disk by
 * the manager app. The token is the secret the AI host presents on
 * every RPC call.
 */
data class AiSession(
    val token: String,
    val deviceSerial: String,
    val pairingCode: String,
    val scopes: Set<String>,
    val createdAtMs: Long,
    val lastUsedMs: Long,
    val callCount: AtomicLong = AtomicLong(0L),
)
