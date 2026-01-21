package tech.dokus.features.ai.orchestrator

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Represents a single step in document processing for audit trail.
 *
 * Each processing step captures:
 * - What action was taken
 * - Which tool was called (if any)
 * - Timing information
 * - Input/output for debugging
 */
@Serializable
data class ProcessingStep(
    /** Step number in the processing sequence (1-indexed) */
    val step: Int,

    /** Human-readable description of the action taken */
    val action: String,

    /** Name of the tool that was called, if any */
    val tool: String? = null,

    /** When this step started */
    val timestamp: Instant,

    /** How long this step took in milliseconds */
    val durationMs: Long,

    /** Input parameters passed to the tool (for debugging) */
    val input: JsonElement? = null,

    /** Output from the tool (for debugging) */
    val output: JsonElement? = null,

    /** Additional notes or context about this step */
    val notes: String? = null
) {
    companion object {
        /**
         * Create a processing step with the current timestamp.
         */
        fun create(
            step: Int,
            action: String,
            tool: String? = null,
            durationMs: Long = 0,
            input: JsonElement? = null,
            output: JsonElement? = null,
            notes: String? = null
        ): ProcessingStep = ProcessingStep(
            step = step,
            action = action,
            tool = tool,
            timestamp = kotlinx.datetime.Clock.System.now(),
            durationMs = durationMs,
            input = input,
            output = output,
            notes = notes
        )
    }
}
