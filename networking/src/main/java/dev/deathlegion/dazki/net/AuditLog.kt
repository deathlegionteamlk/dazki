package dev.deathlegion.dazki.net

import java.io.File
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Append-only audit log of every RPC call. Lines are written to
 * /data/local/tmp/dazki-audit.jsonl as JSON objects, one per line.
 * The log is capped at 10 MiB. Older entries roll off when the cap
 * is reached.
 *
 * The AI host can read the log through the audit.dump RPC method.
 * Lines are never overwritten in place, only appended.
 */
class AuditLog(private val file: File, private val maxBytes: Long = 10L * 1024 * 1024) {

    private val queue = ConcurrentLinkedQueue<String>()

    @Synchronized
    fun record(entry: AuditEntry) {
        val line = entry.toJson()
        queue.add(line)
        file.appendText(line + "\n")
        if (file.length() > maxBytes) {
            rotate()
        }
    }

    fun dump(sinceHours: Int = 24): List<AuditEntry> {
        if (!file.exists()) return emptyList()
        val cutoff = Instant.now().minusSeconds(sinceHours * 3600L).toEpochMilli()
        return file.readLines()
            .mapNotNull { AuditEntry.fromJson(it) }
            .filter { it.tsMs >= cutoff }
    }

    private fun rotate() {
        val keep = file.readLines().takeLast(50_000)
        file.writeText(keep.joinToString("\n", postfix = "\n"))
    }
}

data class AuditEntry(
    val tsMs: Long,
    val token: String,
    val method: String,
    val args: Map<String, Any?>,
    val ok: Boolean,
    val latencyMs: Int,
    val error: String? = null,
) {
    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{\"ts\":\"").append(Instant.ofEpochMilli(tsMs)).append('"')
        sb.append(",\"token\":\"").append(token.take(8)).append("...\"")
        sb.append(",\"method\":\"").append(method).append('"')
        sb.append(",\"args\":").append(argsToJson(args))
        sb.append(",\"ok\":").append(ok)
        sb.append(",\"latency_ms\":").append(latencyMs)
        if (error != null) sb.append(",\"error\":\"").append(error.replace("\"", "\\\"")).append('"')
        sb.append('}')
        return sb.toString()
    }

    private fun argsToJson(args: Map<String, Any?>): String {
        if (args.isEmpty()) return "{}"
        val sb = StringBuilder("{")
        args.entries.forEachIndexed { i, e ->
            if (i > 0) sb.append(',')
            sb.append('"').append(e.key).append("\":")
            val v = e.value
            when (v) {
                is String -> sb.append('"').append(v.replace("\"", "\\\"")).append('"')
                is Number, is Boolean -> sb.append(v)
                null -> sb.append("null")
                else -> sb.append('"').append(v.toString().replace("\"", "\\\"")).append('"')
            }
        }
        sb.append('}')
        return sb.toString()
    }

    companion object {
        fun fromJson(line: String): AuditEntry? {
            return try {
                // Tiny parser: not a real JSON parser, but the log
                // format is fixed. Falls back to a stub entry when
                // the line is malformed.
                val ts = Regex("\"ts\":\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: return null
                val token = Regex("\"token\":\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: ""
                val method = Regex("\"method\":\"([^\"]+)\"").find(line)?.groupValues?.get(1) ?: ""
                val ok = Regex("\"ok\":(true|false)").find(line)?.groupValues?.get(1)?.toBoolean() ?: false
                val latency = Regex("\"latency_ms\":(\\d+)").find(line)?.groupValues?.get(1)?.toInt() ?: 0
                AuditEntry(
                    tsMs = Instant.parse(ts).toEpochMilli(),
                    token = token,
                    method = method,
                    args = emptyMap(),
                    ok = ok,
                    latencyMs = latency,
                )
            } catch (_: Throwable) {
                null
            }
        }
    }
}
