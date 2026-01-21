package tech.dokus.foundation.backend.cache

/**
 * Enum for Redis namespaces to ensure type-safe namespace usage
 * Each namespace isolates keys to prevent conflicts between modules
 */
enum class RedisNamespace(val value: String) {
    Auth("auth"),
    Ai("ai"),
    Invoicing("invoicing"),
    Expense("expense"),
    Payment("payment"),
    Reporting("reporting"),
    Audit("audit"),
    Banking("banking");

    override fun toString(): String = value
}
