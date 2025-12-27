package tech.dokus.domain.config

/**
 * Represents a Dokus service endpoint.
 *
 * With Traefik gateway, external clients connect to a single gateway host/port,
 * and the path prefix determines which service handles the request.
 * Internal service-to-service calls still use direct host/port within Docker network.
 *
 * @property pathPrefix The path prefix for this service (e.g., "/api/v1/identity")
 * @property internalHost Hostname for inter-service calls within Docker network
 * @property internalPort Port for inter-service calls within Docker network
 */
sealed class DokusEndpoint(
    val pathPrefix: String,
    val internalHost: String,
    val internalPort: Int,
) {
    /** Gateway host for external connections (from BuildKonfig) */
    val gatewayHost: String get() = BuildKonfig.gatewayHost

    /** Gateway port for external connections (from BuildKonfig) */
    val gatewayPort: Int get() = BuildKonfig.gatewayPort

    /** Gateway protocol (http or https) from BuildKonfig */
    val gatewayProtocol: String get() = BuildKonfig.gatewayProtocol

    data object Auth : DokusEndpoint(
        pathPrefix = "/api/v1",
        internalHost = BuildKonfig.authInternalHost,
        internalPort = BuildKonfig.authInternalPort
    )

    data object Cashflow : DokusEndpoint(
        pathPrefix = "/api/v1",
        internalHost = BuildKonfig.cashflowInternalHost,
        internalPort = BuildKonfig.cashflowInternalPort
    )

    data object Payment : DokusEndpoint(
        pathPrefix = "/api/v1",
        internalHost = BuildKonfig.paymentInternalHost,
        internalPort = BuildKonfig.paymentInternalPort
    )

    data object Banking : DokusEndpoint(
        pathPrefix = "/api/v1",
        internalHost = BuildKonfig.bankingInternalHost,
        internalPort = BuildKonfig.bankingInternalPort
    )

    data object Media : DokusEndpoint(
        pathPrefix = "/api/v1",
        internalHost = BuildKonfig.mediaInternalHost,
        internalPort = BuildKonfig.mediaInternalPort
    )
}
