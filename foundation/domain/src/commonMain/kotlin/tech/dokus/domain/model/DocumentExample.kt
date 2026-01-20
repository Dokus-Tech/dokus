package tech.dokus.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.ExampleId
import tech.dokus.domain.ids.TenantId

/**
 * A document example used for few-shot learning.
 *
 * When processing a new document, the orchestrator looks up examples
 * from the same vendor (by VAT number or name) to guide extraction.
 * This improves accuracy for repeat vendors.
 */
@Serializable
data class DocumentExample(
    /** Unique identifier */
    val id: ExampleId,

    /** Tenant this example belongs to */
    val tenantId: TenantId,

    /** Vendor VAT number (primary lookup key) */
    val vendorVat: String?,

    /** Vendor name (fallback lookup key) */
    val vendorName: String,

    /** Document type */
    val documentType: DocumentType,

    /** Extraction data as JSON */
    val extraction: JsonElement,

    /** Confidence score when this example was created */
    val confidence: Double,

    /** Number of times this example has been used for few-shot learning */
    val timesUsed: Int = 0,

    /** When this example was created */
    val createdAt: Instant,

    /** When this example was last updated */
    val updatedAt: Instant
)
