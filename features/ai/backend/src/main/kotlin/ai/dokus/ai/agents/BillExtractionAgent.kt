package ai.dokus.ai.agents

import ai.dokus.ai.models.BillProvenance
import ai.dokus.ai.models.ExtractedBillData
import ai.dokus.ai.models.FieldProvenance
import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.singleRunStrategy
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.serialization.json.Json
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Agent responsible for extracting data from bill (supplier invoice) documents.
 * Step 2b in the two-step document processing pipeline (for bills).
 *
 * Bills are incoming invoices from suppliers, as opposed to outgoing invoices to clients.
 *
 * This agent extracts structured bill data AND provenance information,
 * linking each extracted field back to its source location in the document.
 */
class BillExtractionAgent(
    private val executor: PromptExecutor,
    private val model: LLModel
) {
    private val logger = loggerFor()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val systemPrompt = """
        You are a bill/supplier invoice data extraction specialist with forensic attention to detail.
        Extract structured data from supplier invoices (bills) AND track the evidence for each extraction.
        Always respond with valid JSON matching the requested schema.

        A "bill" is an invoice you RECEIVE from a supplier (you owe them money).
        This is different from an "invoice" which is what you SEND to clients (they owe you money).

        Extract these fields:
        - Supplier: name, VAT number (BE format or international), address
        - Bill: invoice number, issue date, due date
        - Amount: total amount, VAT amount, VAT rate, currency
        - Line items (if present): description, quantity, unit price, VAT rate, total
        - Category: suggested expense category (OFFICE_SUPPLIES, HARDWARE, SOFTWARE, TRAVEL, etc.)
        - Payment: bank account (IBAN), payment terms

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

        Suggested expense categories for Belgian freelancers:
        - OFFICE_SUPPLIES: Paper, pens, office consumables
        - HARDWARE: Computers, monitors, peripherals
        - SOFTWARE: Licenses, subscriptions, SaaS
        - TRAVEL: Hotels, flights, transport
        - TRANSPORTATION: Fuel, public transport, parking
        - MEALS: Business meals, restaurants
        - PROFESSIONAL_SERVICES: Accountant, lawyer, consultants
        - UTILITIES: Internet, phone, electricity
        - TRAINING: Courses, certifications, books
        - MARKETING: Advertising, website, branding
        - INSURANCE: Professional liability, health
        - RENT: Office space, coworking
        - OTHER: Miscellaneous expenses

        Respond with a JSON object matching this schema:
        {
            "supplierName": "string or null",
            "supplierVatNumber": "string or null",
            "supplierAddress": "string or null",
            "invoiceNumber": "string or null",
            "issueDate": "YYYY-MM-DD or null",
            "dueDate": "YYYY-MM-DD or null",
            "currency": "EUR",
            "amount": "string or null",
            "vatAmount": "string or null",
            "vatRate": "21%",
            "totalAmount": "string or null",
            "lineItems": [
                {
                    "description": "string",
                    "quantity": number or null,
                    "unitPrice": "string or null",
                    "vatRate": "string or null",
                    "total": "string or null"
                }
            ],
            "category": "EXPENSE_CATEGORY or null",
            "description": "brief description of what was purchased or null",
            "paymentTerms": "string or null",
            "bankAccount": "IBAN or null",
            "notes": "string or null",
            "confidence": 0.0 to 1.0,
            "provenance": {
                "supplierName": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "supplierVatNumber": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "supplierAddress": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "invoiceNumber": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "issueDate": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "dueDate": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "amount": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "vatAmount": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "vatRate": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "currency": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "category": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." },
                "description": { "sourceText": "...", "fieldConfidence": 0.0-1.0, "extractionNotes": "..." }
            }
        }

        Note: For each provenance entry, only include fields that apply. If extraction was straightforward,
        you can omit extractionNotes. If a field wasn't found in the document, set its provenance to null.
    """.trimIndent()

    /**
     * Extract bill data from OCR text.
     *
     * @param ocrText The raw OCR text extracted from the document
     * @return ExtractedBillData with values and provenance information
     */
    suspend fun extract(ocrText: String): ExtractedBillData {
        logger.debug("Extracting bill data with provenance (${ocrText.length} chars)")

        val userPrompt = """
            Extract bill/supplier invoice data from this text:

            $ocrText
        """.trimIndent()

        return try {
            val agent = AIAgent(
                promptExecutor = executor,
                llmModel = model,
                strategy = singleRunStrategy(),
                toolRegistry = ToolRegistry.EMPTY,
                id = "bill-extractor",
                systemPrompt = systemPrompt
            )

            val response: String = agent.run(userPrompt)
            val extractedData = parseExtractionResponse(response)

            // Enhance provenance with text offsets by finding sourceText in the OCR text
            enhanceProvenanceWithOffsets(extractedData, ocrText)
        } catch (e: Exception) {
            logger.error("Failed to extract bill data", e)
            ExtractedBillData(confidence = 0.0)
        }
    }

    /**
     * Enhance provenance data by finding the character offsets of sourceText
     * within the original OCR text.
     */
    private fun enhanceProvenanceWithOffsets(
        extractedData: ExtractedBillData,
        ocrText: String
    ): ExtractedBillData {
        val provenance = extractedData.provenance ?: return extractedData

        return extractedData.copy(
            provenance = BillProvenance(
                supplierName = enhanceFieldProvenance(provenance.supplierName, ocrText),
                supplierVatNumber = enhanceFieldProvenance(provenance.supplierVatNumber, ocrText),
                supplierAddress = enhanceFieldProvenance(provenance.supplierAddress, ocrText),
                invoiceNumber = enhanceFieldProvenance(provenance.invoiceNumber, ocrText),
                issueDate = enhanceFieldProvenance(provenance.issueDate, ocrText),
                dueDate = enhanceFieldProvenance(provenance.dueDate, ocrText),
                amount = enhanceFieldProvenance(provenance.amount, ocrText),
                vatAmount = enhanceFieldProvenance(provenance.vatAmount, ocrText),
                vatRate = enhanceFieldProvenance(provenance.vatRate, ocrText),
                currency = enhanceFieldProvenance(provenance.currency, ocrText),
                category = enhanceFieldProvenance(provenance.category, ocrText),
                description = enhanceFieldProvenance(provenance.description, ocrText),
                paymentTerms = enhanceFieldProvenance(provenance.paymentTerms, ocrText),
                bankAccount = enhanceFieldProvenance(provenance.bankAccount, ocrText)
            )
        )
    }

    /**
     * Find the sourceText within the OCR text and add character offsets.
     */
    private fun enhanceFieldProvenance(
        provenance: FieldProvenance?,
        ocrText: String
    ): FieldProvenance? {
        if (provenance == null) return null

        val sourceText = provenance.sourceText
        if (sourceText.isNullOrBlank()) return provenance

        if (provenance.startOffset != null && provenance.endOffset != null) {
            return provenance
        }

        var startIndex = ocrText.indexOf(sourceText)

        if (startIndex < 0) {
            startIndex = ocrText.lowercase().indexOf(sourceText.lowercase())
        }

        if (startIndex < 0) {
            val normalizedSource = normalizeWhitespace(sourceText)
            val normalizedOcr = normalizeWhitespace(ocrText)
            val normalizedIndex = normalizedOcr.indexOf(normalizedSource)

            if (normalizedIndex >= 0) {
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

    private fun normalizeWhitespace(text: String): String {
        return text.replace(Regex("\\s+"), " ").trim().lowercase()
    }

    private fun parseExtractionResponse(response: String): ExtractedBillData {
        return try {
            val jsonString = extractJson(response)
            json.decodeFromString<ExtractedBillData>(jsonString)
        } catch (e: Exception) {
            logger.warn("Failed to parse extraction response: ${response.take(500)}", e)
            ExtractedBillData(confidence = 0.0)
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
