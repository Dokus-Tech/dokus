package tech.dokus.domain.model.common

enum class Feature(
    val serviceName: String,
    val fullPackageName: String,
    val poolName: String,
    val frontendDbName: String
) {
    Auth(
        serviceName = "auth",
        fullPackageName = "tech.dokus.features.auth",
        poolName = "dokus-auth",
        frontendDbName = "dokus_auth"
    ),
    Cashflow(
        serviceName = "cashflow",
        fullPackageName = "tech.dokus.features.cashflow",
        poolName = "dokus-cashflow",
        frontendDbName = "dokus_cashflow"
    ),
    Expense(
        serviceName = "expense",
        fullPackageName = "tech.dokus.features.expense",
        poolName = "dokus-expense",
        frontendDbName = "dokus_expense"
    ),
    Invoicing(
        serviceName = "invoicing",
        fullPackageName = "tech.dokus.features.invoicing",
        poolName = "dokus-invoicing",
        frontendDbName = "dokus_invoicing"
    ),
    Payment(
        serviceName = "payment",
        fullPackageName = "tech.dokus.features.payment",
        poolName = "dokus-payment",
        frontendDbName = "dokus_payment"
    ),
    Reporting(
        serviceName = "reporting",
        fullPackageName = "tech.dokus.features.reporting",
        poolName = "dokus-reporting",
        frontendDbName = "dokus_reporting"
    ),
    Media(
        serviceName = "media",
        fullPackageName = "tech.dokus.features.media",
        poolName = "dokus-media",
        frontendDbName = "dokus_media"
    ),
    Contacts(
        serviceName = "contacts",
        fullPackageName = "tech.dokus.features.contacts",
        poolName = "dokus-contacts",
        frontendDbName = "dokus_contacts"
    ),
    Navigation(
        serviceName = "navigation",
        fullPackageName = "tech.dokus.foundation.navigation",
        poolName = "dokus-navigation",
        frontendDbName = "dokus_navigation"
    ),
}
