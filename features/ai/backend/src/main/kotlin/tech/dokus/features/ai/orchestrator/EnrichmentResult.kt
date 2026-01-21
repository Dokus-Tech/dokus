package tech.dokus.features.ai.orchestrator

import kotlinx.serialization.Serializable

/**
 * Result of document enrichment (used for PEPPOL documents).
 *
 * PEPPOL documents arrive with extraction already done by Recommand,
 * so we only need to generate description, keywords, and RAG chunks.
 */
@Serializable
data class EnrichmentResult(
    /** Human-readable description for UI: "Vendor — Item — €Amount" */
    val description: String,

    /** Keywords for search */
    val keywords: List<String>,

    /** Number of RAG chunks stored */
    val chunksStored: Int,

    /** Whether this document was indexed as an example for future few-shot learning */
    val exampleIndexed: Boolean,

    /** Audit trail of enrichment steps */
    val auditTrail: List<ProcessingStep>
)
