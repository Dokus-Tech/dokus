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
}