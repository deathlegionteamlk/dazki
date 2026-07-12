package dev.deathlegion.dazki.net

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Per-token sliding window rate limiter. Allows 60 calls per minute
 * by default. The manager app can raise the limit per session.
 *
 * Uses a simple minute bucket counter. Good enough for the volumes
 * an AI assistant produces. Replace with a real sliding window if
 * the AI ever bursts more than 1000 calls per minute.
 */
class RateLimiter(private val defaultLimit: Int = 60) {

    private data class Bucket(val count: AtomicInteger = AtomicInteger(0), var minute: Long = 0L)

    private val buckets = ConcurrentHashMap<String, Bucket>()

    fun check(token: String, limit: Int = defaultLimit): Boolean {
        val now = System.currentTimeMillis() / 60_000L
        val bucket = buckets.computeIfAbsent(token) { Bucket() }
        synchronized(bucket) {
            if (bucket.minute != now) {
                bucket.minute = now
                bucket.count.set(0)
            }
            return bucket.count.incrementAndGet() <= limit
        }
    }

    fun remaining(token: String, limit: Int = defaultLimit): Int {
        val now = System.currentTimeMillis() / 60_000L
        val bucket = buckets[token] ?: return limit
        synchronized(bucket) {
            if (bucket.minute != now) return limit
            return (limit - bucket.count.get()).coerceAtLeast(0)
        }
    }
}
