package tech.dokus.domain.model

import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.BusinessProfileSubjectType

@Serializable
data class UpdateBusinessProfileRequest(
    val websiteUrl: String? = null,
    val businessSummary: String? = null,
    val businessActivities: List<String>? = null,
)

@Serializable
data class PinBusinessProfileFieldsRequest(
    val websitePinned: Boolean? = null,
    val summaryPinned: Boolean? = null,
    val activitiesPinned: Boolean? = null,
    val logoPinned: Boolean? = null,
)

@Serializable
data class BusinessProfileUpdateResponse(
    val subjectType: BusinessProfileSubjectType,
    val subjectId: String,
    val websitePinned: Boolean,
    val summaryPinned: Boolean,
    val activitiesPinned: Boolean,
    val logoPinned: Boolean,
)
