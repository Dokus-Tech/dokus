package ai.dokus.features.auth.domain.model

import ai.dokus.foundation.domain.DeviceType
import ai.dokus.foundation.domain.current
import kotlinx.serialization.Serializable

@Serializable
data class QrLoginInitRequest(
    val deviceType: DeviceType = DeviceType.current
)

@Serializable
data class QrLoginScanRequest(
    val token: String,
    val deviceType: DeviceType = DeviceType.current,
)

@Serializable
data class QrLoginDecisionRequest(
    val approved: Boolean
)