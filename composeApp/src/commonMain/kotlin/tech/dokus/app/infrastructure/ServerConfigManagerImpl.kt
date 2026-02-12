package tech.dokus.app.infrastructure

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerConfigManager

/**
 * Implementation of [ServerConfigManager] that persists configuration using Settings.
 *
 * Uses the multiplatform-settings library to store server configuration locally.
 * Configuration is loaded on [initialize] and updated on [setServer].
 */
class ServerConfigManagerImpl(
    private val settings: Settings = Settings()
) : ServerConfigManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentServer = MutableStateFlow(loadPersistedServerConfig() ?: ServerConfig.Cloud)
    override val currentServer: StateFlow<ServerConfig> = _currentServer.asStateFlow()

    override val isCloudServer: StateFlow<Boolean> = _currentServer
        .map { it.isCloud }
        .stateIn(scope, SharingStarted.Eagerly, _currentServer.value.isCloud)

    init {
        mirrorToShareExtension(_currentServer.value)
    }

    override suspend fun initialize() {
        _currentServer.value = loadPersistedServerConfig() ?: ServerConfig.Cloud
        mirrorToShareExtension(_currentServer.value)
    }

    override suspend fun setServer(config: ServerConfig) {
        // Persist to settings
        settings[KEY_SERVER_HOST] = config.host
        settings[KEY_SERVER_PORT] = config.port
        settings[KEY_SERVER_PROTOCOL] = config.protocol
        settings[KEY_SERVER_NAME] = config.name
        settings[KEY_SERVER_VERSION] = config.version
        settings[KEY_SERVER_IS_CLOUD] = config.isCloud

        // Update state
        _currentServer.value = config
        mirrorToShareExtension(config)
    }

    override suspend fun resetToCloud() {
        setServer(ServerConfig.Cloud)
    }

    private fun loadPersistedServerConfig(): ServerConfig? {
        val savedHost = settings.get<String>(KEY_SERVER_HOST)
        val savedPort = settings.get<Int>(KEY_SERVER_PORT)
        val savedProtocol = settings.get<String>(KEY_SERVER_PROTOCOL)
        val savedName = settings.get<String>(KEY_SERVER_NAME)
        val savedVersion = settings.get<String>(KEY_SERVER_VERSION)
        val isCloud = settings.get<Boolean>(KEY_SERVER_IS_CLOUD) ?: true

        return if (savedHost != null && savedPort != null && savedProtocol != null) {
            ServerConfig(
                host = savedHost,
                port = savedPort,
                protocol = savedProtocol,
                name = savedName,
                version = savedVersion,
                isCloud = isCloud
            )
        } else {
            null
        }
    }

    companion object {
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_SERVER_PORT = "server_port"
        private const val KEY_SERVER_PROTOCOL = "server_protocol"
        private const val KEY_SERVER_NAME = "server_name"
        private const val KEY_SERVER_VERSION = "server_version"
        private const val KEY_SERVER_IS_CLOUD = "server_is_cloud"
    }

    private fun mirrorToShareExtension(config: ServerConfig) {
        runCatching {
            ShareExtensionServerConfigBridge.mirrorServerBaseUrl(config.baseUrl)
        }
    }
}
