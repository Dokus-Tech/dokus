package ai.dokus.foundation.ktor.config

import com.typesafe.config.Config

data class StorageConfig(
    val type: String,
    val directory: String
) {
    companion object {
        fun fromConfig(config: Config): StorageConfig {
            val storage = config.getConfig("storage")
            return StorageConfig(
                type = config.getString("type"),
                directory = config.getString("directory")
            )
        }

        fun load(baseConfig: AppBaseConfig): StorageConfig {
            return fromConfig(baseConfig.config.getConfig("storage"))
        }
    }
}