package dev.deathlegion.dazki

import dev.deathlegion.dazki.net.RateLimiter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    @Test
    fun `under limit returns true`() {
        val limiter = RateLimiter(defaultLimit = 5)
        repeat(5) { assertTrue(limiter.check("token1")) }
    }

    @Test
    fun `over limit returns false`() {
        val limiter = RateLimiter(defaultLimit = 5)
        repeat(5) { limiter.check("token1") }
        assertFalse(limiter.check("token1"))
    }

    @Test
    fun `different tokens are independent`() {
        val limiter = RateLimiter(defaultLimit = 5)
        repeat(5) { limiter.check("token1") }
        assertTrue(limiter.check("token2"))
    }
}
