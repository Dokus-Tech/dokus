package ai.dokus.foundation.domain.model.common

enum class Feature(
    val serviceName: String,
    val fullPackageName: String,
    val poolName: String,
    val frontendDbName: String
) {
    Auth(
        serviceName = "auth",
        fullPackageName = "ai.dokus.auth",
        poolName = "dokus-auth",
        frontendDbName = "dokus_auth"
    ),
    Cashflow(
        serviceName = "cashflow",
        fullPackageName = "ai.dokus.cashflow",
        poolName = "dokus-cashflow",
        frontendDbName = "dokus_cashflow"
    ),
    Expense(
        serviceName = "expense",
        fullPackageName = "ai.dokus.expense",
        poolName = "dokus-expense",
        frontendDbName = "dokus_expense"
    ),
    Invoicing(
        serviceName = "invoicing",
        fullPackageName = "ai.dokus.invoicing",
        poolName = "dokus-invoicing",
        frontendDbName = "dokus_invoicing"
    ),
    Payment(
        serviceName = "payment",
        fullPackageName = "ai.dokus.payment",
        poolName = "dokus-payment",
        frontendDbName = "dokus_payment"
    ),
    Reporting(
        serviceName = "reporting",
        fullPackageName = "ai.dokus.reporting",
        poolName = "dokus-reporting",
        frontendDbName = "dokus_reporting"
    ),
    Media(
        serviceName = "media",
        fullPackageName = "ai.dokus.media",
        poolName = "dokus-media",
        frontendDbName = "dokus_media"
    ),
}
