package ai.dokus.auth.backend.database.repository

import java.util.*

/**
 * Thread-local storage for the current tenant ID
 * This ensures all database operations are properly scoped to a tenant
 */
object TenantContext {
    private val tenantIdHolder = ThreadLocal<UUID>()

    /**
     * Set the current tenant ID for this thread
     */
    fun setTenantId(tenantId: UUID) {
        tenantIdHolder.set(tenantId)
    }

    /**
     * Get the current tenant ID
     * @throws IllegalStateException if no tenant is set
     */
    fun getTenantId(): UUID {
        return tenantIdHolder.get() ?: throw IllegalStateException(
            "No tenant ID set in context. This is a security violation."
        )
    }

    /**
     * Get the current tenant ID or null
     */
    fun getTenantIdOrNull(): UUID? {
        return tenantIdHolder.get()
    }

    /**
     * Clear the tenant context (important for cleanup)
     */
    fun clear() {
        tenantIdHolder.remove()
    }

    /**
     * Execute a block with a specific tenant context
     */
    inline fun <T> withTenant(tenantId: UUID, block: () -> T): T {
        return try {
            setTenantId(tenantId)
            block()
        } finally {
            clear()
        }
    }
}