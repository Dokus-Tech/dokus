package tech.dokus.app.infrastructure

import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

private const val DefaultAppGroupIdentifier = "group.vision.invoid.dokus.share"
private const val ShareServerBaseUrlKey = "share.server.base_url"

internal actual object ShareExtensionServerConfigBridge {
    actual fun mirrorServerBaseUrl(baseUrl: String) {
        val trimmedBaseUrl = baseUrl.trim()
        if (trimmedBaseUrl.isEmpty()) return

        val appGroupIdentifier = NSBundle.mainBundle
            .objectForInfoDictionaryKey("DokusShareAppGroupIdentifier")
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DefaultAppGroupIdentifier

        val defaults = NSUserDefaults(suiteName = appGroupIdentifier)
        defaults.setObject(trimmedBaseUrl, forKey = ShareServerBaseUrlKey)
        defaults.synchronize()
    }
}
