package tech.dokus.features.ai.orchestrator

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.ExampleId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentExample
import tech.dokus.domain.repository.ExampleRepository
import tech.dokus.features.ai.agents.DocumentClassificationAgent
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.domain.model.ExtractedDocumentData as DomainExtractedDocumentData
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.models.toDomainType
import tech.dokus.features.ai.prompts.AgentPrompt
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.config.IntelligenceMode
import tech.dokus.foundation.backend.utils.loggerFor
import kotlin.time.measureTimedValue

/**
 * Main orchestrator for document processing.
 *
 * Coordinates the full document processing pipeline:
 * 1. Triage - Classify document type
 * 2. Example Lookup - Find similar vendor documents for few-shot learning
 * 3. Extraction - Extract structured data using vision
 * 4. Validation - Verify extracted data (basic checks)
 * 5. Enrichment - Generate description, keywords
 *
 * For PEPPOL documents (pre-parsed), only enrichment is performed.
 */
class DocumentOrchestrator(
    private val executor: PromptExecutor,
    private val visionModel: LLModel,
    private val mode: IntelligenceMode,
    private val exampleRepository: ExampleRepository
) {
    private val logger = loggerFor()
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    companion object {
        /** Confidence threshold for auto-confirm */
        const val AUTO_CONFIRM_THRESHOLD = 0.85

        /** Maximum self-correction attempts */
        const val MAX_CORRECTIONS = 3
    }

    /**
     * Process an uploaded document (full pipeline).
     *
     * @param documentId The document ID
     * @param tenantId The tenant ID
     * @param images Document page images
     * @param tenantContext Tenant context for classification
     * @return Processing result (Success, NeedsReview, or Failed)
     */
    suspend fun process(
        documentId: DocumentId,
        tenantId: TenantId,
        images: List<DocumentImage>,
        tenantContext: AgentPrompt.TenantContext
    ): OrchestratorResult {
        val auditTrail = mutableListOf<ProcessingStep>()
        var stepNumber = 1

        logger.info("Processing document: $documentId (${images.size} pages)")

        try {
            // =========================================================================
            // PHASE 1: TRIAGE - Classify document type
            // =========================================================================
            val (classification, classifyDuration) = measureTimedValue {
                classifyDocument(images, tenantContext)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Classified document",
                    tool = "see_document",
                    durationMs = classifyDuration.inWholeMilliseconds,
                    notes = "Type: ${classification.type}, Confidence: ${classification.confidence}"
                )
            )

            if (classification.type == ClassifiedDocumentType.UNKNOWN) {
                return OrchestratorResult.NeedsReview(
                    documentType = null,
                    partialExtraction = null,
                    reason = "Could not determine document type",
                    issues = listOf(classification.reasoning),
                    auditTrail = auditTrail
                )
            }

            // =========================================================================
            // PHASE 2: EXAMPLE LOOKUP - Find similar vendor documents
            // =========================================================================
            val (example, lookupDuration) = measureTimedValue {
                findExampleForVendor(tenantId, classification.type)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = if (example != null) "Found example" else "No example found",
                    tool = "find_similar_document",
                    durationMs = lookupDuration.inWholeMilliseconds,
                    notes = example?.let { "Using example from ${it.vendorName}" }
                )
            )

            // =========================================================================
            // PHASE 3: EXTRACTION - Extract structured data
            // =========================================================================
            val (extraction, extractDuration) = measureTimedValue {
                extractDocument(images, classification.type, example)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Extracted document data",
                    tool = "extract_${classification.type.name.lowercase()}",
                    durationMs = extractDuration.inWholeMilliseconds,
                    notes = "Confidence: ${extraction.confidence}"
                )
            )

            // =========================================================================
            // PHASE 4: VALIDATION - Basic checks
            // =========================================================================
            val (validation, validateDuration) = measureTimedValue {
                validateExtraction(extraction.data, classification.type)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Validated extraction",
                    tool = "validate",
                    durationMs = validateDuration.inWholeMilliseconds,
                    notes = "Passed: ${validation.passed}, Issues: ${validation.issues.size}"
                )
            )

            // Self-correction loop if validation failed and mode allows it
            var correctedExtraction = extraction
            var correctionsApplied = 0
            if (!validation.passed && mode.enableSelfCorrection) {
                for (attempt in 1..minOf(mode.maxRetries, MAX_CORRECTIONS)) {
                    logger.info("Self-correction attempt $attempt for issues: ${validation.issues}")
                    val (corrected, correctDuration) = measureTimedValue {
                        selfCorrect(images, classification.type, correctedExtraction, validation.issues, example)
                    }
                    if (corrected != null && corrected.confidence > correctedExtraction.confidence) {
                        correctedExtraction = corrected
                        correctionsApplied++
                        auditTrail.add(
                            ProcessingStep.create(
                                step = stepNumber++,
                                action = "Applied self-correction",
                                tool = "extract_${classification.type.name.lowercase()}",
                                durationMs = correctDuration.inWholeMilliseconds,
                                notes = "Attempt $attempt, new confidence: ${corrected.confidence}"
                            )
                        )
                    } else {
                        break // No improvement, stop trying
                    }
                }
            }

            // =========================================================================
            // PHASE 5: CONFIDENCE CHECK
            // =========================================================================
            if (correctedExtraction.confidence < AUTO_CONFIRM_THRESHOLD) {
                return OrchestratorResult.NeedsReview(
                    documentType = classification.type,
                    partialExtraction = correctedExtraction.data,
                    reason = "Confidence below threshold",
                    issues = listOf(
                        "Confidence ${String.format("%.0f%%", correctedExtraction.confidence * 100)} is below " +
                            "auto-confirm threshold ${String.format("%.0f%%", AUTO_CONFIRM_THRESHOLD * 100)}"
                    ) + validation.issues,
                    auditTrail = auditTrail
                )
            }

            // =========================================================================
            // PHASE 6: ENRICHMENT - Generate description, keywords
            // =========================================================================
            val (enrichment, enrichDuration) = measureTimedValue {
                enrichDocument(correctedExtraction.data)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Generated enrichment",
                    tool = "generate_description",
                    durationMs = enrichDuration.inWholeMilliseconds,
                    notes = "Description: ${enrichment.description.take(50)}..."
                )
            )

            // =========================================================================
            // PHASE 7: INDEX AS EXAMPLE (for future few-shot learning)
            // =========================================================================
            val (indexed, indexDuration) = measureTimedValue {
                indexAsExample(tenantId, classification.type, correctedExtraction)
            }
            if (indexed) {
                auditTrail.add(
                    ProcessingStep.create(
                        step = stepNumber++,
                        action = "Indexed as example",
                        tool = "index_as_example",
                        durationMs = indexDuration.inWholeMilliseconds,
                        notes = "Vendor: ${extractVendorName(correctedExtraction.data)}"
                    )
                )
            }

            // =========================================================================
            // SUCCESS
            // =========================================================================
            return OrchestratorResult.Success(
                documentType = classification.type,
                extraction = correctedExtraction.data,
                confidence = correctedExtraction.confidence,
                rawText = correctedExtraction.rawText,
                description = enrichment.description,
                keywords = enrichment.keywords,
                validationPassed = validation.passed || correctionsApplied > 0,
                correctionsApplied = correctionsApplied,
                exampleUsed = example?.id,
                contactId = null, // Contact resolution is handled by the worker
                contactCreated = false,
                auditTrail = auditTrail
            )
        } catch (e: Exception) {
            logger.error("Document processing failed", e)
            return OrchestratorResult.Failed(
                reason = e.message ?: "Unknown error",
                stage = "processing",
                auditTrail = auditTrail
            )
        }
    }

    /**
     * Enrich a PEPPOL document (extraction already done by Recommand).
     */
    suspend fun enrich(
        documentId: DocumentId,
        tenantId: TenantId,
        extraction: DomainExtractedDocumentData,
        rawText: String
    ): EnrichmentResult {
        val auditTrail = mutableListOf<ProcessingStep>()
        var stepNumber = 1

        logger.info("Enriching PEPPOL document: $documentId")

        try {
            // Generate description and keywords
            val extractionJson = extractionToJson(extraction)
            val (enrichment, enrichDuration) = measureTimedValue {
                enrichDocument(extractionJson)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Generated enrichment",
                    tool = "generate_description",
                    durationMs = enrichDuration.inWholeMilliseconds
                )
            )

            return EnrichmentResult(
                description = enrichment.description,
                keywords = enrichment.keywords,
                chunksStored = 0, // RAG chunking is handled by the worker
                exampleIndexed = false, // PEPPOL docs don't need to be indexed as examples
                auditTrail = auditTrail
            )
        } catch (e: Exception) {
            logger.error("PEPPOL enrichment failed", e)
            return EnrichmentResult(
                description = "PEPPOL Document",
                keywords = emptyList(),
                chunksStored = 0,
                exampleIndexed = false,
                auditTrail = auditTrail
            )
        }
    }

    // =========================================================================
    // Private Implementation Methods
    // =========================================================================

    private data class ClassificationResult(
        val type: ClassifiedDocumentType,
        val confidence: Double,
        val reasoning: String
    )

    private suspend fun classifyDocument(
        images: List<DocumentImage>,
        tenantContext: AgentPrompt.TenantContext
    ): ClassificationResult {
        val agent = DocumentClassificationAgent(
            executor = executor,
            model = visionModel,
            prompt = AgentPrompt.DocumentClassification
        )

        val result = agent.classify(images, tenantContext)

        return ClassificationResult(
            type = result.documentType,
            confidence = result.confidence,
            reasoning = result.reasoning
        )
    }

    private suspend fun findExampleForVendor(
        tenantId: TenantId,
        documentType: ClassifiedDocumentType
    ): DocumentExample? {
        // We can't find examples until we have vendor info from extraction
        // This is a chicken-and-egg problem - for now, skip example lookup
        // A future improvement could do a quick vendor name extraction first
        return null
    }

    private data class ExtractionResult(
        val data: JsonElement,
        val confidence: Double,
        val rawText: String
    )

    private suspend fun extractDocument(
        images: List<DocumentImage>,
        type: ClassifiedDocumentType,
        example: DocumentExample?
    ): ExtractionResult {
        val exampleJson = example?.extraction?.toString()

        return when (type) {
            ClassifiedDocumentType.INVOICE -> extractInvoice(images, exampleJson)
            ClassifiedDocumentType.BILL -> extractBill(images, exampleJson)
            ClassifiedDocumentType.RECEIPT -> extractReceipt(images, exampleJson)
            ClassifiedDocumentType.EXPENSE -> extractExpense(images, exampleJson)
            ClassifiedDocumentType.CREDIT_NOTE -> extractInvoice(images, exampleJson) // Same format as invoice
            ClassifiedDocumentType.PRO_FORMA -> extractInvoice(images, exampleJson) // Same format as invoice
            ClassifiedDocumentType.UNKNOWN -> ExtractionResult(
                data = json.parseToJsonElement("{}"),
                confidence = 0.0,
                rawText = ""
            )
        }
    }

    private suspend fun extractInvoice(
        images: List<DocumentImage>,
        exampleJson: String?
    ): ExtractionResult {
        val agent = ExtractionAgent<ExtractedInvoiceData>(
            executor = executor,
            model = visionModel,
            prompt = AgentPrompt.Extraction.Invoice,
            userPromptPrefix = buildUserPromptPrefix(exampleJson, "invoice"),
            promptId = "invoice-extractor",
            emptyResult = { ExtractedInvoiceData(confidence = 0.0) }
        )

        val result = agent.extract(images)
        return ExtractionResult(
            data = json.encodeToJsonElement(ExtractedInvoiceData.serializer(), result),
            confidence = result.confidence,
            rawText = result.extractedText ?: ""
        )
    }

    private suspend fun extractBill(
        images: List<DocumentImage>,
        exampleJson: String?
    ): ExtractionResult {
        val agent = ExtractionAgent<ExtractedBillData>(
            executor = executor,
            model = visionModel,
            prompt = AgentPrompt.Extraction.Bill,
            userPromptPrefix = buildUserPromptPrefix(exampleJson, "bill"),
            promptId = "bill-extractor",
            emptyResult = { ExtractedBillData(confidence = 0.0) }
        )

        val result = agent.extract(images)
        return ExtractionResult(
            data = json.encodeToJsonElement(ExtractedBillData.serializer(), result),
            confidence = result.confidence,
            rawText = result.extractedText ?: ""
        )
    }

    private suspend fun extractReceipt(
        images: List<DocumentImage>,
        exampleJson: String?
    ): ExtractionResult {
        val agent = ExtractionAgent<ExtractedReceiptData>(
            executor = executor,
            model = visionModel,
            prompt = AgentPrompt.Extraction.Receipt,
            userPromptPrefix = buildUserPromptPrefix(exampleJson, "receipt"),
            promptId = "receipt-extractor",
            emptyResult = { ExtractedReceiptData(confidence = 0.0) }
        )

        val result = agent.extract(images)
        return ExtractionResult(
            data = json.encodeToJsonElement(ExtractedReceiptData.serializer(), result),
            confidence = result.confidence,
            rawText = result.extractedText ?: ""
        )
    }

    private suspend fun extractExpense(
        images: List<DocumentImage>,
        exampleJson: String?
    ): ExtractionResult {
        val agent = ExtractionAgent<ExtractedExpenseData>(
            executor = executor,
            model = visionModel,
            prompt = AgentPrompt.Extraction.Expense,
            userPromptPrefix = buildUserPromptPrefix(exampleJson, "expense"),
            promptId = "expense-extractor",
            emptyResult = { ExtractedExpenseData(confidence = 0.0) }
        )

        val result = agent.extract(images)
        return ExtractionResult(
            data = json.encodeToJsonElement(ExtractedExpenseData.serializer(), result),
            confidence = result.confidence,
            rawText = result.extractedText ?: ""
        )
    }

    private fun buildUserPromptPrefix(exampleJson: String?, documentType: String): String {
        return if (exampleJson != null) {
            """
            Extract $documentType data from this document.

            REFERENCE EXAMPLE from same vendor (use this format and look for similar fields):
            $exampleJson

            Now extract from this
            """.trimIndent()
        } else {
            "Extract $documentType data from this"
        }
    }

    private data class ValidationResult(
        val passed: Boolean,
        val issues: List<String>
    )

    private fun validateExtraction(data: JsonElement, type: ClassifiedDocumentType): ValidationResult {
        val issues = mutableListOf<String>()
        val obj = data.jsonObject

        // Check for essential fields based on document type
        when (type) {
            ClassifiedDocumentType.INVOICE, ClassifiedDocumentType.CREDIT_NOTE, ClassifiedDocumentType.PRO_FORMA -> {
                if (obj["vendorName"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing vendor name")
                }
                if (obj["totalAmount"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing total amount")
                }
            }
            ClassifiedDocumentType.BILL -> {
                if (obj["supplierName"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing supplier name")
                }
                if (obj["totalAmount"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing total amount")
                }
            }
            ClassifiedDocumentType.RECEIPT -> {
                if (obj["merchantName"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing merchant name")
                }
                if (obj["totalAmount"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing total amount")
                }
            }
            ClassifiedDocumentType.EXPENSE -> {
                if (obj["totalAmount"]?.jsonPrimitive?.content.isNullOrBlank()) {
                    issues.add("Missing total amount")
                }
            }
            ClassifiedDocumentType.UNKNOWN -> {
                issues.add("Unknown document type")
            }
        }

        return ValidationResult(
            passed = issues.isEmpty(),
            issues = issues
        )
    }

    private suspend fun selfCorrect(
        images: List<DocumentImage>,
        type: ClassifiedDocumentType,
        current: ExtractionResult,
        issues: List<String>,
        example: DocumentExample?
    ): ExtractionResult? {
        // Re-extract with focus on the issues
        logger.debug("Self-correcting extraction, issues: $issues")

        // Simply re-run extraction - the model may get different/better results
        return try {
            extractDocument(images, type, example)
        } catch (e: Exception) {
            logger.warn("Self-correction failed", e)
            null
        }
    }

    private data class EnrichmentData(
        val description: String,
        val keywords: List<String>
    )

    private fun enrichDocument(data: JsonElement): EnrichmentData {
        val obj = data.jsonObject

        // Generate description in format: "Vendor — Item/Service — €Amount — Month"
        val description = generateDescription(obj)
        val keywords = generateKeywords(obj)

        return EnrichmentData(
            description = description,
            keywords = keywords
        )
    }

    private fun generateDescription(obj: Map<String, JsonElement>): String {
        // Extract vendor name
        val vendorName = extractField(obj, "vendorName", "supplierName", "merchantName")
            ?: "Unknown Vendor"

        // Extract item description
        val itemDescription = extractItemDescription(obj)

        // Extract amount and currency
        val amount = extractField(obj, "totalAmount", "amount", "total")
        val currency = extractField(obj, "currency") ?: "EUR"
        val currencySymbol = when (currency.uppercase()) {
            "EUR" -> "€"
            "USD" -> "$"
            "GBP" -> "£"
            else -> currency
        }

        // Extract month
        val month = extractMonth(obj)

        return buildString {
            append(vendorName.take(30))
            append(" — ")
            append(itemDescription.take(30))
            if (amount != null) {
                append(" — ")
                append(currencySymbol)
                append(amount)
            }
            if (month != null) {
                append(" — ")
                append(month)
            }
        }
    }

    private fun generateKeywords(obj: Map<String, JsonElement>): List<String> {
        val keywords = mutableSetOf<String>()
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "by", "from", "as", "is", "was", "are", "were", "de",
            "en", "het", "van", "een", "voor", "met", "op", "te", "zijn"
        )

        // Extract from various fields
        extractField(obj, "vendorName", "supplierName", "merchantName")?.let { keywords.add(it) }
        extractField(obj, "invoiceNumber", "billNumber", "receiptNumber")?.let { keywords.add(it) }
        extractField(obj, "vendorVatNumber", "supplierVatNumber", "vatNumber")?.let { keywords.add(it) }
        extractField(obj, "category", "expenseCategory")?.let { keywords.add(it) }

        // Clean and filter
        return keywords
            .flatMap { it.split(Regex("[\\s,;./\\-_]+")) }
            .map { it.lowercase().trim() }
            .filter { it.length >= 3 }
            .filter { it !in stopWords }
            .filter { !it.all { c -> c.isDigit() } }
            .distinct()
            .take(30)
    }

    private fun extractField(obj: Map<String, JsonElement>, vararg fieldNames: String): String? {
        for (name in fieldNames) {
            try {
                val value = obj[name]?.jsonPrimitive?.content
                if (!value.isNullOrBlank()) return value
            } catch (_: Exception) {
                // Field is not a primitive, skip
            }
        }
        return null
    }

    private fun extractItemDescription(obj: Map<String, JsonElement>): String {
        // Try to get first line item description
        try {
            val lineItems = obj["lineItems"]
            if (lineItems != null) {
                val items = lineItems as? kotlinx.serialization.json.JsonArray
                if (items != null && items.isNotEmpty()) {
                    val firstItem = items[0].jsonObject
                    val desc = firstItem["description"]?.jsonPrimitive?.content
                    if (!desc.isNullOrBlank()) return desc
                }
            }
        } catch (_: Exception) {
            // Ignore
        }

        // Fallback
        return extractField(obj, "description", "category", "expenseCategory") ?: "Document"
    }

    private fun extractMonth(obj: Map<String, JsonElement>): String? {
        val dateStr = extractField(obj, "issueDate", "billDate", "transactionDate", "date")
            ?: return null

        return try {
            val month = dateStr.substring(5, 7).toInt()
            val monthNames = listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            monthNames.getOrNull(month - 1)
        } catch (_: Exception) {
            null
        }
    }

    private fun extractVendorName(data: JsonElement): String? {
        val obj = data.jsonObject
        return extractField(obj, "vendorName", "supplierName", "merchantName")
    }

    private fun extractVendorVat(data: JsonElement): String? {
        val obj = data.jsonObject
        return extractField(obj, "vendorVatNumber", "supplierVatNumber", "merchantVatNumber")
    }

    private suspend fun indexAsExample(
        tenantId: TenantId,
        type: ClassifiedDocumentType,
        extraction: ExtractionResult
    ): Boolean {
        // Only index high-confidence extractions
        if (extraction.confidence < AUTO_CONFIRM_THRESHOLD) {
            return false
        }

        val vendorName = extractVendorName(extraction.data)
        if (vendorName.isNullOrBlank()) {
            return false
        }

        val vendorVat = extractVendorVat(extraction.data)

        return try {
            val now = Clock.System.now()
            val example = DocumentExample(
                id = ExampleId.generate(),
                tenantId = tenantId,
                vendorVat = vendorVat,
                vendorName = vendorName,
                documentType = type.toDomainType(),
                extraction = extraction.data,
                confidence = extraction.confidence,
                timesUsed = 0,
                createdAt = now,
                updatedAt = now
            )
            exampleRepository.save(example)
            logger.debug("Indexed example for vendor: $vendorName")
            true
        } catch (e: Exception) {
            logger.warn("Failed to index example: ${e.message}")
            false
        }
    }

    private fun extractionToJson(extraction: DomainExtractedDocumentData): JsonElement {
        // Convert domain ExtractedDocumentData to JsonElement for enrichment
        return when {
            extraction.invoice != null -> json.encodeToJsonElement(
                ExtractedInvoiceData.serializer(),
                ExtractedInvoiceData(
                    vendorName = extraction.invoice!!.clientName,
                    vendorVatNumber = extraction.invoice!!.clientVatNumber,
                    invoiceNumber = extraction.invoice!!.invoiceNumber,
                    issueDate = extraction.invoice!!.issueDate?.toString(),
                    dueDate = extraction.invoice!!.dueDate?.toString(),
                    totalAmount = extraction.invoice!!.totalAmount?.toDisplayString(),
                    currency = extraction.invoice!!.currency?.name,
                    confidence = 1.0 // PEPPOL is authoritative
                )
            )
            extraction.bill != null -> json.encodeToJsonElement(
                ExtractedBillData.serializer(),
                ExtractedBillData(
                    supplierName = extraction.bill!!.supplierName,
                    supplierVatNumber = extraction.bill!!.supplierVatNumber,
                    invoiceNumber = extraction.bill!!.invoiceNumber,
                    issueDate = extraction.bill!!.issueDate?.toString(),
                    dueDate = extraction.bill!!.dueDate?.toString(),
                    totalAmount = extraction.bill!!.amount?.toDisplayString(),
                    currency = extraction.bill!!.currency?.name,
                    confidence = 1.0
                )
            )
            extraction.expense != null -> json.encodeToJsonElement(
                ExtractedExpenseData.serializer(),
                ExtractedExpenseData(
                    merchantName = extraction.expense!!.merchant,
                    description = extraction.expense!!.description,
                    totalAmount = extraction.expense!!.amount?.toDisplayString(),
                    currency = extraction.expense!!.currency?.name,
                    confidence = 1.0
                )
            )
            else -> json.parseToJsonElement("{}")
        }
    }
}
