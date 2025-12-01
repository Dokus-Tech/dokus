package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Money
import ai.dokus.foundation.domain.enums.Currency
import ai.dokus.foundation.domain.enums.EntityType
import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.MediaDocumentType
import ai.dokus.foundation.domain.enums.MediaStatus
import ai.dokus.foundation.domain.ids.MediaId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.TransactionId
import ai.dokus.foundation.domain.VatRate
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class MediaDto(
    val id: MediaId,
    val tenantId: TenantId,
    val filename: String,
    val mimeType: String,
    val sizeBytes: Long,
    val status: MediaStatus,
    val processingSummary: String? = null,
    val extraction: MediaExtraction? = null,
    val attachedEntityType: EntityType? = null,
    val attachedEntityId: String? = null,
    val downloadUrl: String? = null,
    val errorMessage: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

@Serializable
data class MediaExtraction(
    val documentType: MediaDocumentType = MediaDocumentType.Unknown,
    val summary: String? = null,
    val invoice: ExtractedInvoiceData? = null,
    val expense: ExtractedExpenseData? = null,
    val bill: ExtractedBillData? = null,
    val confidence: ExtractionConfidence? = null,
    val rawText: String? = null
)

@Serializable
data class ExtractionConfidence(
    val overall: Double,
    val fields: Map<String, Double> = emptyMap()
)

@Serializable
data class ExtractedInvoiceData(
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val clientName: String? = null,
    val subtotal: Money? = null,
    val vatAmount: Money? = null,
    val total: Money? = null,
    val currency: Currency? = null,
    val paymentMethod: String? = null,
    val transactionId: TransactionId? = null,
    val notes: String? = null,
    val items: List<InvoiceItemDto>? = null
)

@Serializable
data class ExtractedExpenseData(
    val merchant: String? = null,
    val date: LocalDate? = null,
    val amount: Money? = null,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory? = null,
    val currency: Currency? = null,
    val paymentMethod: String? = null,
    val notes: String? = null
)

@Serializable
data class ExtractedBillData(
    val supplierName: String? = null,
    val supplierVatNumber: String? = null,
    val invoiceNumber: String? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val amount: Money? = null,
    val vatAmount: Money? = null,
    val vatRate: VatRate? = null,
    val category: ExpenseCategory? = null,
    val currency: Currency? = null,
    val description: String? = null,
    val notes: String? = null
)

@Serializable
data class MediaUploadRequest(
    val fileContent: ByteArray,
    val filename: String,
    val contentType: String,
    val entityType: EntityType? = null,
    val entityId: String? = null
)

@Serializable
data class MediaProcessingUpdateRequest(
    val mediaId: MediaId,
    val status: MediaStatus,
    val summary: String? = null,
    val extraction: MediaExtraction? = null,
    val errorMessage: String? = null,
    val attachedEntityType: EntityType? = null,
    val attachedEntityId: String? = null
)
