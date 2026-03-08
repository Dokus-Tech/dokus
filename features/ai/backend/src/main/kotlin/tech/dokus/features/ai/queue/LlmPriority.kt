package tech.dokus.features.ai.queue

/**
 * Priority for LLM requests. Lower ordinal = higher priority.
 * User-interactive work always preempts background work.
 */
enum class LlmPriority {
    /** User is waiting for a response (chat, interactive extraction). */
    Interactive,

    /** Important but not user-blocking (document processing). */
    Standard,

    /** Background enrichment, can wait indefinitely. */
    Background,
}
