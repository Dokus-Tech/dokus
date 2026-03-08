package tech.dokus.features.ai.queue

/**
 * Named lane for LLM requests.
 *
 * Each lane encapsulates its target [LlmModelSlot] and [LlmPriority],
 * so callers don't need to repeat them at every submit site.
 */
sealed class LlmLane(
    val label: String,
    val slot: LlmModelSlot,
    val defaultPriority: LlmPriority,
) {
    /** Document classification + financial extraction (vision model). */
    data object DocumentProcessing : LlmLane(
        label = "document-processing",
        slot = LlmModelSlot.Vision,
        defaultPriority = LlmPriority.Standard,
    )

    /** RAG-powered document Q&A (chat/text model). */
    data object Chat : LlmLane(
        label = "chat",
        slot = LlmModelSlot.Text,
        defaultPriority = LlmPriority.Interactive,
    )

    /** Business profile enrichment + logo discovery (text model). */
    data object BusinessEnrichment : LlmLane(
        label = "business-enrichment",
        slot = LlmModelSlot.Text,
        defaultPriority = LlmPriority.Background,
    )
}
