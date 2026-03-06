package tech.dokus.features.ai.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.DocumentPurposeSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.PurposePeriodMode
import tech.dokus.domain.ids.TenantId

@Serializable
data class PurposeEnrichmentInput(
    val tenantId: TenantId,
    val documentType: DocumentType,
    val supplierDisplayName: String?,
    val issueDate: LocalDate?,
    val servicePeriodStart: LocalDate? = null,
    val templatePurposeBase: String? = null,
    val templatePeriodMode: PurposePeriodMode? = null,
    val ragPurposeBaseCandidates: List<String> = emptyList(),
    val fallbackPurposeBase: String? = null
)

@Serializable
data class PurposeEnrichmentResult(
    val purposeBase: String?,
    val purposePeriodYear: Int?,
    val purposePeriodMonth: Int?,
    val purposeRendered: String?,
    val purposeSource: DocumentPurposeSource?,
    val purposePeriodMode: PurposePeriodMode
)
