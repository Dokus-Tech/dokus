package tech.dokus.app.infrastructure

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
internal expect object ShareExtensionServerConfigBridge {
    fun mirrorServerBaseUrl(baseUrl: String)
}
