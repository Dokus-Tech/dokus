package ai.dokus.ai.service

import ai.dokus.ai.agents.CategorySuggestionAgent
import ai.dokus.ai.agents.DocumentClassificationAgent
import ai.dokus.ai.agents.InvoiceExtractionAgent
import ai.dokus.ai.agents.ReceiptExtractionAgent
import ai.dokus.ai.config.AIConfig
import ai.dokus.ai.config.AIProviderFactory
import ai.dokus.ai.config.ModelPurpose
import ai.dokus.ai.models.CategorySuggestion
import ai.dokus.ai.models.ClassifiedDocumentType
import ai.dokus.ai.models.DocumentClassification
import ai.dokus.ai.models.DocumentProcessingResult
import ai.dokus.ai.models.ExtractedDocumentData
import ai.dokus.ai.models.ExtractedInvoiceData
import ai.dokus.ai.models.ExtractedReceiptData
import org.slf4j.LoggerFactory

/**
 * High-level AI service that orchestrates document processing agents.
 *
 * Provides a clean API for:
 * - Two-step document processing (classify then extract)
 * - Direct extraction (when type is known)
 * - Category suggestions for expenses
 */
class AIService(
    private val config: AIConfig
) {
    private val logger = LoggerFactory.getLogger(AIService::class.java)

    // Create executor once for all agents
    private val executor by lazy {
        logger.info("Initializing AI executor: provider=${config.defaultProvider}")
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

    // Step 2b: Receipt extraction agent
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
     * 1. Classify the document type
     * 2. Extract data using the appropriate agent
     *
     * @param ocrText The OCR text from the document
     * @return Result containing classification and extracted data
     */
    suspend fun processDocument(ocrText: String): Result<DocumentProcessingResult> = runCatching {
        logger.info("Processing document (${ocrText.length} chars)")

        // Step 1: Classify document type
        val classification = classificationAgent.classify(ocrText)
        logger.info("Document classified as ${classification.documentType} (confidence: ${classification.confidence})")

        // Step 2: Extract based on classification
        val extractedData: ExtractedDocumentData = when (classification.documentType) {
            ClassifiedDocumentType.INVOICE -> {
                logger.debug("Extracting as invoice")
                val data = invoiceAgent.extract(ocrText)
                ExtractedDocumentData.Invoice(data)
            }
            ClassifiedDocumentType.RECEIPT -> {
                logger.debug("Extracting as receipt")
                val data = receiptAgent.extract(ocrText)
                ExtractedDocumentData.Receipt(data)
            }
            ClassifiedDocumentType.BILL -> {
                // Bills use invoice extraction (similar structure)
                logger.debug("Extracting as bill (using invoice structure)")
                val data = invoiceAgent.extract(ocrText)
                ExtractedDocumentData.Bill(data)
            }
            ClassifiedDocumentType.UNKNOWN -> {
                logger.warn("Could not classify document type")
                throw IllegalArgumentException("Could not classify document type. Classification confidence too low.")
            }
        }

        DocumentProcessingResult(
            classification = classification,
            extractedData = extractedData
        )
    }

    /**
     * Classify document without extraction.
     * Useful for quick categorization or when extraction is not needed.
     *
     * @param ocrText The OCR text from the document
     * @return Document classification result
     */
    suspend fun classifyDocument(ocrText: String): Result<DocumentClassification> = runCatching {
        classificationAgent.classify(ocrText)
    }

    /**
     * Direct invoice extraction (skip classification).
     * Use when you know the document is an invoice.
     *
     * @param ocrText The OCR text from the document
     * @return Extracted invoice data
     */
    suspend fun extractInvoice(ocrText: String): Result<ExtractedInvoiceData> = runCatching {
        invoiceAgent.extract(ocrText)
    }

    /**
     * Direct receipt extraction (skip classification).
     * Use when you know the document is a receipt.
     *
     * @param ocrText The OCR text from the document
     * @return Extracted receipt data
     */
    suspend fun extractReceipt(ocrText: String): Result<ExtractedReceiptData> = runCatching {
        receiptAgent.extract(ocrText)
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
     */
    fun isConfigured(): Boolean {
        return when (config.defaultProvider) {
            AIConfig.AIProvider.OLLAMA -> config.ollama.enabled
            AIConfig.AIProvider.OPENAI -> config.openai.enabled && config.openai.apiKey.isNotBlank()
        }
    }

    /**
     * Get the current configuration summary.
     */
    fun getConfigSummary(): String {
        return buildString {
            append("Provider: ${config.defaultProvider}")
            when (config.defaultProvider) {
                AIConfig.AIProvider.OLLAMA -> {
                    append(", URL: ${config.ollama.baseUrl}")
                    append(", Model: ${config.ollama.defaultModel}")
                }
                AIConfig.AIProvider.OPENAI -> {
                    append(", Model: ${config.openai.defaultModel}")
                    append(", API Key: ${if (config.openai.apiKey.isNotBlank()) "configured" else "missing"}")
                }
            }
        }
    }
}
