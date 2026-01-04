package tech.dokus.features.ai.services

import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Service for provenance tracking using Claude API.
 *
 * Provenance links extracted field values back to their source text
 * in the original document, enabling:
 * - User verification of AI extractions
 * - Audit trail for compliance
 * - Training data collection
 *
 * Only active in cloud mode with ANTHROPIC_API_KEY configured.
 *
 * TODO: Implement Anthropic Claude integration for provenance generation.
 */
class ProvenanceService(
    private val config: AIConfig
) {
    private val logger = loggerFor()

    init {
        if (config.isProvenanceEnabled()) {
            logger.info("Provenance service initialized (Claude API enabled)")
        } else {
            logger.debug(
                "Provenance service disabled (mode=${config.mode}, " +
                    "apiKey=${if (config.anthropicApiKey != null) "set" else "not set"})"
            )
        }
    }

    /**
     * Check if provenance tracking is available.
     */
    fun isEnabled(): Boolean = config.isProvenanceEnabled()

    /**
     * Generate provenance for extracted invoice data.
     *
     * TODO: Implement Claude API call to analyze extraction quality
     * and generate source text mappings.
     *
     * @param rawText The original OCR text
     * @param extractedData The extracted invoice fields
     * @return Provenance mapping or null if not enabled
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun generateInvoiceProvenance(
        rawText: String,
        extractedData: Map<String, Any?>
    ): Map<String, FieldProvenanceData>? {
        if (!isEnabled()) return null

        // TODO: Implement Anthropic Claude integration
        // 1. Send rawText + extractedData to Claude
        // 2. Ask Claude to locate source text for each field
        // 3. Return field -> source text mapping with character offsets

        logger.debug("Provenance generation placeholder called (${extractedData.size} fields)")
        return null
    }

    /**
     * Generate provenance for extracted bill data.
     *
     * TODO: Implement similarly to invoice provenance.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun generateBillProvenance(
        rawText: String,
        extractedData: Map<String, Any?>
    ): Map<String, FieldProvenanceData>? {
        if (!isEnabled()) return null
        logger.debug("Bill provenance generation placeholder called")
        return null
    }

    /**
     * Generate provenance for extracted receipt data.
     *
     * TODO: Implement similarly to invoice provenance.
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun generateReceiptProvenance(
        rawText: String,
        extractedData: Map<String, Any?>
    ): Map<String, FieldProvenanceData>? {
        if (!isEnabled()) return null
        logger.debug("Receipt provenance generation placeholder called")
        return null
    }
}

/**
 * Provenance data for a single extracted field.
 */
data class FieldProvenanceData(
    /** The exact text from the document that was used to extract this field */
    val sourceText: String,
    /** Confidence that the source text is correct (0.0-1.0) */
    val confidence: Double,
    /** Character offset where the source text starts in the raw document */
    val startOffset: Int? = null,
    /** Character offset where the source text ends in the raw document */
    val endOffset: Int? = null,
    /** Optional notes about extraction ambiguity or alternatives */
    val notes: String? = null
)
