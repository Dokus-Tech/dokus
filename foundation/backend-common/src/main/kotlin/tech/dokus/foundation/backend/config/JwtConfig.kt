package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

data class JwtConfig(
    val issuer: String,
    val audience: String,
    val realm: String,
    val secret: String,
    val publicKeyPath: String?,
    val privateKeyPath: String?,
    val algorithm: String
) {
    companion object {
        fun fromConfig(config: Config): JwtConfig {
            return JwtConfig(
                issuer = config.getString("issuer"),
                audience = config.getString("audience"),
                realm = config.getString("realm"),
                secret = config.getString("secret"),
                publicKeyPath = if (config.hasPath("publicKeyPath")) config.getString("publicKeyPath") else null,
                privateKeyPath = if (config.hasPath("privateKeyPath")) config.getString("privateKeyPath") else null,
                algorithm = if (config.hasPath("algorithm")) config.getString("algorithm") else "HS256"
            )
        }
    }
}