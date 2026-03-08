package tech.dokus.features.ai.queue

/** Submit document processing work (vision model, standard priority). */
suspend fun <T> LlmQueue.documentProcessing(
    description: String = "",
    work: suspend () -> T,
): T = submit(LlmLane.DocumentProcessing, description, work)

/** Submit chat work (text model, interactive priority). */
suspend fun <T> LlmQueue.chat(
    description: String = "",
    work: suspend () -> T,
): T = submit(LlmLane.Chat, description, work)

/** Submit business enrichment work (text model, background priority). */
suspend fun <T> LlmQueue.businessEnrichment(
    description: String = "",
    work: suspend () -> T,
): T = submit(LlmLane.BusinessEnrichment, description, work)
