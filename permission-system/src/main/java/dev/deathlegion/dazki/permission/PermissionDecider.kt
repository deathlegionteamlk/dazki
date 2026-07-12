package dev.deathlegion.dazki.permission

/**
 * Decides whether a principal may perform an action that requires a
 * capability. The server consults the decider before every privileged
 * call.
 *
 * The decider is a thin wrapper around GrantStore so the policy can
 * change without touching every callsite. Future rules (time of day,
 * per-app quotas, device lock state) can land here.
 */
class PermissionDecider(private val grants: GrantStore) {

    fun can(principal: Principal, capability: Capability): Boolean {
        return grants.has(principal, capability)
    }

    fun explain(principal: Principal, capability: Capability): String {
        if (can(principal, capability)) return "allowed"
        val existing = grants.grantsFor(principal)
        if (existing.isEmpty()) {
            return "principal has no grants"
        }
        val caps = existing.joinToString(",") { it.capability.name }
        return "principal has [$caps] but needs [${capability.name}]"
    }
}
