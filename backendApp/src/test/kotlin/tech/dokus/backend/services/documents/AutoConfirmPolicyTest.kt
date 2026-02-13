package tech.dokus.backend.services.documents

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.cashflow.DocumentRepository
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutoConfirmPolicyTest {

    private lateinit var documentRepository: DocumentRepository
    private lateinit var policy: AutoConfirmPolicy

    private val tenantId = TenantId.parse("44e8ed5c-020a-4bbb-9439-ac85899c5589")
    private val documentId = DocumentId.parse("e72f69a8-6913-4d8f-98e7-224db7f4133f")
    private val contactId = ContactId.parse("ee14421b-c659-45e3-9a27-e4d3ef6b32eb")

    @BeforeEach
    fun setup() {
        documentRepository = mockk()
        policy = AutoConfirmPolicy(documentRepository)
        coEvery { documentRepository.getContentHash(any(), any()) } returns null
    }

    @Test
    fun `blocks auto confirm when direction came only from ai hint`() = runBlocking {
        val canAutoConfirm = policy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Upload,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(direction = DocumentDirection.Inbound),
            auditPassed = true,
            confidence = 0.99,
            linkedContactId = contactId,
            directionResolvedFromAiHintOnly = true
        )

        assertFalse(canAutoConfirm)
    }

    @Test
    fun `allows auto confirm when confidence and deterministic direction are valid`() = runBlocking {
        val canAutoConfirm = policy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Upload,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(direction = DocumentDirection.Inbound),
            auditPassed = true,
            confidence = 1.0,
            linkedContactId = contactId,
            directionResolvedFromAiHintOnly = false
        )

        assertTrue(canAutoConfirm)
    }

    @Test
    fun `blocks auto confirm when direction is unknown`() = runBlocking {
        val canAutoConfirm = policy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Upload,
            documentType = DocumentType.Invoice,
            draftData = invoiceDraft(direction = DocumentDirection.Unknown),
            auditPassed = true,
            confidence = 0.99,
            linkedContactId = contactId,
            directionResolvedFromAiHintOnly = false
        )

        assertFalse(canAutoConfirm)
    }

    @Test
    fun `blocks auto confirm for receipt when required date is missing`() = runBlocking {
        val canAutoConfirm = policy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Peppol,
            documentType = DocumentType.Receipt,
            draftData = receiptDraft(date = null),
            auditPassed = true,
            confidence = 1.0,
            linkedContactId = null,
            directionResolvedFromAiHintOnly = false
        )

        assertFalse(canAutoConfirm)
    }

    @Test
    fun `blocks auto confirm for receipt when merchant is missing`() = runBlocking {
        val canAutoConfirm = policy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Peppol,
            documentType = DocumentType.Receipt,
            draftData = receiptDraft(merchant = null),
            auditPassed = true,
            confidence = 1.0,
            linkedContactId = null,
            directionResolvedFromAiHintOnly = false
        )

        assertFalse(canAutoConfirm)
    }

    @Test
    fun `blocks auto confirm for credit note when required fields are missing`() = runBlocking {
        val canAutoConfirm = policy.canAutoConfirm(
            tenantId = tenantId,
            documentId = documentId,
            source = DocumentSource.Peppol,
            documentType = DocumentType.CreditNote,
            draftData = creditNoteDraft(issueDate = null),
            auditPassed = true,
            confidence = 1.0,
            linkedContactId = contactId,
            directionResolvedFromAiHintOnly = false
        )

        assertFalse(canAutoConfirm)
    }

    private fun invoiceDraft(direction: DocumentDirection) = InvoiceDraftData(
        direction = direction,
        invoiceNumber = "5480883565",
        totalAmount = Money.from("18.48")
    )

    private fun receiptDraft(
        date: LocalDate? = LocalDate(2024, 1, 5),
        merchant: String? = "Acme Market"
    ) = ReceiptDraftData(
        direction = DocumentDirection.Inbound,
        merchantName = merchant,
        date = date,
        totalAmount = Money.from("18.48")
    )

    private fun creditNoteDraft(
        issueDate: LocalDate? = LocalDate(2024, 1, 5)
    ) = CreditNoteDraftData(
        creditNoteNumber = "CN-100",
        direction = DocumentDirection.Inbound,
        issueDate = issueDate,
        subtotalAmount = Money.from("100.00"),
        vatAmount = Money.from("21.00"),
        totalAmount = Money.from("121.00")
    )
}
