package tech.dokus.features.ai.ensemble

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.dokus.features.ai.agents.ExtractionAgent
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import tech.dokus.foundation.backend.utils.loggerFor

/**
 * Layer 1: Perception Ensemble
 *
 * Runs multiple extraction models in parallel and returns both results.
 * This enables the Consensus Engine (Layer 2) to compare and resolve conflicts.
 *
 * ## Performance
 * On M4 Max with 128GB RAM, both models can be kept loaded simultaneously.
 * Parallel execution means total time is approximately max(fast, expert) instead of sum.
 *
 * ## Model Configuration
 * - Fast model: Smaller, quicker extraction (e.g., qwen3-vl:8b)
 * - Expert model: Larger, more accurate extraction (e.g., qwen3-vl:72b)
 *
 * @param T The type of extracted data (e.g., ExtractedInvoiceData)
 */
class PerceptionEnsemble<T : Any>(
    private val fastAgent: ExtractionAgent<T>,
    private val expertAgent: ExtractionAgent<T>
) {
    private val logger = loggerFor()

    /**
     * Run both extraction models in parallel on the same document images.
     *
     * @param images List of document page images
     * @return EnsembleResult containing both candidates and any errors
     */
    suspend fun extract(images: List<DocumentImage>): EnsembleResult<T> = coroutineScope {
        logger.info("Starting perception ensemble extraction (${images.size} pages)")
        val startTime = System.currentTimeMillis()

        // Launch both extractions in parallel
        val fastDeferred = async {
            runCatching {
                logger.debug("Fast model: starting extraction")
                val result = fastAgent.extract(images)
                logger.debug("Fast model: completed")
                result
            }
        }

        val expertDeferred = async {
            runCatching {
                logger.debug("Expert model: starting extraction")
                val result = expertAgent.extract(images)
                logger.debug("Expert model: completed")
                result
            }
        }

        // Await both results
        val fastResult = fastDeferred.await()
        val expertResult = expertDeferred.await()

        val elapsed = System.currentTimeMillis() - startTime
        logger.info("Perception ensemble completed in ${elapsed}ms")
        logger.info("  Fast model: ${if (fastResult.isSuccess) "SUCCESS" else "FAILED"}")
        logger.info("  Expert model: ${if (expertResult.isSuccess) "SUCCESS" else "FAILED"}")

        EnsembleResult(
            fastCandidate = fastResult.getOrNull(),
            expertCandidate = expertResult.getOrNull(),
            fastError = fastResult.exceptionOrNull(),
            expertError = expertResult.exceptionOrNull()
        )
    }

    companion object {
        /**
         * Create an ensemble from two extraction agents.
         */
        fun <T : Any> of(
            fastAgent: ExtractionAgent<T>,
            expertAgent: ExtractionAgent<T>
        ): PerceptionEnsemble<T> = PerceptionEnsemble(fastAgent, expertAgent)
    }
}
