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

    data object Cashflow : DokusEndpoint(
        host = BuildKonfig.cashflowHost,
        port = BuildKonfig.cashflowPort,
        internalHost = BuildKonfig.cashflowInternalHost,
        internalPort = BuildKonfig.cashflowInternalPort
    )

    data object Payment : DokusEndpoint(
        host = BuildKonfig.paymentHost,
        port = BuildKonfig.paymentPort,
        internalHost = BuildKonfig.paymentInternalHost,
        internalPort = BuildKonfig.paymentInternalPort
    )

    data object Banking : DokusEndpoint(
        host = BuildKonfig.bankingHost,
        port = BuildKonfig.bankingPort,
        internalHost = BuildKonfig.bankingInternalHost,
        internalPort = BuildKonfig.bankingInternalPort
    )

    data object Media : DokusEndpoint(
        host = BuildKonfig.mediaHost,
        port = BuildKonfig.mediaPort,
        internalHost = BuildKonfig.mediaInternalHost,
        internalPort = BuildKonfig.mediaInternalPort
    )
}
