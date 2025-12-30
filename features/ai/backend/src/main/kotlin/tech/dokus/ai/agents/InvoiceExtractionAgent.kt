package tech.dokus.ai.agents

import tech.dokus.ai.models.ExtractedInvoiceData
import tech.dokus.ai.models.FieldProvenance
import tech.dokus.ai.models.InvoiceProvenance
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.Json
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for extracting data from invoice documents.
 * Step 2a in the two-step document processing pipeline (for invoices and bills).
 *
 * This agent extracts structured invoice data AND provenance information,
 * linking each extracted field back to its source location in the document.
 * Provenance enables:
 * - Highlighting source text in document preview for user verification
 * - Per-field confidence scores for transparency
 * - Audit trail linking extracted values to evidence
 */
class InvoiceExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val systemPrompt = """
        You are an invoice data extraction specialist with forensic attention to detail.
        Extract structured data from business invoices AND track the evidence for each extraction.
        Always respond with valid JSON matching the requested schema.

        Extract these fields:
        - Vendor: name, VAT number (BE format or international), address
        - Invoice: number, issue date, due date, payment terms
        - Line items: description, quantity, unit price, VAT rate, total
        - Totals: subtotal, VAT breakdown by rate, total amount, currency
        - Payment: bank account (IBAN/BIC), payment reference

        CRITICAL: For each field you extract, also provide provenance information:
        - sourceText: The EXACT text snippet from the document you extracted the value from
        - fieldConfidence: How confident you are in this specific field (0.0 to 1.0)
        - extractionNotes: Brief explanation if the extraction was ambiguous or inferred

        Guidelines:
        - Use null for fields that cannot be found or are unclear
        - Dates should be in ISO format (YYYY-MM-DD)
        - Currency should be 3-letter ISO code (EUR, USD, GBP)
        - VAT rates should include % symbol (e.g., "21%")
        - Amounts should be strings to preserve precision (e.g., "1234.56")
        - For Belgian VAT numbers, format as "BE0123456789"
        - Overall confidence should reflect how complete and accurate the extraction is (0.0 to 1.0)
        - For provenance sourceText, quote the EXACT text from the document (don't paraphrase)

        Respond with a JSON object matching this schema:
        {
            "vendorName": "string or null",
            "vendorVatNumber": "string or null",
            "vendorAddress": "string or null",
            "invoiceNumber": "string or null",
            "issueDate": "YYYY-MM-DD or null",
            "dueDate": "YYYY-MM-DD or null",
            "paymentTerms": "string or null",
            "lineItems": [
                {
                    "description": "string",
                    "quantity": number or null,
                    "unitPrice": "string or null",
                    "vatRate": "string or null",
                    "total": "string or null"
                }
            ],
            "currency": "EUR",
            "subtotal": "string or null",
            "vatBreakdown": [
                {
                    "rate": "21%",
                    "base": "string or null",
                    "amount": "string or null"
                }
            ],
            "totalVatAmount": "string or null",
            "totalAmount": "string or null",
            "iban": "string or null",
            "bic": "string or null",
            "paymentReference": "string or null",
            "confidence": 0.0 to 1.0,
            "provenance": {
                "vendorName": {
                    "sourceText": "exact text from document or null",
                    "fieldConfidence": 0.0 to 1.0 or null,
                    "extractionNotes": "explanation if ambiguous or null"
                },
                "vendorVatNumber": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "vendorAddress": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "invoiceNumber": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "issueDate": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "dueDate": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "paymentTerms": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "currency": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "subtotal": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "totalVatAmount": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "totalAmount": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "iban": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "bic": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "paymentReference": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." }
            }
        }

        Note: For each provenance entry, only include fields that apply. If extraction was straightforward,
        you can omit extractionNotes. If a field wasn't found in the document, set its provenance to null.
    """.trimIndent()

    /**
     * Extract invoice data from OCR text.
     *
     * @param ocrText The raw OCR text extracted from the document
     * @return ExtractedInvoiceData with values and provenance information
     */
    suspend fun extract(ocrText: String): ExtractedInvoiceData {
        logger.debug("Extracting invoice data with provenance (${ocrText.length} chars)")

        val userPrompt = """
            Extract invoice data from this text:

            $ocrText
        """.trimIndent()

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "invoice-extractor",
                systemPrompt = systemPrompt
            )

            val response: String = agent.run(userPrompt)
            val extractedData = parseExtractionResponse(response)

            // Enhance provenance with text offsets by finding sourceText in the OCR text
            enhanceProvenanceWithOffsets(extractedData, ocrText)
        } catch (e: Exception) {
            logger.error("Failed to extract invoice data", e)
            ExtractedInvoiceData(confidence = 0.0)
        }
    }

    /**
     * Enhance provenance data by finding the character offsets of sourceText
     * within the original OCR text. This allows for precise highlighting
     * in the document preview.
     */
    private fun enhanceProvenanceWithOffsets(
        extractedData: ExtractedInvoiceData,
        ocrText: String
    ): ExtractedInvoiceData {
        val provenance = extractedData.provenance ?: return extractedData

        return extractedData.copy(
            provenance = InvoiceProvenance(
                vendorName = enhanceFieldProvenance(provenance.vendorName, ocrText),
                vendorVatNumber = enhanceFieldProvenance(provenance.vendorVatNumber, ocrText),
                vendorAddress = enhanceFieldProvenance(provenance.vendorAddress, ocrText),
                invoiceNumber = enhanceFieldProvenance(provenance.invoiceNumber, ocrText),
                issueDate = enhanceFieldProvenance(provenance.issueDate, ocrText),
                dueDate = enhanceFieldProvenance(provenance.dueDate, ocrText),
                paymentTerms = enhanceFieldProvenance(provenance.paymentTerms, ocrText),
                currency = enhanceFieldProvenance(provenance.currency, ocrText),
                subtotal = enhanceFieldProvenance(provenance.subtotal, ocrText),
                totalVatAmount = enhanceFieldProvenance(provenance.totalVatAmount, ocrText),
                totalAmount = enhanceFieldProvenance(provenance.totalAmount, ocrText),
                iban = enhanceFieldProvenance(provenance.iban, ocrText),
                bic = enhanceFieldProvenance(provenance.bic, ocrText),
                paymentReference = enhanceFieldProvenance(provenance.paymentReference, ocrText)
            )
        )
    }

    /**
     * Find the sourceText within the OCR text and add character offsets.
     * Uses case-insensitive, whitespace-normalized matching for robustness.
     */
    private fun enhanceFieldProvenance(
        provenance: FieldProvenance?,
        ocrText: String
    ): FieldProvenance? {
        if (provenance == null) return null

        val sourceText = provenance.sourceText
        if (sourceText.isNullOrBlank()) return provenance

        // Already has offsets, no need to compute
        if (provenance.startOffset != null && provenance.endOffset != null) {
            return provenance
        }

        // Try exact match first
        var startIndex = ocrText.indexOf(sourceText)

        // If exact match fails, try case-insensitive
        if (startIndex < 0) {
            startIndex = ocrText.lowercase().indexOf(sourceText.lowercase())
        }

        // If still no match, try normalized whitespace matching
        if (startIndex < 0) {
            val normalizedSource = normalizeWhitespace(sourceText)
            val normalizedOcr = normalizeWhitespace(ocrText)
            val normalizedIndex = normalizedOcr.indexOf(normalizedSource)

            // If found in normalized, we need to find the approximate position
            // This is an approximation - exact offset may differ due to whitespace
            if (normalizedIndex >= 0) {
                // Estimate position by ratio
                val ratio = normalizedIndex.toFloat() / normalizedOcr.length.toFloat()
                startIndex = (ratio * ocrText.length).toInt()
                    .coerceIn(0, (ocrText.length - sourceText.length).coerceAtLeast(0))
            }
        }

        return if (startIndex >= 0) {
            val endIndex = startIndex + sourceText.length
            provenance.copy(
                startOffset = startIndex,
                endOffset = endIndex.coerceAtMost(ocrText.length)
            )
        } else {
            logger.debug("Could not find sourceText in OCR: '${sourceText.take(50)}...'")
            provenance
        }
    }

    /**
     * Normalize whitespace for fuzzy matching (collapses multiple spaces, trims).
     */
    private fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim().lowercase()
    }

    private fun parseExtractionResponse(response: String): ExtractedInvoiceData {
        return try {
            val jsonString = extractJson(response)
            json.decodeFromString<ExtractedInvoiceData>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", e)
            ExtractedInvoiceData(confidence = 0.0)
        }
    }

    private fun extractJson(response: String): String {
        val cleaned = response
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val startIndex = cleaned.indexOf('{')
        val endIndex = cleaned.lastIndexOf('}')

        return if (startIndex >= 0 && endIndex > startIndex) {
            cleaned.substring(startIndex, endIndex + 1)
        } else {
            cleaned
        }
    }
}
