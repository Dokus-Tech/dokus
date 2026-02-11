package tech.dokus.backend.services.documents

import kotlinx.datetime.LocalDateTime
import org.junit.jupiter.api.Test
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraft
import tech.dokus.domain.model.Tenant
import kotlin.test.assertEquals
import kotlin.test.assertIs

class DocumentDirectionResolverTest {

    private val resolver = DocumentDirectionResolver()

    @Test
    fun `resolves inbound invoice when tenant vat is buyer vat`() {
        val tenantVat = VatNumber.from("BE0777887045")!!
        val tenant = testTenant(tenantVat)
        val draft = InvoiceDraftData(
            seller = PartyDraft(
                name = "Google Cloud EMEA Limited",
                vat = VatNumber.from("IE6388047V")
            ),
            buyer = PartyDraft(
                name = "INVOID VISION",
                vat = tenantVat
            ),
        )

        val normalized = resolver.normalize(
            classifiedType = DocumentType.Invoice,
            draftData = draft,
            tenant = tenant
        )

        val invoice = assertIs<InvoiceDraftData>(normalized.draftData)
        assertEquals(DocumentType.Invoice, normalized.documentType)
        assertEquals(DocumentDirection.Inbound, invoice.direction)
        assertEquals("INVOID VISION", invoice.customerName)
        assertEquals(tenantVat, invoice.customerVat)
    }

    @Test
    fun `resolves outbound invoice when tenant vat is seller vat`() {
        val tenantVat = VatNumber.from("BE0123456789")!!
        val tenant = testTenant(tenantVat)
        val draft = InvoiceDraftData(
            seller = PartyDraft(
                name = "INVOID VISION",
                vat = tenantVat
            ),
            buyer = PartyDraft(
                name = "Acme",
                vat = VatNumber.from("BE0999999999")
            ),
        )

        val normalized = resolver.normalize(
            classifiedType = DocumentType.Invoice,
            draftData = draft,
            tenant = tenant
        )

        val invoice = assertIs<InvoiceDraftData>(normalized.draftData)
        assertEquals(DocumentDirection.Outbound, invoice.direction)
    }

    @Test
    fun `keeps unknown direction when tenant party cannot be determined`() {
        val tenant = testTenant(VatNumber.from("BE0123456789")!!)
        val draft = InvoiceDraftData(
            seller = PartyDraft(name = "Google"),
            buyer = PartyDraft(name = "Acme")
        )

        val normalized = resolver.normalize(
            classifiedType = DocumentType.Invoice,
            draftData = draft,
            tenant = tenant
        )

        val invoice = assertIs<InvoiceDraftData>(normalized.draftData)
        assertEquals(DocumentDirection.Unknown, invoice.direction)
    }

    private fun testTenant(vat: VatNumber): Tenant {
        val now = LocalDateTime.parse("2026-01-01T00:00:00")
        return Tenant(
            id = TenantId.generate(),
            type = TenantType.Company,
            legalName = LegalName("INVOID VISION"),
            displayName = DisplayName("INVOID VISION"),
            subscription = SubscriptionTier.Core,
            status = TenantStatus.Active,
            language = Language.En,
            vatNumber = vat,
            createdAt = now,
            updatedAt = now
        )
    }
}
