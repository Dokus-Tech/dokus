package tech.dokus.app.infrastructure

internal expect object ShareExtensionServerConfigBridge {
    fun mirrorServerBaseUrl(baseUrl: String)
}
