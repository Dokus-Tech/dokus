package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import kotlin.uuid.ExperimentalUuidApi

internal class DocumentFetcherTool(
    private val tenantId: TenantId,
    private val fetcher: DocumentFetcher
) : Tool<DocumentFetcherTool.Input, DocumentFetcherTool.Output>(
    argsSerializer = Input.serializer(),
    resultSerializer = Output.serializer(),
    name = "document_fetcher_tool",
    description = """
            Retrieves a document's raw content by ID.
            Returns the document bytes and MIME type (image/png, image/jpeg, application/pdf).
            Returns an error if the document is not found or inaccessible.
        """.trimIndent()
) {

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun execute(args: Input): Output {
        return fetcher(tenantId, DocumentId(args.documentId)).fold(
            onSuccess = {
                Output.DocumentFound(it.bytes, it.mimeType)
            },
            onFailure = {
                Output.Failure(it.localizedMessage)
            }
        )
    }


    @Serializable
    data class Input(
        @property:LLMDescription("The document ID to fetch")
        val documentId: String,
    )


    @Serializable
    sealed interface Output {
        @Serializable
        data class DocumentFound(
            val bytes: ByteArray,
            val mimeType: String
        ) : Output {
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

        @Serializable
        data class Failure(val reason: String) : Output

    }
}