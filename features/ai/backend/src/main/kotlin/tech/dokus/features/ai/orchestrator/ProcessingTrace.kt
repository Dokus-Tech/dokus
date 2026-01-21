package tech.dokus.features.ai.orchestrator

import kotlinx.serialization.json.JsonElement

/**
 * Lightweight sink for recording tool-level trace events during orchestration.
 */
fun interface ToolTraceSink {
    fun record(
        action: String,
        tool: String? = null,
        durationMs: Long = 0,
        input: JsonElement? = null,
        output: JsonElement? = null,
        notes: String? = null
    )
}

/**
 * In-memory collector for processing trace steps.
 */
class ProcessingTraceCollector : ToolTraceSink {
    private val steps = mutableListOf<ProcessingStep>()
    private var stepCounter = 1

    override fun record(
        action: String,
        tool: String?,
        durationMs: Long,
        input: JsonElement?,
        output: JsonElement?,
        notes: String?
    ) {
        steps += ProcessingStep.create(
            step = stepCounter++,
            action = action,
            tool = tool,
            durationMs = durationMs,
            input = input,
            output = output,
            notes = notes
        )
    }

    fun snapshot(): List<ProcessingStep> = steps.toList()
}
