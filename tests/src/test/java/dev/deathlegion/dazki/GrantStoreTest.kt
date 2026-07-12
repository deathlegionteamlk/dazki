package dev.deathlegion.dazki

import dev.deathlegion.dazki.permission.Capabilities
import dev.deathlegion.dazki.permission.Capability
import dev.deathlegion.dazki.permission.Grant
import dev.deathlegion.dazki.permission.GrantStore
import dev.deathlegion.dazki.permission.Principal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the permission system. Run with
 * `./gradlew :permission-system:test`.
 *
 * (Not yet wired into the build because the permission-system
 * module is not in settings.gradle.kts. The tests are here as a
 * reference for when we wire it in.)
 */
class GrantStoreTest {

    @Test
    fun `grant then has returns true`() {
        val store = GrantStore(onPersist = {})
        val principal = Principal.App(uid = 10001, packageName = "com.example")
        val cap = Capabilities.PACKAGES_READ
        store.add(Grant(principal, cap, System.currentTimeMillis(), "test"))
        assertTrue(store.has(principal, cap))
    }

    @Test
    fun `no grant returns false`() {
        val store = GrantStore(onPersist = {})
        val principal = Principal.App(uid = 10001, packageName = "com.example")
        assertFalse(store.has(principal, Capabilities.PACKAGES_READ))
    }

    @Test
    fun `revoke removes the grant`() {
        val store = GrantStore(onPersist = {})
        val principal = Principal.App(uid = 10001, packageName = "com.example")
        val cap = Capabilities.PACKAGES_READ
        store.add(Grant(principal, cap, System.currentTimeMillis(), "test"))
        store.revoke(principal, cap)
        assertFalse(store.has(principal, cap))
    }

    @Test
    fun `wildcard capability implies everything`() {
        val cap = Capability("*")
        assertTrue(cap.implies(Capabilities.PACKAGES_READ))
        assertTrue(cap.implies(Capabilities.SHELL_EXEC))
    }

    @Test
    fun `prefix capability implies children`() {
        val cap = Capability("packages.*")
        assertTrue(cap.implies(Capabilities.PACKAGES_READ))
        assertTrue(cap.implies(Capabilities.PACKAGES_WRITE))
    }

    @Test
    fun `distinct capability does not imply another`() {
        val cap = Capabilities.PACKAGES_READ
        assertFalse(cap.implies(Capabilities.PACKAGES_WRITE))
    }

    @Test
    fun `revokeAll clears all grants for principal`() {
        val store = GrantStore(onPersist = {})
        val principal = Principal.App(uid = 10001, packageName = "com.example")
        store.add(Grant(principal, Capabilities.PACKAGES_READ, System.currentTimeMillis(), "test"))
        store.add(Grant(principal, Capabilities.SETTINGS_GLOBAL_READ, System.currentTimeMillis(), "test"))
        store.revokeAll(principal)
        assertEquals(0, store.grantsFor(principal).size)
    }
}
