package tech.dokus.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Type-safe per-field provenance for canonical document data.
 *
 * Each document type has its own provenance class with named properties.
 * Null property = no provenance recorded for that field.
 */
@Serializable
sealed interface DocumentFieldProvenance {
    val direction: FieldProvenance?

    /** Returns a copy with the direction field's extraction confidence overridden. */
    fun withDirectionConfidence(confidence: Double): DocumentFieldProvenance
}

/**
 * Provenance for [InvoiceDraftData] fields.
 */
@Serializable
@SerialName("invoice")
data class InvoiceFieldProvenance(
    override val direction: FieldProvenance? = null,
    val invoiceNumber: FieldProvenance? = null,
    val issueDate: FieldProvenance? = null,
    val dueDate: FieldProvenance? = null,
    val currency: FieldProvenance? = null,
    val subtotalAmount: FieldProvenance? = null,
    val vatAmount: FieldProvenance? = null,
    val totalAmount: FieldProvenance? = null,
    val lineItems: FieldProvenance? = null,
    val vatBreakdown: FieldProvenance? = null,
    val iban: FieldProvenance? = null,
    val payment: FieldProvenance? = null,
    val notes: FieldProvenance? = null,
    val seller: PartyFieldProvenanceDto = PartyFieldProvenanceDto(),
    val buyer: PartyFieldProvenanceDto = PartyFieldProvenanceDto(),
) : DocumentFieldProvenance {
    override fun withDirectionConfidence(confidence: Double) =
        copy(direction = direction?.copy(extractionConfidence = confidence))
}

/**
 * Provenance for [CreditNoteDraftData] fields.
 */
@Serializable
@SerialName("credit_note")
data class CreditNoteFieldProvenance(
    override val direction: FieldProvenance? = null,
    val creditNoteNumber: FieldProvenance? = null,
    val issueDate: FieldProvenance? = null,
    val currency: FieldProvenance? = null,
    val subtotalAmount: FieldProvenance? = null,
    val vatAmount: FieldProvenance? = null,
    val totalAmount: FieldProvenance? = null,
    val lineItems: FieldProvenance? = null,
    val vatBreakdown: FieldProvenance? = null,
    val originalInvoiceNumber: FieldProvenance? = null,
    val reason: FieldProvenance? = null,
    val notes: FieldProvenance? = null,
    val seller: PartyFieldProvenanceDto = PartyFieldProvenanceDto(),
    val buyer: PartyFieldProvenanceDto = PartyFieldProvenanceDto(),
) : DocumentFieldProvenance {
    override fun withDirectionConfidence(confidence: Double) =
        copy(direction = direction?.copy(extractionConfidence = confidence))
}

/**
 * Provenance for [ReceiptDraftData] fields.
 */
@Serializable
@SerialName("receipt")
data class ReceiptFieldProvenance(
    override val direction: FieldProvenance? = null,
    val merchantName: FieldProvenance? = null,
    val merchantVat: FieldProvenance? = null,
    val date: FieldProvenance? = null,
    val currency: FieldProvenance? = null,
    val totalAmount: FieldProvenance? = null,
    val vatAmount: FieldProvenance? = null,
    val lineItems: FieldProvenance? = null,
    val vatBreakdown: FieldProvenance? = null,
    val receiptNumber: FieldProvenance? = null,
    val paymentMethod: FieldProvenance? = null,
    val notes: FieldProvenance? = null,
) : DocumentFieldProvenance {
    override fun withDirectionConfidence(confidence: Double) =
        copy(direction = direction?.copy(extractionConfidence = confidence))
}

/**
 * Provenance for [BankStatementDraftData] fields.
 */
@Serializable
@SerialName("bank_statement")
data class BankStatementFieldProvenance(
    override val direction: FieldProvenance? = null,
    val transactions: FieldProvenance? = null,
    val accountIban: FieldProvenance? = null,
    val openingBalance: FieldProvenance? = null,
    val closingBalance: FieldProvenance? = null,
    val periodStart: FieldProvenance? = null,
    val periodEnd: FieldProvenance? = null,
    val notes: FieldProvenance? = null,
) : DocumentFieldProvenance {
    override fun withDirectionConfidence(confidence: Double) =
        copy(direction = direction?.copy(extractionConfidence = confidence))
}

/**
 * Provenance for classified-only document types that only have a direction field.
 */
@Serializable
@SerialName("direction_only")
data class DirectionOnlyFieldProvenance(
    override val direction: FieldProvenance? = null,
) : DocumentFieldProvenance {
    override fun withDirectionConfidence(confidence: Double) =
        copy(direction = direction?.copy(extractionConfidence = confidence))
}

/**
 * Provenance for party (seller/buyer) sub-fields.
 */
@Serializable
data class PartyFieldProvenanceDto(
    val name: FieldProvenance? = null,
    val vat: FieldProvenance? = null,
    val email: FieldProvenance? = null,
    val iban: FieldProvenance? = null,
    val streetLine1: FieldProvenance? = null,
    val streetLine2: FieldProvenance? = null,
    val postalCode: FieldProvenance? = null,
    val city: FieldProvenance? = null,
    val country: FieldProvenance? = null,
)
