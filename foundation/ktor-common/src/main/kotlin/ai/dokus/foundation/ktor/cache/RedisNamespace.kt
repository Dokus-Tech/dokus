package ai.dokus.foundation.ktor.cache

/**
 * Enum for Redis namespaces to ensure type-safe namespace usage
 * Each namespace isolates keys to prevent conflicts between modules
 */
enum class RedisNamespace(val value: String) {
    Auth("auth"),
    Invoicing("invoicing"),
    Expense("expense"),
    Payment("payment"),
    Reporting("reporting"),
    Audit("audit"),
    Banking("banking");

    override fun toString(): String = value
}