package tech.dokus.features.ai.service

import tech.dokus.features.ai.agents.BillExtractionAgent
import tech.dokus.features.ai.agents.CategorySuggestionAgent
import tech.dokus.features.ai.agents.DocumentClassificationAgent
import tech.dokus.features.ai.agents.InvoiceExtractionAgent
import tech.dokus.features.ai.agents.ReceiptExtractionAgent
import tech.dokus.features.ai.config.AIProviderFactory
import tech.dokus.features.ai.models.CategorySuggestion
import tech.dokus.features.ai.models.ClassifiedDocumentType
import tech.dokus.features.ai.models.DocumentAIResult
import tech.dokus.features.ai.models.DocumentClassification
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.config.AIConfig
import tech.dokus.foundation.backend.config.AIMode
import tech.dokus.foundation.backend.config.ModelPurpose
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * High-level AI service that orchestrates document processing agents.
 *
 * VISION-FIRST: This service accepts document images directly.
 * Vision models (qwen3-vl) analyze images without requiring OCR.
 *
 * Provides a clean API for:
 * - Two-step document processing (classify then extract) -> DocumentAIResult
 * - Direct extraction (when type is known)
 * - Category suggestions for expenses
 */
class AIService(
    private val config: AIConfig
) {
    private val logger = loggerFor()

    init {
        logModeConfiguration()
    }

    private fun logModeConfiguration() {
        logger.info("============================================================")
        logger.info("AI Service initialized (VISION-FIRST)")
        logger.info("  Mode: ${config.mode}")
        logger.info("  Ollama Host: ${config.ollamaHost}")
        logger.info("  Vision Model (Classification): ${config.getModel(ModelPurpose.CLASSIFICATION)}")
        logger.info("  Vision Model (Extraction): ${config.getModel(ModelPurpose.DOCUMENT_EXTRACTION)}")
        logger.info("  Chat Model: ${config.getModel(ModelPurpose.CHAT)}")
        logger.info("  Embedding Model: ${AIProviderFactory.EMBEDDING_MODEL}")
        logger.info("  Provenance Enabled: ${config.isProvenanceEnabled()}")
        if (config.mode == AIMode.CLOUD && !config.isProvenanceEnabled()) {
            logger.warn("  [!] Cloud mode without ANTHROPIC_API_KEY - provenance disabled")
        }
        logger.info("============================================================")
    }

    // Create executor once for all agents
    private val executor by lazy {
        logger.info("Initializing Ollama executor: ${config.ollamaHost}")
        AIProviderFactory.createExecutor(config)
    }

    // Step 1: Classification agent
    private val classificationAgent by lazy {
        DocumentClassificationAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.CLASSIFICATION)
        )
    }

    // Step 2a: Invoice extraction agent
    private val invoiceAgent by lazy {
        InvoiceExtractionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION)
        )
    }

    // Step 2b: Bill extraction agent
    private val billAgent by lazy {
        BillExtractionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION)
        )
    }

    // Step 2c: Receipt extraction agent
    private val receiptAgent by lazy {
        ReceiptExtractionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.DOCUMENT_EXTRACTION)
        )
    }

    // Category suggestion agent
    private val categoryAgent by lazy {
        CategorySuggestionAgent(
            executor = executor,
            model = AIProviderFactory.getModel(config, ModelPurpose.CATEGORIZATION)
        )
    }

    /**
     * Two-step document processing:
     * 1. Classify the document type using vision model
     * 2. Extract data using the appropriate vision agent
     *
     * VISION-FIRST: Accepts document images directly.
     * Vision models analyze images without requiring OCR preprocessing.
     *
     * @param images List of document page images
     * @return Result containing DocumentAIResult sealed class with:
     *         - classification
     *         - type-specific extracted payload with provenance
     *         - extractedText for RAG indexing
     *         - confidence and warnings
     */
    suspend fun processDocument(images: List<DocumentImage>): Result<DocumentAIResult> = runCatching {
        logger.info("Processing document (${images.size} pages)")
        val warnings = mutableListOf<String>()

        if (images.isEmpty()) {
            throw IllegalArgumentException("No images provided for processing")
        }

        // Step 1: Classify document type (use first page for classification)
        val classificationImages = images.take(1) // Usually first page is enough for classification
        val classification = classificationAgent.classify(classificationImages)
        logger.info("Document classified as ${classification.documentType} (confidence: ${classification.confidence})")

        if (classification.confidence < 0.5) {
            warnings.add("Low classification confidence: ${classification.confidence}")
        }

        // Step 2: Extract based on classification (use all pages for extraction)
        when (classification.documentType) {
            ClassifiedDocumentType.INVOICE -> {
                logger.debug("Extracting as invoice")
                val data = invoiceAgent.extract(images)
                DocumentAIResult.Invoice(
                    classification = classification,
                    extractedData = data,
                    confidence = data.confidence,
                    warnings = warnings,
                    rawText = data.extractedText ?: ""
                )
            }

            ClassifiedDocumentType.BILL -> {
                logger.debug("Extracting as bill")
                val data = billAgent.extract(images)
                DocumentAIResult.Bill(
                    classification = classification,
                    extractedData = data,
                    confidence = data.confidence,
                    warnings = warnings,
                    rawText = data.extractedText ?: ""
                )
            }

            ClassifiedDocumentType.RECEIPT -> {
                logger.debug("Extracting as receipt")
                val data = receiptAgent.extract(images)
                DocumentAIResult.Receipt(
                    classification = classification,
                    extractedData = data,
                    confidence = data.confidence,
                    warnings = warnings,
                    rawText = data.extractedText ?: ""
                )
            }

            ClassifiedDocumentType.UNKNOWN -> {
                logger.warn("Could not classify document type")
                warnings.add("Document type could not be determined")
                DocumentAIResult.Unknown(
                    classification = classification,
                    confidence = classification.confidence,
                    warnings = warnings,
                    rawText = ""
                )
            }
        }
    }

    /**
     * Classify document without extraction.
     * Useful for quick categorization or when extraction is not needed.
     *
     * @param images List of document page images
     * @return Document classification result
     */
    suspend fun classifyDocument(images: List<DocumentImage>): Result<DocumentClassification> = runCatching {
        classificationAgent.classify(images)
    }

    /**
     * Direct invoice extraction (skip classification).
     * Use when you know the document is an invoice.
     *
     * @param images List of document page images
     * @return Extracted invoice data
     */
    suspend fun extractInvoice(images: List<DocumentImage>): Result<ExtractedInvoiceData> = runCatching {
        invoiceAgent.extract(images)
    }

    /**
     * Direct receipt extraction (skip classification).
     * Use when you know the document is a receipt.
     *
     * @param images List of document page images
     * @return Extracted receipt data
     */
    suspend fun extractReceipt(images: List<DocumentImage>): Result<ExtractedReceiptData> = runCatching {
        receiptAgent.extract(images)
    }

    /**
     * Direct bill extraction (skip classification).
     * Use when you know the document is a bill (supplier invoice).
     *
     * @param images List of document page images
     * @return Extracted bill data
     */
    suspend fun extractBill(images: List<DocumentImage>): Result<ExtractedBillData> = runCatching {
        billAgent.extract(images)
    }

    /**
     * Suggest expense category for a description.
     *
     * @param description The expense description
     * @param merchantName Optional merchant name for context
     * @return Category suggestion with confidence
     */
    suspend fun suggestCategory(
        description: String,
        merchantName: String? = null
    ): Result<CategorySuggestion> = runCatching {
        categoryAgent.suggest(description, merchantName)
    }

    /**
     * Check if the AI service is properly configured and ready.
     * Always true since Ollama is the only provider.
     */
    fun isConfigured(): Boolean = true

    /**
     * Get the current configuration summary.
     */
    fun getConfigSummary(): String {
        return "Mode: ${config.mode}, Ollama: ${config.ollamaHost}, " +
            "Extraction: ${config.getModel(ModelPurpose.DOCUMENT_EXTRACTION)}, " +
            "Provenance: ${config.isProvenanceEnabled()}"
    }
}
