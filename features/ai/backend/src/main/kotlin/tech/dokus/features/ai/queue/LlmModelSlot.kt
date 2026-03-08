package tech.dokus.features.ai.queue

/**
 * Identifies a hardware concurrency unit (model slot).
 *
 * Requests targeting the same slot are serialized (or concurrency-limited).
 * Requests targeting different slots proceed in parallel.
 */
@JvmInline
value class LlmModelSlot(val value: String) {
    companion object {
        /** Text model slot (orchestrator + chat). */
        val Text = LlmModelSlot("text")

        /** Vision-language model slot. */
        val Vision = LlmModelSlot("vision")

        /** Embedding model slot. */
        val Embedding = LlmModelSlot("embedding")
    }
}
