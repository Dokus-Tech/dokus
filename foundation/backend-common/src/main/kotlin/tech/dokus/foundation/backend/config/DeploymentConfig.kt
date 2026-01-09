package tech.dokus.foundation.backend.config

import com.typesafe.config.Config

/**
 * Deployment hosting mode - determines how the application is deployed.
 * This is a DEPLOYMENT-level setting, not per-tenant.
 *
 * Cloud vs Self-Hosted affects multiple features:
 * - Credential management (managed vs user-provided)
 * - Feature availability
 * - Support flows
 */
enum class HostingMode {
    /** Cloud deployment - Dokus manages infrastructure and credentials */
    Cloud,
    /** Self-hosted deployment - Users manage their own infrastructure */
    SelfHosted;

    companion object {
        fun fromString(value: String): HostingMode = when (value.lowercase()) {
            "cloud" -> Cloud
            "self-hosted", "selfhosted" -> SelfHosted
            else -> SelfHosted // Default to self-hosted for safety
        }
    }
}

/**
 * Deployment-level configuration.
 * Contains settings that apply to the entire deployment, not per-tenant.
 */
data class DeploymentConfig(
    /** Hosting mode (cloud or self-hosted) */
    val hostingMode: HostingMode
) {
    /** Returns true if this is a cloud deployment */
    val isCloud: Boolean get() = hostingMode == HostingMode.Cloud

    /** Returns true if this is a self-hosted deployment */
    val isSelfHosted: Boolean get() = hostingMode == HostingMode.SelfHosted

    companion object {
        /**
         * Create config from HOCON configuration.
         * Expected path: deployment.*
         */
        fun fromConfig(config: Config): DeploymentConfig {
            val deploymentConfig = config.getConfig("deployment")
            return DeploymentConfig(
                hostingMode = HostingMode.fromString(deploymentConfig.getString("hostingMode"))
            )
        }
    }
}
