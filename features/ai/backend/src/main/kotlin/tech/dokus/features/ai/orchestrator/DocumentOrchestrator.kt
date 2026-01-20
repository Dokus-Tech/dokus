package tech.dokus.features.ai.orchestrator

import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.ContentPart
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.RequestMetaInfo
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentExample
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.ExtractedDocumentData
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
 * 4. Validation - Verify extracted data (totals, IBAN, OGM)
 * 5. Enrichment - Generate description, keywords, RAG chunks
 * 6. Storage - Persist extraction, chunks, examples
 * 7. Contacts - Lookup or create vendor contacts
 *
 * For PEPPOL documents (pre-parsed), only enrichment and storage are performed.
 */
class DocumentOrchestrator(
    private val executor: PromptExecutor,
    private val orchestratorModel: LLModel,
    private val visionModel: LLModel,
    private val toolRegistry: ToolRegistry,
    private val mode: IntelligenceMode
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
                findExampleForVendor(tenantId, images)
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
            // PHASE 4: VALIDATION - Verify extracted data
            // =========================================================================
            val (validation, validateDuration) = measureTimedValue {
                validateExtraction(extraction.data)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Validated extraction",
                    tool = "verify_totals, validate_iban, validate_ogm",
                    durationMs = validateDuration.inWholeMilliseconds,
                    notes = "Passed: ${validation.passed}, Issues: ${validation.issues.size}"
                )
            )

            // Self-correction loop if validation failed
            var correctedExtraction = extraction
            var correctionsApplied = 0
            if (!validation.passed && mode.enableSelfCorrection) {
                for (attempt in 1..minOf(mode.maxRetries, MAX_CORRECTIONS)) {
                    logger.info("Self-correction attempt $attempt")
                    val (corrected, correctDuration) = measureTimedValue {
                        selfCorrect(images, correctedExtraction, validation.issues)
                    }
                    if (corrected != null) {
                        correctedExtraction = corrected
                        correctionsApplied++
                        auditTrail.add(
                            ProcessingStep.create(
                                step = stepNumber++,
                                action = "Applied self-correction",
                                tool = "see_document",
                                durationMs = correctDuration.inWholeMilliseconds,
                                notes = "Attempt $attempt"
                            )
                        )
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
                        "Confidence ${correctedExtraction.confidence} is below " +
                            "auto-confirm threshold $AUTO_CONFIRM_THRESHOLD"
                    ) + validation.issues,
                    auditTrail = auditTrail
                )
            }

            // =========================================================================
            // PHASE 6: ENRICHMENT - Generate description, keywords, chunks
            // =========================================================================
            val (enrichment, enrichDuration) = measureTimedValue {
                enrichDocument(correctedExtraction.data, correctedExtraction.rawText)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Generated enrichment",
                    tool = "generate_description, generate_keywords, prepare_rag_chunks",
                    durationMs = enrichDuration.inWholeMilliseconds,
                    notes = "Description: ${enrichment.description.take(50)}..."
                )
            )

            // =========================================================================
            // PHASE 7: CONTACT RESOLUTION
            // =========================================================================
            val (contactResult, contactDuration) = measureTimedValue {
                resolveContact(tenantId, correctedExtraction.data, correctedExtraction.confidence)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = if (contactResult.created) "Created contact" else "Resolved contact",
                    tool = "lookup_contact, create_contact",
                    durationMs = contactDuration.inWholeMilliseconds,
                    notes = contactResult.contactId?.toString()
                )
            )

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
                validationPassed = validation.passed,
                correctionsApplied = correctionsApplied,
                exampleUsed = example?.id,
                contactId = contactResult.contactId,
                contactCreated = contactResult.created,
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
     *
     * @param documentId The document ID
     * @param tenantId The tenant ID
     * @param extraction Pre-parsed extraction data from PEPPOL
     * @param rawText Raw text content (from UBL XML)
     * @return Enrichment result
     */
    suspend fun enrich(
        documentId: DocumentId,
        tenantId: TenantId,
        extraction: ExtractedDocumentData,
        rawText: String
    ): EnrichmentResult {
        val auditTrail = mutableListOf<ProcessingStep>()
        var stepNumber = 1

        logger.info("Enriching PEPPOL document: $documentId")

        try {
            // Generate description
            val (description, descDuration) = measureTimedValue {
                generateDescription(extraction)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Generated description",
                    tool = "generate_description",
                    durationMs = descDuration.inWholeMilliseconds
                )
            )

            // Generate keywords
            val (keywords, kwDuration) = measureTimedValue {
                generateKeywords(extraction)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Generated keywords",
                    tool = "generate_keywords",
                    durationMs = kwDuration.inWholeMilliseconds
                )
            )

            // Prepare and store chunks
            val (chunksStored, chunkDuration) = measureTimedValue {
                prepareAndStoreChunks(documentId, tenantId, rawText)
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = "Stored RAG chunks",
                    tool = "prepare_rag_chunks, embed_text, store_chunks",
                    durationMs = chunkDuration.inWholeMilliseconds,
                    notes = "$chunksStored chunks"
                )
            )

            // Index as example (high confidence PEPPOL data)
            val (exampleIndexed, exampleDuration) = measureTimedValue {
                indexAsExample(tenantId, extraction, 1.0) // PEPPOL data is authoritative
            }
            auditTrail.add(
                ProcessingStep.create(
                    step = stepNumber++,
                    action = if (exampleIndexed) "Indexed as example" else "Skipped example indexing",
                    tool = "index_as_example",
                    durationMs = exampleDuration.inWholeMilliseconds
                )
            )

            return EnrichmentResult(
                description = description,
                keywords = keywords,
                chunksStored = chunksStored,
                exampleIndexed = exampleIndexed,
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
    // Private Helper Methods
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
        // This would be implemented using the SeeDocumentTool
        // For now, return a placeholder that the actual implementation would fill
        TODO("Implement using SeeDocumentTool via tool calling")
    }

    private suspend fun findExampleForVendor(
        tenantId: TenantId,
        images: List<DocumentImage>
    ): DocumentExample? {
        // This would be implemented using FindSimilarDocumentTool
        // First need to extract vendor info from the document
        TODO("Implement using FindSimilarDocumentTool via tool calling")
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
        // This would use the appropriate Extract*Tool based on type
        TODO("Implement using Extract*Tool via tool calling")
    }

    private data class ValidationResult(
        val passed: Boolean,
        val issues: List<String>
    )

    private suspend fun validateExtraction(data: JsonElement): ValidationResult {
        // This would use VerifyTotalsTool, ValidateIbanTool, ValidateOgmTool
        TODO("Implement using validation tools via tool calling")
    }

    private suspend fun selfCorrect(
        images: List<DocumentImage>,
        current: ExtractionResult,
        issues: List<String>
    ): ExtractionResult? {
        // Re-analyze document focusing on specific issues
        TODO("Implement self-correction loop")
    }

    private data class EnrichmentData(
        val description: String,
        val keywords: List<String>
    )

    private suspend fun enrichDocument(
        data: JsonElement,
        rawText: String
    ): EnrichmentData {
        // This would use GenerateDescriptionTool and GenerateKeywordsTool
        TODO("Implement using enrichment tools via tool calling")
    }

    private data class ContactResult(
        val contactId: ContactId?,
        val created: Boolean
    )

    private suspend fun resolveContact(
        tenantId: TenantId,
        data: JsonElement,
        confidence: Double
    ): ContactResult {
        // This would use LookupContactTool and optionally CreateContactTool
        TODO("Implement using contact tools via tool calling")
    }

    private suspend fun generateDescription(extraction: ExtractedDocumentData): String {
        // Use GenerateDescriptionTool
        TODO("Implement")
    }

    private suspend fun generateKeywords(extraction: ExtractedDocumentData): List<String> {
        // Use GenerateKeywordsTool
        TODO("Implement")
    }

    private suspend fun prepareAndStoreChunks(
        documentId: DocumentId,
        tenantId: TenantId,
        rawText: String
    ): Int {
        // Use PrepareRagChunksTool, EmbedTextTool, StoreChunksTool
        TODO("Implement")
    }

    private suspend fun indexAsExample(
        tenantId: TenantId,
        extraction: ExtractedDocumentData,
        confidence: Double
    ): Boolean {
        // Use IndexAsExampleTool
        TODO("Implement")
    }
}
