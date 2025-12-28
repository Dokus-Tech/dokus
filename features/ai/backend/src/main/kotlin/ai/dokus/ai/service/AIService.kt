package ai.dokus.ai.service

import ai.dokus.ai.agents.BillExtractionAgent
import ai.dokus.ai.agents.CategorySuggestionAgent
import ai.dokus.ai.agents.DocumentClassificationAgent
import ai.dokus.ai.agents.InvoiceExtractionAgent
import ai.dokus.ai.agents.ReceiptExtractionAgent
import ai.dokus.ai.config.AIProviderFactory
import ai.dokus.ai.models.CategorySuggestion
import ai.dokus.ai.models.ClassifiedDocumentType
import ai.dokus.ai.models.DocumentAIResult
import ai.dokus.ai.models.DocumentClassification
import ai.dokus.ai.models.ExtractedBillData
import ai.dokus.ai.models.ExtractedInvoiceData
import ai.dokus.ai.models.ExtractedReceiptData
import tech.dokus.domain.model.ai.AiProvider
import tech.dokus.foundation.ktor.config.AIConfig
import tech.dokus.foundation.ktor.config.ModelPurpose
import tech.dokus.foundation.ktor.utils.loggerFor

/**
 * High-level AI service that orchestrates document processing agents.
 *
 * TEXT-FIRST: This service accepts only pre-extracted text (ocrText).
 * Text acquisition (PDF extraction + OCR fallback) is done by the caller (worker).
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
     * 1. Classify the document type
     * 2. Extract data using the appropriate agent
     *
     * TEXT-FIRST: Accepts only pre-extracted text.
     * The caller is responsible for text acquisition (PDF/OCR).
     *
     * @param ocrText The OCR/extracted text from the document
     * @return Result containing DocumentAIResult sealed class with:
     *         - classification
     *         - type-specific extracted payload with provenance
     *         - confidence and warnings
     */
    suspend fun processDocument(ocrText: String): Result<DocumentAIResult> = runCatching {
        logger.info("Processing document (${ocrText.length} chars)")
        val warnings = mutableListOf<String>()

        // Step 1: Classify document type
        val classification = classificationAgent.classify(ocrText)
        logger.info("Document classified as ${classification.documentType} (confidence: ${classification.confidence})")

        if (classification.confidence < 0.5) {
            warnings.add("Low classification confidence: ${classification.confidence}")
        }

        // Step 2: Extract based on classification
        when (classification.documentType) {
            ClassifiedDocumentType.INVOICE -> {
                logger.debug("Extracting as invoice")
                val data = invoiceAgent.extract(ocrText)
                DocumentAIResult.Invoice(
                    classification = classification,
                    extractedData = data,
                    confidence = data.confidence,
                    warnings = warnings,
                    rawText = ocrText
                )
            }

            ClassifiedDocumentType.BILL -> {
                logger.debug("Extracting as bill")
                val data = billAgent.extract(ocrText)
                DocumentAIResult.Bill(
                    classification = classification,
                    extractedData = data,
                    confidence = data.confidence,
                    warnings = warnings,
                    rawText = ocrText
                )
            }

            ClassifiedDocumentType.RECEIPT -> {
                logger.debug("Extracting as receipt")
                val data = receiptAgent.extract(ocrText)
                DocumentAIResult.Receipt(
                    classification = classification,
                    extractedData = data,
                    confidence = data.confidence,
                    warnings = warnings,
                    rawText = ocrText
                )
            }

            ClassifiedDocumentType.UNKNOWN -> {
                logger.warn("Could not classify document type")
                warnings.add("Document type could not be determined")
                DocumentAIResult.Unknown(
                    classification = classification,
                    confidence = classification.confidence,
                    warnings = warnings,
                    rawText = ocrText
                )
            }
        }
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
     * Direct bill extraction (skip classification).
     * Use when you know the document is a bill (supplier invoice).
     *
     * @param ocrText The OCR text from the document
     * @return Extracted bill data
     */
    suspend fun extractBill(ocrText: String): Result<ExtractedBillData> = runCatching {
        billAgent.extract(ocrText)
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
            AiProvider.Ollama -> config.ollama.enabled
            AiProvider.OpenAi -> config.openai.enabled && config.openai.apiKey.isNotBlank()
        }
    }

    /**
     * Get the current configuration summary.
     */
    fun getConfigSummary(): String {
        return buildString {
            append("Provider: ${config.defaultProvider}")
            when (config.defaultProvider) {
                AiProvider.Ollama -> {
                    append(", URL: ${config.ollama.baseUrl}")
                    append(", Model: ${config.ollama.defaultModel}")
                }

                AiProvider.OpenAi -> {
                    append(", Model: ${config.openai.defaultModel}")
                    append(", API Key: ${if (config.openai.apiKey.isNotBlank()) "configured" else "missing"}")
                }
            }
        }
    }
}
