package tech.dokus.foundation.app.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import org.koin.compose.koinInject
import tech.dokus.domain.config.DynamicDokusEndpointProvider

/**
 * Resolves server-relative API paths (e.g. "/api/v1/...") to absolute URLs
 * using the currently selected Dokus endpoint.
 */
@Composable
fun rememberResolvedApiUrl(url: String?): String? {
    if (LocalInspectionMode.current) return url
    val endpointProvider = koinInject<DynamicDokusEndpointProvider>()
    val endpoint = endpointProvider.currentEndpointSnapshot()
    return remember(url, endpoint.host, endpoint.port, endpoint.protocol) {
        resolveApiUrl(url, endpoint.host, endpoint.port, endpoint.protocol)
    }
}

internal fun resolveApiUrl(
    url: String?,
    host: String,
    port: Int,
    protocol: String,
): String? {
    if (url.isNullOrBlank()) return null
    val trimmed = url.trim()
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed

    val normalizedPath = if (trimmed.startsWith("/")) trimmed else "/$trimmed"
    val useDefaultPort = (protocol == "https" && port == 443) || (protocol == "http" && port == 80)
    val portSuffix = if (useDefaultPort) "" else ":$port"

    return "$protocol://$host$portSuffix$normalizedPath"
}
