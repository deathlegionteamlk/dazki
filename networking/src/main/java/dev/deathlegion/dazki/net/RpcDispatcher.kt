package dev.deathlegion.dazki.net

import org.json.JSONObject
import java.time.Instant

/**
 * Routes an incoming RPC frame to the right handler. Validates the
 * token, checks the scope, runs the call, writes the audit entry,
 * and returns the JSON reply bytes.
 *
 * The dispatcher is given a HandlerRegistry at construction time.
 * Each method name maps to a suspend function that takes the
 * session and args and returns a result map. The dispatcher handles
 * all the cross-cutting concerns (auth, rate limit, audit, error
 * handling) so the handlers stay small.
 */
class RpcDispatcher(
    private val sessions: SessionStore,
    private val rateLimiter: RateLimiter,
    private val audit: AuditLog,
    private val handlers: Map<String, RpcHandler>,
) {

    fun dispatch(payload: ByteArray): String {
        val start = System.currentTimeMillis()
        val request = try {
            parseFrame(payload)
        } catch (e: Throwable) {
            return errorReply(0L, "malformed frame: ${e.message}")
        }

        val session = sessions.findByToken(request.token)
            ?: return errorReply(request.seq, "unknown token")

        if (!rateLimiter.check(request.token)) {
            return errorReply(request.seq, "rate limit exceeded")
        }

        val handler = handlers[request.method]
            ?: return errorReply(request.seq, "unknown method: ${request.method}")

        if (!handler.scopes.any { it in session.scopes }) {
            return errorReply(request.seq, "scope denied: ${handler.scopes.joinToString()}")
        }

        val reply = try {
            val result = handler.handle(session, request.args)
            RpcReply(ok = true, result = result, seq = request.seq)
        } catch (e: Throwable) {
            RpcReply(ok = false, error = e.message, seq = request.seq)
        }

        audit.record(
            AuditEntry(
                tsMs = System.currentTimeMillis(),
                token = request.token,
                method = request.method,
                args = request.args,
                ok = reply.ok,
                latencyMs = (System.currentTimeMillis() - start).toInt(),
                error = reply.error,
            )
        )

        return serializeReply(reply)
    }

    private fun parseFrame(payload: ByteArray): RpcFrame {
        val obj = JSONObject(String(payload, Charsets.UTF_8))
        return RpcFrame(
            token = obj.getString("token"),
            method = obj.getString("method"),
            args = obj.optJSONObject("args")?.toMap() ?: emptyMap(),
            seq = obj.optLong("seq"),
        )
    }

    private fun serializeReply(reply: RpcReply): String {
        val obj = JSONObject()
        obj.put("ok", reply.ok)
        obj.put("seq", reply.seq)
        if (reply.error != null) obj.put("error", reply.error)
        if (reply.result.isNotEmpty()) obj.put("result", JSONObject(reply.result))
        return obj.toString()
    }

    private fun errorReply(seq: Long, msg: String): String {
        return serializeReply(RpcReply(ok = false, error = msg, seq = seq))
    }
}

private fun JSONObject.toMap(): Map<String, Any?> {
    val out = mutableMapOf<String, Any?>()
    for (key in keys()) {
        out[key] = when (val v = get(key)) {
            is JSONObject -> v.toMap()
            else -> v
        }
    }
    return out.toMap()
}

/** One RPC handler. */
interface RpcHandler {
    val scopes: Set<String>
    fun handle(session: AiSession, args: Map<String, Any?>): Map<String, Any?>
}
