package tech.dokus.features.cashflow.presentation.review.models

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CounterpartyIntent
import tech.dokus.domain.enums.Country
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.contact.CounterpartySnapshot
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CounterpartyInfoMapperTest {

    @Test
    fun `counterparty info reads name and vat from snapshot`() {
        val state = contentState(
            counterpartySnapshot = CounterpartySnapshot(
                name = "Apple Distribution International Ltd.",
                vatNumber = VatNumber.from("IE9700053D"),
                iban = Iban.from("IE29AIBK93115212345678"),
                streetLine1 = "Hollyhill Industrial Estate",
                postalCode = "T23",
                city = "Cork",
                country = Country.Belgium
            )
        )

        val info = counterpartyInfo(state)
        assertEquals("Apple Distribution International Ltd.", info.name)
        assertEquals("IE9700053D", info.vatNumber)
        assertEquals("IE29AIBK93115212345678", info.iban)
        assertEquals("Hollyhill Industrial Estate, T23 Cork, BE", info.address)
    }

    @Test
    fun `missing snapshot yields empty extracted info`() {
        val state = contentState(counterpartySnapshot = null)

        val info = counterpartyInfo(state)
        assertNull(info.name)
        assertNull(info.vatNumber)
        assertNull(info.iban)
        assertNull(info.address)
    }

    private fun contentState(counterpartySnapshot: CounterpartySnapshot?): DocumentReviewState.Content {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val contactId = ContactId.parse("ee14421b-c659-45e3-9a27-e4d3ef6b32eb")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        val draftData = InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            issueDate = LocalDate(2026, 2, 10),
            subtotalAmount = Money.from("100.00")
        )

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = DocumentType.Invoice,
            extractedData = draftData,
            aiDraftData = draftData,
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            linkedContactId = contactId,
            counterpartySnapshot = counterpartySnapshot,
            counterpartyIntent = CounterpartyIntent.None,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )

        val document = DocumentRecordDto(
            document = DocumentDto(
                id = documentId,
                tenantId = tenantId,
                filename = "invoice.pdf",
                contentType = "application/pdf",
                sizeBytes = 1200L,
                storageKey = "documents/$tenantId/invoice.pdf",
                source = DocumentSource.Upload,
                uploadedAt = now
            ),
            draft = draft,
            latestIngestion = null,
            confirmedEntity = null
        )

        return DocumentReviewState.Content(
            documentId = documentId,
            document = document,
            draftData = draftData,
            originalData = draftData,
            isContactRequired = true,
            selectedContactId = contactId,
            counterpartyIntent = CounterpartyIntent.None
        )
    }
}
