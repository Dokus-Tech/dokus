package ai.dokus.foundation.domain.config

sealed class DokusEndpoint(
    val host: String,
    val port: Int,
    val internalHost: String,
    val internalPort: Int,
) {
    data object Auth : DokusEndpoint(
        host = BuildKonfig.authHost,
        port = BuildKonfig.authPort,
        internalHost = BuildKonfig.authInternalHost,
        internalPort = BuildKonfig.authInternalPort
    )

    data object Invoicing : DokusEndpoint(
        host = BuildKonfig.invoicingHost,
        port = BuildKonfig.invoicingPort,
        internalHost = BuildKonfig.invoicingInternalHost,
        internalPort = BuildKonfig.invoicingInternalPort
    )

    data object Expense : DokusEndpoint(
        host = BuildKonfig.expenseHost,
        port = BuildKonfig.expensePort,
        internalHost = BuildKonfig.expenseInternalHost,
        internalPort = BuildKonfig.expenseInternalPort
    )

    data object Payment : DokusEndpoint(
        host = BuildKonfig.paymentHost,
        port = BuildKonfig.paymentPort,
        internalHost = BuildKonfig.paymentInternalHost,
        internalPort = BuildKonfig.paymentInternalPort
    )

    data object Reporting : DokusEndpoint(
        host = BuildKonfig.reportingHost,
        port = BuildKonfig.reportingPort,
        internalHost = BuildKonfig.reportingInternalHost,
        internalPort = BuildKonfig.reportingInternalPort
    )

    data object Audit : DokusEndpoint(
        host = BuildKonfig.auditHost,
        port = BuildKonfig.auditPort,
        internalHost = BuildKonfig.auditInternalHost,
        internalPort = BuildKonfig.auditInternalPort
    )

    data object Banking : DokusEndpoint(
        host = BuildKonfig.bankingHost,
        port = BuildKonfig.bankingPort,
        internalHost = BuildKonfig.bankingInternalHost,
        internalPort = BuildKonfig.bankingInternalPort
    )
}