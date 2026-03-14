package tech.dokus.backend.services.documents

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AutoConfirmPolicyTest {

    private lateinit var policy: AutoConfirmPolicy

    private val contactId = ContactId.parse("ee14421b-c659-45e3-9a27-e4d3ef6b32eb")

    @BeforeEach
    fun setup() {
        policy = AutoConfirmPolicy()
    }

    // ── Invoice tests ──────────────────────────────────────────────

    @Test
    fun `allows auto confirm for uploaded invoice with contact and high confidence`() {
        val result = policy.evaluate(
            invoiceInput(direction = DocumentDirection.Inbound, contactId = contactId)
        )
        assertNull(result)
    }

    @Test
    fun `blocks auto confirm when direction came only from ai hint`() {
        val result = policy.evaluate(
            invoiceInput(
                direction = DocumentDirection.Inbound,
                contactId = contactId,
                directionResolvedFromAiHintOnly = true
            )
        )
        assertEquals(AutoConfirmRejection.DirectionFromAiHintOnly, result)
    }

    @Test
    fun `blocks auto confirm when direction is unknown`() {
        val result = policy.evaluate(
            invoiceInput(direction = DocumentDirection.Unknown, contactId = contactId)
        )
        assertEquals(AutoConfirmRejection.InvalidDirection, result)
    }

    @Test
    fun `blocks auto confirm for uploaded invoice without contact`() {
        val result = policy.evaluate(
            invoiceInput(direction = DocumentDirection.Inbound, contactId = null)
        )
        assertEquals(AutoConfirmRejection.UnresolvedCounterparty, result)
    }

    // ── Receipt tests ──────────────────────────────────────────────

    @Test
    fun `allows auto confirm for uploaded receipt without contact when confidence is high`() {
        val result = policy.evaluate(
            receiptInput(contactId = null)
        )
        assertNull(result)
    }

    @Test
    fun `blocks auto confirm for receipt when required date is missing`() {
        val result = policy.evaluate(
            receiptInput(date = null, source = DocumentSource.Peppol)
        )
        assertEquals(AutoConfirmRejection.MissingRequiredFields, result)
    }

    @Test
    fun `blocks auto confirm for receipt when merchant is missing`() {
        val result = policy.evaluate(
            receiptInput(merchant = null, source = DocumentSource.Peppol)
        )
        assertEquals(AutoConfirmRejection.MissingRequiredFields, result)
    }

    @Test
    fun `blocks auto confirm for receipt when audit fails`() {
        val result = policy.evaluate(
            receiptInput(auditPassed = false)
        )
        assertEquals(AutoConfirmRejection.AuditFailed, result)
    }

    @Test
    fun `blocks auto confirm for receipt when confidence is too low`() {
        val result = policy.evaluate(
            receiptInput(confidence = 0.5)
        )
        assertEquals(AutoConfirmRejection.InsufficientConfidence, result)
    }

    // ── Credit note tests ──────────────────────────────────────────

    @Test
    fun `blocks auto confirm for credit note when required fields are missing`() {
        val result = policy.evaluate(
            creditNoteInput(issueDate = null)
        )
        assertEquals(AutoConfirmRejection.MissingRequiredFields, result)
    }

    @Test
    fun `blocks auto confirm for credit note without contact`() {
        val result = policy.evaluate(
            creditNoteInput(contactId = null)
        )
        assertEquals(AutoConfirmRejection.UnresolvedCounterparty, result)
    }

    // ── Source tests ───────────────────────────────────────────────

    @Test
    fun `blocks auto confirm for manual source`() {
        val result = policy.evaluate(
            invoiceInput(
                direction = DocumentDirection.Inbound,
                contactId = contactId,
                source = DocumentSource.Manual
            )
        )
        assertEquals(AutoConfirmRejection.ManualSource, result)
    }

    @Test
    fun `allows auto confirm for peppol invoice without confidence check`() {
        val result = policy.evaluate(
            invoiceInput(
                direction = DocumentDirection.Inbound,
                contactId = contactId,
                source = DocumentSource.Peppol,
                confidence = 0.5
            )
        )
        assertNull(result)
    }

    // ── Type mismatch test ─────────────────────────────────────────

    @Test
    fun `blocks auto confirm when document type does not match draft type`() {
        val result = policy.evaluate(
            AutoConfirmInput(
                source = DocumentSource.Upload,
                documentType = DocumentType.Invoice,
                draftData = receiptDraft(),
                auditPassed = true,
                confidence = 0.95,
                contactId = contactId,
                directionResolvedFromAiHintOnly = false,
            )
        )
        assertEquals(AutoConfirmRejection.TypeMismatch, result)
    }

    // ── Helpers ────────────────────────────────────────────────────

    private fun invoiceInput(
        direction: DocumentDirection = DocumentDirection.Inbound,
        contactId: ContactId? = this.contactId,
        source: DocumentSource = DocumentSource.Upload,
        confidence: Double = 1.0,
        auditPassed: Boolean = true,
        directionResolvedFromAiHintOnly: Boolean = false,
    ) = AutoConfirmInput(
        source = source,
        documentType = DocumentType.Invoice,
        draftData = invoiceDraft(direction),
        auditPassed = auditPassed,
        confidence = confidence,
        contactId = contactId,
        directionResolvedFromAiHintOnly = directionResolvedFromAiHintOnly,
    )

    private fun receiptInput(
        date: LocalDate? = LocalDate(2024, 1, 5),
        merchant: String? = "Acme Market",
        contactId: ContactId? = null,
        source: DocumentSource = DocumentSource.Upload,
        confidence: Double = 0.95,
        auditPassed: Boolean = true,
    ) = AutoConfirmInput(
        source = source,
        documentType = DocumentType.Receipt,
        draftData = receiptDraft(date, merchant),
        auditPassed = auditPassed,
        confidence = confidence,
        contactId = contactId,
        directionResolvedFromAiHintOnly = false,
    )

    private fun creditNoteInput(
        issueDate: LocalDate? = LocalDate(2024, 1, 5),
        contactId: ContactId? = this.contactId,
    ) = AutoConfirmInput(
        source = DocumentSource.Upload,
        documentType = DocumentType.CreditNote,
        draftData = creditNoteDraft(issueDate),
        auditPassed = true,
        confidence = 1.0,
        contactId = contactId,
        directionResolvedFromAiHintOnly = false,
    )

    private fun invoiceDraft(direction: DocumentDirection = DocumentDirection.Inbound) = InvoiceDraftData(
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
