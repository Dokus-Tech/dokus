package tech.dokus.features.cashflow.presentation.review

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.DocumentDraftDto
import tech.dokus.domain.model.toDocDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.domain.model.toDocumentType
import tech.dokus.foundation.app.state.DokusState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DocumentReviewStateConfirmBlockersTest {

    @Test
    fun `missing total blocks confirmation`() {
        val state = contentState(
            InvoiceDraftData(
                direction = DocumentDirection.Unknown,
                issueDate = LocalDate(2026, 2, 10),
                subtotalAmount = Money.from("100.00"),
                vatAmount = Money.from("21.00"),
                totalAmount = null,
            )
        )

        assertTrue(state.isBlocking)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `missing vat blocks invoice confirmation`() {
        val state = contentState(
            InvoiceDraftData(
                direction = DocumentDirection.Unknown,
                issueDate = LocalDate(2026, 2, 10),
                subtotalAmount = Money.from("100.00"),
                vatAmount = null,
                totalAmount = Money.from("121.00"),
            )
        )

        assertTrue(state.isBlocking)
    }

    @Test
    fun `missing vat blocks credit note confirmation`() {
        val state = contentState(
            CreditNoteDraftData(
                direction = DocumentDirection.Unknown,
                issueDate = LocalDate(2026, 2, 10),
                subtotalAmount = Money.from("100.00"),
                vatAmount = null,
                totalAmount = Money.from("121.00"),
            )
        )

        assertTrue(state.isBlocking)
    }

    @Test
    fun `pending counterparty blocks confirmation`() {
        val state = contentState(
            draftData = defaultInvoiceDraft(direction = DocumentDirection.Inbound),
            isPendingCreation = true,
            selectedContactId = null,
        )

        assertTrue(state.isBlocking)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `missing merchant blocks receipt confirmation`() {
        val state = contentState(
            ReceiptDraftData(
                direction = DocumentDirection.Inbound,
                merchantName = null,
                date = LocalDate(2026, 2, 10),
                totalAmount = Money.from("12.34"),
                vatAmount = null,
            )
        )

        assertTrue(state.isBlocking)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `missing credit note number blocks confirmation`() {
        val state = contentState(
            CreditNoteDraftData(
                direction = DocumentDirection.Inbound,
                creditNoteNumber = null,
                issueDate = LocalDate(2026, 2, 10),
                subtotalAmount = Money.from("100.00"),
                vatAmount = Money.from("21.00"),
                totalAmount = Money.from("121.00"),
            ),
            selectedContactId = ContactId.parse("8d2af381-d2bc-4f7f-8d2d-fae6dcbecf77"),
        )

        assertTrue(state.isBlocking)
        assertFalse(state.canConfirm)
    }

    @Test
    fun `missing vat does not block receipt confirmation`() {
        val state = contentState(
            ReceiptDraftData(
                direction = DocumentDirection.Inbound,
                merchantName = "Coffee shop",
                date = LocalDate(2026, 2, 10),
                totalAmount = Money.from("12.34"),
                vatAmount = null,
            )
        )

        assertFalse(state.isBlocking)
        assertTrue(state.canConfirm)
    }

    @Test
    fun `back guard is true only for unsynced changes`() {
        val synced = contentState(
            draftData = defaultInvoiceDraft(),
            hasUnsavedChanges = false,
            isSaving = false,
        )
        val unsynced = contentState(
            draftData = defaultInvoiceDraft(),
            hasUnsavedChanges = true,
            isSaving = false,
        )
        val inFlight = contentState(
            draftData = defaultInvoiceDraft(),
            hasUnsavedChanges = false,
            isSaving = true,
        )

        assertFalse(synced.hasUnsyncedLocalChanges)
        assertTrue(unsynced.hasUnsyncedLocalChanges)
        assertTrue(inFlight.hasUnsyncedLocalChanges)
    }

    private fun contentState(
        draftData: DocumentDraftData,
        hasUnsavedChanges: Boolean = false,
        isSaving: Boolean = false,
        selectedContactId: ContactId? = null,
        isPendingCreation: Boolean = false,
    ): DocumentReviewState {
        val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
        val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
        val now = LocalDateTime(2026, 2, 11, 0, 0, 0)

        val resolvedContact = when {
            selectedContactId != null -> ResolvedContact.Linked(
                contactId = selectedContactId,
                name = "Test Contact",
                vatNumber = null,
                email = null,
                avatarPath = null,
            )
            else -> ResolvedContact.Unknown
        }

        val draft = DocumentDraftDto(
            documentId = documentId,
            tenantId = tenantId,
            documentStatus = DocumentStatus.NeedsReview,
            documentType = draftData.toType(),
            content = draftData.toDocDto(),
            aiDraftSourceRunId = null,
            draftVersion = 1,
            draftEditedAt = null,
            draftEditedBy = null,
            resolvedContact = resolvedContact,
            lastSuccessfulRunId = null,
            createdAt = now,
            updatedAt = now,
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

        return DocumentReviewState(
            document = DokusState.success(
                ReviewDocumentData(
                    documentId = documentId,
                    documentRecord = record,
                    draftData = draftData.toDocDto(),
                    originalData = draftData.toDocDto(),
                    previewUrl = null,
                    contactSuggestions = emptyList(),
                )
            ),
            hasUnsavedChanges = hasUnsavedChanges,
            isSaving = isSaving,
            isContactRequired = true,
            selectedContactOverride = (resolvedContact as? ResolvedContact.Linked),
        )
    }

    private fun defaultInvoiceDraft(
        direction: DocumentDirection = DocumentDirection.Unknown,
    ) = InvoiceDraftData(
        direction = direction,
        issueDate = LocalDate(2026, 2, 10),
        subtotalAmount = Money.from("100.00"),
        vatAmount = Money.from("21.00"),
        totalAmount = Money.from("121.00"),
    )

    private fun DocumentDraftData.toType(): DocumentType = toDocumentType()
}
