package tech.dokus.features.ai.graph

import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.IntelligenceMode

object TestAiFixtures {
    val aiConfig = AIConfig(
        mode = IntelligenceMode.Sovereign,
        ollamaHost = "",
        lmStudioHost = "http://192.168.0.150:1234"
    )

    val tenant = Tenant(
        id = TenantId.generate(),
        type = TenantType.Company,
        legalName = LegalName("Invoid Vision B.V."),
        displayName = DisplayName("Invoid Vision"),
        subscription = SubscriptionTier.CoreFounder,
        status = TenantStatus.Active,
        language = Language.En,
        vatNumber = VatNumber(""),
        createdAt = LocalDateTime(2024, 1, 1, 10, 0),
        updatedAt = LocalDateTime(2024, 1, 1, 10, 0),
    )
}
