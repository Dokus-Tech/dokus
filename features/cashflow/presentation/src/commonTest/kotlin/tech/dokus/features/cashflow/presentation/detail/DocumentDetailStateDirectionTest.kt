package tech.dokus.features.cashflow.presentation.detail

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.PartyDraftDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.from
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentDetailStateDirectionTest {

    @Test
    fun `confirm is blocked for invoice when direction is unknown`() {
        val state = contentState(direction = DocumentDirection.Unknown)

        assertFalse(state.canConfirm)
        assertTrue(state.confirmBlockedReason != null)
    }

    @Test
    fun `confirm is allowed after invoice direction is selected`() {
        val state = contentState(direction = DocumentDirection.Inbound)

        assertTrue(state.canConfirm)
    }

    @Test
    fun `confirm is blocked when required contact is missing`() {
        val state = contentState(
            direction = DocumentDirection.Inbound,
            selectedContactId = null,
            isContactRequired = true,
        )

        assertFalse(state.canConfirm)
        assertTrue(state.confirmBlockedReason != null)
    }

    @Test
    fun `header description prefers snapshot name over extracted payment token name`() {
        val state = contentState(
            direction = DocumentDirection.Inbound,
            sellerName = "Visa .... 9803 (Apple Pay)",
            counterpartySnapshotName = "Apple Distribution International Ltd."
        )

        assertEquals("Apple Distribution International Ltd.", state.description)
    }

    private fun contentState(
        direction: DocumentDirection,
        sellerName: String? = null,
        counterpartySnapshotName: String? = null,
        selectedContactId: ContactId? = ContactId.parse("ee14421b-c659-45e3-9a27-e4d3ef6b32eb"),
        isContactRequired: Boolean = true,
    ): DocumentDetailState {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        val draftData = InvoiceDraftData(
            direction = direction,
            issueDate = LocalDate(2026, 2, 10),
            subtotalAmount = Money.from("100.00"),
            vatAmount = Money.from("21.00"),
            totalAmount = Money.from("121.00"),
            seller = PartyDraftDto(name = sellerName)
        )

        val resolvedContact = when {
            selectedContactId != null -> ResolvedContact.Linked(
                contactId = selectedContactId,
                name = counterpartySnapshotName ?: "Test Contact",
                vatNumber = null,
                email = null,
                avatarPath = null,
            )
            counterpartySnapshotName != null -> ResolvedContact.Detected(
                name = counterpartySnapshotName,
                vatNumber = null,
                iban = null,
                address = null,
            )
            else -> ResolvedContact.Unknown
        }

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = DocumentType.Invoice,
            content = DocDto.from(draftData),
            aiDraftSourceRunId = null,
            draftVersion = 0,
            draftEditedAt = null,
            draftEditedBy = null,
            resolvedContact = resolvedContact,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now
        )

        val record = DocumentDetailDto(
            document = DocumentDto(
                id = documentId,
                tenantId = tenantId,
                filename = "invoice.pdf",
                uploadedAt = now,
                sortDate = LocalDate(2026, 2, 11),
            ),
            draft = draft,
            latestIngestion = null,
        )

        return DocumentDetailState(
            document = DokusState.success(
                ReviewDocumentData(
                    documentId = documentId,
                    documentRecord = record,
                    draftData = DocDto.from(draftData),
                    originalData = DocDto.from(draftData),
                    previewUrl = null,
                    contactSuggestions = emptyList(),
                )
            ),
            isContactRequired = isContactRequired,
            selectedContactOverride = (resolvedContact as? ResolvedContact.Linked),
        )
    }
}
