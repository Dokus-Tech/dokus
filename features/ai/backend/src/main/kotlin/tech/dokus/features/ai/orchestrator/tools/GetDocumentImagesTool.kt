package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.services.DocumentImageService

fun interface DocumentImageFetcher {
    suspend operator fun invoke(documentId: String): GetDocumentImagesTool.DocumentData?
}

/**
 * Tool for converting documents to images for vision model processing.
 *
 * Converts PDFs and images to PNG format suitable for vision models.
 * Returns cache-backed image IDs that can be passed to vision tools.
 */
class GetDocumentImagesTool(
    private val documentImageService: DocumentImageService,
    private val documentFetcher: DocumentImageFetcher,
    private val imageCache: DocumentImageCache,
    private val traceSink: tech.dokus.features.ai.orchestrator.ToolTraceSink? = null
) : SimpleTool<GetDocumentImagesTool.Args>(
    argsSerializer = Args.serializer(),
    name = "get_document_images",
    description = """
        Converts a document (PDF or image) to PNG images for vision processing.

        Use this tool first to get the document images before classification or extraction.
        Returns opaque image IDs for each page (cache-backed).

        For PDFs, renders up to 10 pages at 150 DPI by default.
        You can override with maxPages and dpi.
        For images, converts to PNG format.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The document ID to convert to images")
        val documentId: String,

        @property:LLMDescription("Optional max pages to render (default: 10)")
        val maxPages: Int? = null,

        @property:LLMDescription("Optional DPI for PDF rendering (default: 150)")
        val dpi: Int? = null
    )

    /**
     * Document data fetched from storage.
     */
    data class DocumentData(
        val bytes: ByteArray,
        val mimeType: String
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is DocumentData) return false
            return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }

    override suspend fun execute(args: Args): String {
        val documentData = documentFetcher(args.documentId)
            ?: return "ERROR: Document not found: ${args.documentId}"

        return try {
            val start = kotlin.time.TimeSource.Monotonic.markNow()
            val result = documentImageService.getDocumentImages(
                documentBytes = documentData.bytes,
                mimeType = documentData.mimeType,
                maxPages = args.maxPages ?: 10,
                dpi = args.dpi ?: 150
            )

            val refs = imageCache.store(
                documentId = args.documentId,
                runId = null,
                images = result.images
            )

            traceSink?.record(
                action = "convert_document_images",
                tool = name,
                durationMs = start.elapsedNow().inWholeMilliseconds,
                notes = "processedPages=${result.processedPages}, totalPages=${result.totalPages}"
            )

            // Return structured information about the images
            buildString {
                appendLine("SUCCESS: Converted document to ${result.processedPages} image(s)")
                appendLine("Total pages: ${result.totalPages}")
                appendLine("Processed pages: ${result.processedPages}")
                appendLine()
                appendLine("Images (cache IDs):")
                refs.forEach { ref ->
                    appendLine("Page ${ref.pageNumber}: ${ref.imageId}")
                }
            }
        } catch (e: Exception) {
            traceSink?.record(
                action = "convert_document_images",
                tool = name,
                durationMs = 0,
                notes = "error=${e.message}"
            )
            "ERROR: Failed to convert document: ${e.message}"
        }
    }
}
