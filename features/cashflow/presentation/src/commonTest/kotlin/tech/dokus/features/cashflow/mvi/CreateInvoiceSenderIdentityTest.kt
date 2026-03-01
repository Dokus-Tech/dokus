package tech.dokus.features.cashflow.mvi

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
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateInvoiceSenderIdentityTest {

    @Test
    fun `uses tenant legal name when settings company name is missing`() {
        val tenant = createTenant(legalName = "INVOID VISION", vatNumber = "BE0777887045")

        val (senderName, senderVat) = resolveSenderIdentity(
            settingsCompanyName = null,
            currentTenant = tenant,
            existingCompanyName = "",
            existingCompanyVat = null
        )

        assertEquals("INVOID VISION", senderName)
        assertEquals("BE0777.887.045", senderVat)
    }

    @Test
    fun `uses settings company name over tenant legal name`() {
        val tenant = createTenant(legalName = "INVOID VISION", vatNumber = "BE0777887045")

        val (senderName, senderVat) = resolveSenderIdentity(
            settingsCompanyName = "Invoid Ops",
            currentTenant = tenant,
            existingCompanyName = "Old Name",
            existingCompanyVat = "BE0000.000.000"
        )

        assertEquals("Invoid Ops", senderName)
        assertEquals("BE0777.887.045", senderVat)
    }

    @Test
    fun `keeps existing values when neither settings nor tenant provides identity`() {
        val (senderName, senderVat) = resolveSenderIdentity(
            settingsCompanyName = "   ",
            currentTenant = null,
            existingCompanyName = "Existing Tenant Name",
            existingCompanyVat = "BE0888.111.222"
        )

        assertEquals("Existing Tenant Name", senderName)
        assertEquals("BE0888.111.222", senderVat)
    }

    private fun createTenant(
        legalName: String,
        vatNumber: String
    ): Tenant {
        val timestamp = LocalDateTime(2026, 1, 1, 0, 0)
        return Tenant(
            id = TenantId.generate(),
            type = TenantType.Company,
            legalName = LegalName(legalName),
            displayName = DisplayName(legalName),
            subscription = SubscriptionTier.Core,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = VatNumber(vatNumber),
            createdAt = timestamp,
            updatedAt = timestamp
        )
    }
}
