package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * MinIO object storage configuration.
 */
data class MinioConfig(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String
) {
    companion object {
        fun fromConfig(config: Config): MinioConfig {
            return MinioConfig(
                endpoint = config.getString("endpoint"),
                accessKey = config.getString("accessKey"),
                secretKey = config.getString("secretKey"),
                bucket = config.getString("bucket")
            )
        }

        /**
         * Try to load MinIO config from the app config.
         * Returns null if minio section is not present.
         */
        fun loadOrNull(config: Config): MinioConfig? {
            return if (config.hasPath("minio")) {
                fromConfig(config.getConfig("minio"))
            } else {
                null
            }
        }
    }
}
