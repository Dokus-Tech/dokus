package ai.dokus.foundation.domain.model.common

enum class Feature(val serviceName: String, val fullPackageName: String, val poolName: String) {
    Auth("auth", "ai.dokus..auth", "dokus-auth"),
    Expense("expense", "ai.dokus.expense", "dokus-expense"),
    Invoicing("invocing", "ai.dokus.invoicing", "dokus-invoicing"),
    Payment("payment", "ai.dokus.payment", "dokus-payment"),
    Reporting("reporting", "ai.dokus.reporting", "dokus-reporting"),
}