package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageService
import kotlin.uuid.ExperimentalUuidApi

/**
 * Should not be used as it returns bytearray, which is not correct
 * Kept just as an example for clean tool architecture
 */
class DocumentImagesFetcherTool(
    private val tenantId: TenantId,
    private val fetcher: DocumentFetcher,
    private val documentImageService: DocumentImageService,
) : Tool<DocumentImagesFetcherTool.Input, DocumentImagesFetcherTool.Output>(
    argsSerializer = Input.serializer(),
    resultSerializer = Output.serializer(),
    name = "document_fetcher_tool",
    description = """
            Retrieves document pages as images for visual analysis.
            
            For large documents, fetch pages in batches:
            - First call: Use default (pages 1-3) to see document type and structure
            - If more pages needed: Call again with specific page range
            
            Returns images + metadata about total pages available.
        """.trimIndent()
) {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun execute(args: Input): Output {
        val document = fetcher(tenantId, DocumentId(args.documentId)).getOrElse {
            return Output.Failure(it.localizedMessage)
        }

        val images = documentImageService.getDocumentImages(
            document.bytes,
            document.mimeType,
            args.startPage,
            args.pageCount,
            args.dpi
        )
        return Output.DocumentFound(
            images = images.images.map {
                Output.DocumentFound.DocumentImage(
                    it.imageBytes,
                    it.mimeType,
                    it.pageNumber
                )
            },
            totalPages = images.totalPages,
            processedPages = images.processedPages,
            hasMorePages = images.hasMorePages,
            nextPageStart = images.nextPageStart,
        )
    }


    @Serializable
    data class Input(
        @property:LLMDescription("The document ID to fetch")
        val documentId: String,
        @property:LLMDescription("First page to fetch, 1-indexed (default: 1)")
        val startPage: Int = 1,
        @property:LLMDescription("Number of pages to fetch (default: 3, max: 10)")
        val pageCount: Int = 3,
        @property:LLMDescription("DPI for rendering (default: 150)")
        val dpi: Int = 150,
    )


    @Serializable
    sealed interface Output {
        @Serializable
        data class DocumentFound(
            val images: List<DocumentImage>,
            @property:LLMDescription("Total pages in the document")
            val totalPages: Int,
            @property:LLMDescription("Number of pages returned in this response")
            val processedPages: Int,
            @property:LLMDescription("True if more pages available. Call again with nextPageStart if needed.")
            val hasMorePages: Boolean,
            @property:LLMDescription("Use as startPage in next call to continue fetching")
            val nextPageStart: Int? = null,
        ) : Output {
            @Serializable
            data class DocumentImage(
                val bytes: ByteArray,
                val mimeType: String,
                val pageNumber: Int,
            ) {
                override fun equals(other: Any?): Boolean {
                    if (this === other) return true
                    if (other !is DocumentFetcher.FetchedDocumentData) return false
                    return bytes.contentEquals(other.bytes) && mimeType == other.mimeType
                }

                override fun hashCode(): Int {
                    var result = bytes.contentHashCode()
                    result = 31 * result + mimeType.hashCode()
                    return result
                }
            }
        }

        @Serializable
        data class Failure(
            @property:LLMDescription("Why the fetch failed")
            val reason: String
        ) : Output

    }
}