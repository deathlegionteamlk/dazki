package dev.deathlegion.dazki

import dev.deathlegion.dazki.net.AuditEntry
import dev.deathlegion.dazki.net.AuditLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant

class AuditLogTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun `record writes a line to the file`() {
        val file = tempFolder.newFile("audit.jsonl")
        val log = AuditLog(file)
        log.record(
            AuditEntry(
                tsMs = Instant.now().toEpochMilli(),
                token = "abcdef0123456789",
                method = "packages.list",
                args = mapOf("flags" to 0),
                ok = true,
                latencyMs = 12,
            )
        )
        val lines = file.readLines()
        assertEquals(1, lines.size)
        assertTrue(lines[0].contains("\"method\":\"packages.list\""))
        assertTrue(lines[0].contains("\"ok\":true"))
    }

    @Test
    fun `dump filters by time`() {
        val file = tempFolder.newFile("audit.jsonl")
        val log = AuditLog(file)
        val now = Instant.now().toEpochMilli()
        // Record one old entry and one recent entry.
        log.record(AuditEntry(now - 2 * 3600 * 1000L, "old", "x", emptyMap(), true, 1))
        log.record(AuditEntry(now, "new", "y", emptyMap(), true, 1))
        val entries = log.dump(sinceHours = 1)
        assertEquals(1, entries.size)
        assertEquals("y", entries[0].method)
    }
}
