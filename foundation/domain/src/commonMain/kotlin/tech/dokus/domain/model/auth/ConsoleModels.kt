package tech.dokus.domain.model.auth

import kotlinx.serialization.Serializable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber

@Serializable
data class ConsoleClientSummary(
    val tenantId: TenantId,
    val companyName: DisplayName,
    val vatNumber: VatNumber? = null
)
