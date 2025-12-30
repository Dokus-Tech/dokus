package tech.dokus.foundation.backend.database

/**
 * Simple thread-local tenant context so we can inject tenant_id into DB sessions
 * (e.g., for Postgres RLS via `SET LOCAL app.tenant_id`).
 *
 * Ktor pipelines or service layers should call [withTenantContext] to wrap
 * request handling; dbQuery picks it up automatically.
 */
object TenantContextHolder {
    private val threadLocalTenant = ThreadLocal<String?>()

    fun set(tenantId: String?) {
        threadLocalTenant.set(tenantId)
    }

    fun clear() {
        threadLocalTenant.remove()
    }

    fun currentTenantId(): String? = threadLocalTenant.get()
}

inline fun <T> withTenantContext(tenantId: String?, block: () -> T): T {
    TenantContextHolder.set(tenantId)
    return try {
        block()
    } finally {
        TenantContextHolder.clear()
    }
}
