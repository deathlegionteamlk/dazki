package dev.deathlegion.dazki.permission

/**
 * In-memory grant store with persistence hook. Used by the server to
 * answer isAllowed() checks and by the manager UI to show the current
 * grants.
 *
 * The store keeps two indexes: by principal and by capability, so
 * lookups in either direction are O(1).
 */
class GrantStore(
    private val onPersist: (List<Grant>) -> Unit,
) {
    private val byPrincipal = mutableMapOf<Principal, MutableSet<Grant>>()
    private val byCapability = mutableMapOf<Capability, MutableSet<Grant>>()

    @Synchronized
    fun add(grant: Grant) {
        byPrincipal.getOrPut(grant.principal) { mutableSetOf() }.add(grant)
        byCapability.getOrPut(grant.capability) { mutableSetOf() }.add(grant)
        persist()
    }

    @Synchronized
    fun revoke(principal: Principal, capability: Capability) {
        val set = byPrincipal[principal] ?: return
        val toRemove = set.filter { it.capability == capability }
        set.removeAll(toRemove)
        byCapability[capability]?.removeAll(toRemove)
        persist()
    }

    @Synchronized
    fun revokeAll(principal: Principal) {
        val set = byPrincipal.remove(principal) ?: return
        for (grant in set) {
            byCapability[grant.capability]?.remove(grant)
        }
        persist()
    }

    @Synchronized
    fun grantsFor(principal: Principal): Set<Grant> {
        return byPrincipal[principal]?.toSet() ?: emptySet()
    }

    @Synchronized
    fun has(principal: Principal, capability: Capability): Boolean {
        val grants = byPrincipal[principal] ?: return false
        val now = System.currentTimeMillis()
        return grants.any { g ->
            (g.expiresAtMs == 0L || g.expiresAtMs > now) &&
                (g.capability == capability || g.capability.implies(capability) || g.capability == Capabilities.ALL)
        }
    }

    @Synchronized
    fun all(): List<Grant> = byPrincipal.values.flatten().toList()

    @Synchronized
    fun load(grants: List<Grant>) {
        byPrincipal.clear()
        byCapability.clear()
        for (g in grants) {
            byPrincipal.getOrPut(g.principal) { mutableSetOf() }.add(g)
            byCapability.getOrPut(g.capability) { mutableSetOf() }.add(g)
        }
    }

    private fun persist() = onPersist(all())
}
