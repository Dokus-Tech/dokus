package tech.dokus.features.ai.ensemble

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedExpenseData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import tech.dokus.features.ai.models.ExtractedReceiptData
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Layer 1: Perception Ensemble
 *
 * ## Ensemble vs Parallel Semantics (IMPORTANT!)
 *
 * Two distinct concepts:
 * - `enableEnsemble` (in IntelligenceMode): Run fast + expert models (true) or single model (false)
 * - `runParallel` (this class): Execute agents concurrently (true) or sequentially (false)
 *
 * Execution modes by configuration:
 *
 * | enableEnsemble | runParallel | Behavior |
 * |----------------|-------------|----------|
 * | false          | N/A         | Single model only (handled by coordinator) |
 * | true           | false       | Fast → Expert sequential (fallback strategy) |
 * | true           | true        | Fast || Expert parallel, then consensus |
 *
 * ## Rationale
 *
 * **Sequential ensemble** (runParallel=false):
 * - Run fast model first
 * - Only run expert if fast result is uncertain (low confidence or missing fields)
 * - Saves compute on obvious documents
 *
 * **Parallel ensemble** (runParallel=true):
 * - Run both models simultaneously
 * - Use consensus engine to merge results
 * - Higher throughput, higher resource usage
 *
 * @param T The type of extracted data (e.g., ExtractedInvoiceData)
 * @param runParallel If true, run both models in parallel. If false, run sequentially.
 * @param maxParallelAgents Maximum concurrent agent executions per document (from IntelligenceMode)
 */
class PerceptionEnsemble<T : Any>(
    private val fastAgent: ExtractionAgent<T>,
    private val expertAgent: ExtractionAgent<T>,
    private val runParallel: Boolean = true,
    private val maxParallelAgents: Int = 2
) {
    private val logger = loggerFor()

    /**
     * Run extraction using the configured strategy.
     *
     * Creates a per-document semaphore to limit concurrent agent executions.
     * This is critical: the semaphore must be per-document, not class-level,
     * to avoid limiting concurrency across unrelated documents.
     *
     * @param images List of document page images
     * @return EnsembleResult containing candidates and any errors
     */
    suspend fun extract(images: List<DocumentImage>): EnsembleResult<T> {
        // Create semaphore PER DOCUMENT CALL - not shared across documents
        val docSemaphore = Semaphore(maxParallelAgents)

        return if (runParallel) {
            extractParallel(images, docSemaphore)
        } else {
            extractSequential(images, docSemaphore)
        }
    }

    /**
     * Sequential extraction: fast first, expert only if needed.
     *
     * This saves compute on obvious documents where the fast model
     * produces high-confidence results.
     *
     * @param docSemaphore Per-document semaphore for concurrency control
     */
    private suspend fun extractSequential(
        images: List<DocumentImage>,
        docSemaphore: Semaphore
    ): EnsembleResult<T> {
        logger.info("Ensemble: sequential mode (fast → expert fallback), maxParallelAgents={}", maxParallelAgents)
        val startTime = System.currentTimeMillis()

        // Run fast model first (with permit)
        val fastResult = docSemaphore.withPermit {
            runCatching {
                logger.debug("Fast model: starting extraction")
                fastAgent.extract(images).also {
                    logger.debug("Fast model: completed")
                }
            }
        }

        // Check if we need expert
        val needsExpert = fastResult.isFailure ||
            fastResult.getOrNull()?.let { hasLowConfidence(it) } == true

        if (!needsExpert) {
            val elapsed = System.currentTimeMillis() - startTime
            logger.info("Ensemble completed in ${elapsed}ms (fast model sufficient, skipped expert)")
            return EnsembleResult(
                fastCandidate = fastResult.getOrNull(),
                expertCandidate = null,
                fastError = fastResult.exceptionOrNull(),
                expertError = null
            )
        }

        // Run expert model (with permit)
        logger.debug("Expert model: starting (fast model insufficient)")
        val expertResult = docSemaphore.withPermit {
            runCatching {
                expertAgent.extract(images).also {
                    logger.debug("Expert model: completed")
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        logger.info("Ensemble completed in ${elapsed}ms")
        logger.info("  Fast model: ${if (fastResult.isSuccess) "SUCCESS" else "FAILED"}")
        logger.info("  Expert model: ${if (expertResult.isSuccess) "SUCCESS" else "FAILED"}")

        return EnsembleResult(
            fastCandidate = fastResult.getOrNull(),
            expertCandidate = expertResult.getOrNull(),
            fastError = fastResult.exceptionOrNull(),
            expertError = expertResult.exceptionOrNull()
        )
    }

    /**
     * Parallel extraction: run both models simultaneously.
     *
     * Higher throughput but uses more resources (both models loaded).
     * Semaphore limits concurrent executions to maxParallelAgents.
     *
     * @param docSemaphore Per-document semaphore for concurrency control
     */
    private suspend fun extractParallel(
        images: List<DocumentImage>,
        docSemaphore: Semaphore
    ): EnsembleResult<T> = coroutineScope {
        logger.info("Ensemble: parallel mode (fast || expert), maxParallelAgents={}", maxParallelAgents)
        val startTime = System.currentTimeMillis()

        // Launch both extractions in parallel (semaphore limits actual concurrency)
        val fastDeferred = async {
            docSemaphore.withPermit {
                runCatching {
                    logger.debug("Fast model: starting extraction")
                    val result = fastAgent.extract(images)
                    logger.debug("Fast model: completed")
                    result
                }
            }
        }

        val expertDeferred = async {
            docSemaphore.withPermit {
                runCatching {
                    logger.debug("Expert model: starting extraction")
                    val result = expertAgent.extract(images)
                    logger.debug("Expert model: completed")
                    result
                }
            }
        }

        // Await both results
        val fastResult = fastDeferred.await()
        val expertResult = expertDeferred.await()

        val elapsed = System.currentTimeMillis() - startTime
        logger.info("Ensemble completed in ${elapsed}ms")
        logger.info("  Fast model: ${if (fastResult.isSuccess) "SUCCESS" else "FAILED"}")
        logger.info("  Expert model: ${if (expertResult.isSuccess) "SUCCESS" else "FAILED"}")

        EnsembleResult(
            fastCandidate = fastResult.getOrNull(),
            expertCandidate = expertResult.getOrNull(),
            fastError = fastResult.exceptionOrNull(),
            expertError = expertResult.exceptionOrNull()
        )
    }

    /**
     * Check if extraction result has low confidence.
     * Used in sequential mode to decide if expert model is needed.
     */
    private fun hasLowConfidence(result: T): Boolean {
        val threshold = 0.7
        val confidence = when (result) {
            is ExtractedInvoiceData -> result.confidence
            is ExtractedBillData -> result.confidence
            is ExtractedReceiptData -> result.confidence
            is ExtractedExpenseData -> result.confidence
            else -> 0.5
        }
        val isLow = confidence < threshold
        if (isLow) {
            logger.debug("Fast model confidence {} < threshold {}, will run expert", confidence, threshold)
        }
        return isLow
    }

    companion object {
        /**
         * Create an ensemble from two extraction agents.
         *
         * @param runParallel If true, run both models in parallel. If false, run sequentially.
         * @param maxParallelAgents Maximum concurrent agent executions per document (from IntelligenceMode)
         */
        fun <T : Any> of(
            fastAgent: ExtractionAgent<T>,
            expertAgent: ExtractionAgent<T>,
            runParallel: Boolean = true,
            maxParallelAgents: Int = 2
        ): PerceptionEnsemble<T> = PerceptionEnsemble(fastAgent, expertAgent, runParallel, maxParallelAgents)
    }
}
