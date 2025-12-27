package ai.dokus.foundation.domain.model.auth

import tech.dokus.domain.DeviceType
import tech.dokus.domain.current
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