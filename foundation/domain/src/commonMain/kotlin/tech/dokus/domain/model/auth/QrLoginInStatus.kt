package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable
import tech.dokus.domain.DeviceType
import tech.dokus.domain.current

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
