package ai.dokus.processor.backend.worker

import ai.dokus.foundation.database.repository.processor.ProcessingItem
import ai.dokus.foundation.database.repository.processor.ProcessorDocumentProcessingRepository
import ai.dokus.foundation.ktor.storage.DocumentStorageService
import ai.dokus.processor.backend.extraction.AIExtractionProvider
import ai.dokus.processor.backend.extraction.ExtractionException
import ai.dokus.processor.backend.extraction.ExtractionProviderFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Background worker that polls for pending documents and processes them with AI extraction.
 *
 * Features:
 * - Polling-based processing (configurable interval)
 * - Automatic retry with backoff
 * - Graceful shutdown support
 * - Provider fallback if primary fails
 */
class DocumentProcessingWorker(
    private val processingRepository: ProcessorDocumentProcessingRepository,
    private val documentStorage: DocumentStorageService,
    private val providerFactory: ExtractionProviderFactory,
    private val config: WorkerConfig = WorkerConfig()
) {
    private val logger = LoggerFactory.getLogger(DocumentProcessingWorker::class.java)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var pollingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    /**
     * Start the processing worker.
     */
    fun start() {
        if (!isRunning.compareAndSet(false, true)) {
            logger.warn("Worker already running")
            return
        }

        logger.info("Starting document processing worker (interval=${config.pollingInterval}ms, batch=${config.batchSize})")

        pollingJob = scope.launch {
            while (isActive && isRunning.get()) {
                try {
                    processBatch()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    logger.error("Error in processing loop", e)
                }

                delay(config.pollingInterval)
            }
        }
    }

    /**
     * Stop the processing worker gracefully.
     */
    fun stop() {
        if (!isRunning.compareAndSet(true, false)) {
            logger.warn("Worker not running")
            return
        }

        logger.info("Stopping document processing worker...")
        pollingJob?.cancel()
        pollingJob = null
        logger.info("Document processing worker stopped")
    }

    /**
     * Process a batch of pending documents.
     */
    private suspend fun processBatch() {
        // Find pending documents
        val pending = processingRepository.findPendingForProcessing(
            limit = config.batchSize,
            maxAttempts = config.maxAttempts
        )

        if (pending.isEmpty()) {
            logger.debug("No pending documents to process")
            return
        }

        logger.info("Found ${pending.size} pending documents to process")

        for (processing in pending) {
            if (!isRunning.get()) break

            try {
                processDocument(processing)
            } catch (e: Exception) {
                logger.error("Failed to process document ${processing.documentId}", e)
            }
        }
    }

    /**
     * Process a single document.
     */
    private suspend fun processDocument(processing: ProcessingItem) {
        val documentId = processing.documentId
        val processingId = processing.processingId

        logger.info("Processing document: $documentId (attempt ${processing.attempts + 1})")

        // Get AI provider
        val provider = providerFactory.getFirstAvailableProvider()
        if (provider == null) {
            logger.error("No AI provider available for processing")
            processingRepository.markAsFailed(processingId, "No AI provider available")
            return
        }

        // Mark as processing
        processingRepository.markAsProcessing(processingId, provider.name)

        try {
            // Download document from storage
            val documentBytes = downloadDocument(processing.storageKey)

            // Run AI extraction
            val result = provider.extract(
                documentBytes = documentBytes,
                contentType = processing.contentType,
                filename = processing.filename
            )

            // Update with results
            processingRepository.markAsProcessed(
                processingId = processingId,
                documentType = result.documentType,
                extractedData = result.extractedData,
                confidence = result.confidence,
                rawText = result.rawText
            )

            logger.info("Successfully processed document $documentId: type=${result.documentType}, confidence=${result.confidence}")

        } catch (e: ExtractionException) {
            logger.error("Extraction failed for document $documentId: ${e.message}")

            if (e.isRetryable && processing.attempts < config.maxAttempts - 1) {
                // Will be retried on next poll
                processingRepository.markAsFailed(processingId, "Extraction failed: ${e.message}")
            } else {
                // Final failure
                processingRepository.markAsFailed(processingId, "Extraction failed (final): ${e.message}")
            }

            // Try fallback provider if available
            tryFallbackProvider(processing, provider.name, e)

        } catch (e: Exception) {
            logger.error("Unexpected error processing document $documentId", e)
            processingRepository.markAsFailed(processingId, "Processing error: ${e.message}")
        }
    }

    /**
     * Try a fallback provider after primary failure.
     */
    private suspend fun tryFallbackProvider(
        processing: ProcessingItem,
        failedProvider: String,
        originalError: ExtractionException
    ) {
        if (!originalError.isRetryable) return

        val providers = providerFactory.getAvailableProviders()
            .filter { it.name != failedProvider }

        for (provider in providers) {
            try {
                logger.info("Trying fallback provider: ${provider.name}")

                processingRepository.markAsProcessing(processing.processingId, provider.name)

                val documentBytes = downloadDocument(processing.storageKey)

                val result = provider.extract(
                    documentBytes = documentBytes,
                    contentType = processing.contentType,
                    filename = processing.filename
                )

                processingRepository.markAsProcessed(
                    processingId = processing.processingId,
                    documentType = result.documentType,
                    extractedData = result.extractedData,
                    confidence = result.confidence,
                    rawText = result.rawText
                )

                logger.info("Fallback provider ${provider.name} succeeded for document ${processing.documentId}")
                return

            } catch (e: Exception) {
                logger.warn("Fallback provider ${provider.name} also failed: ${e.message}")
            }
        }
    }

    /**
     * Download document from object storage.
     */
    private suspend fun downloadDocument(storageKey: String): ByteArray {
        return try {
            documentStorage.downloadDocument(storageKey)
        } catch (e: Exception) {
            throw ExtractionException(
                "Failed to download document from storage: ${e.message}",
                "storage",
                isRetryable = true,
                cause = e
            )
        }
    }
}

/**
 * Configuration for the processing worker.
 */
data class WorkerConfig(
    val pollingInterval: Long = 5000L,
    val batchSize: Int = 10,
    val maxAttempts: Int = 3
)
