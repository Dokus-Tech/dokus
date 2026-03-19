package tech.dokus.features.ai.graph.nodes

import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.Dpi
import tech.dokus.features.ai.services.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.tools.DocumentImagesFetcherTool

interface InputWithDocumentId {
    val documentId: DocumentId
    val maxPagesOverride: Int? get() = null
    val dpiOverride: Dpi? get() = null
}

/** Maximum number of CSV lines to inject into the prompt for classification + column mapping. */
private const val CSV_PREVIEW_LINE_COUNT = 50

/** Graph storage key for raw CSV bytes (used by CSV extraction subgraph). */
const val CSV_BYTES_STORAGE_KEY = "csv-raw-bytes"

/** Graph storage key for document content type (used to route CSV extraction). */
const val CONTENT_TYPE_STORAGE_KEY = "document-content-type"

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.documentImagesInjectorNode(
    fetcher: DocumentFetcher,
): AIAgentNodeDelegate<Input, Input> where Input : InputWithDocumentId, Input : InputWithTenantContext {
    return node<Input, Input>("inject-document-images") { args ->
        val document = fetcher(args.tenant.id, args.documentId).getOrElse {
            llm.writeSession {
                DocumentImagesFetcherTool.Output.Failure(it.localizedMessage)
            }
            return@node args
        }

        if (document.mimeType == "text/csv") {
            // CSV: store raw bytes for extraction, inject text preview for classification
            val csvBytesKey = createStorageKey<ByteArray>(CSV_BYTES_STORAGE_KEY)
            val contentTypeKey = createStorageKey<String>(CONTENT_TYPE_STORAGE_KEY)
            storage.set(csvBytesKey, document.bytes)
            storage.set(contentTypeKey, document.mimeType)

            val csvText = String(document.bytes, Charsets.UTF_8)
            val preview = csvText.lines().take(CSV_PREVIEW_LINE_COUNT).joinToString("\n")
            llm.writeSession {
                appendPrompt {
                    user {
                        text("CSV file content (first $CSV_PREVIEW_LINE_COUNT lines):\n```\n$preview\n```")
                    }
                }
            }
        } else {
            // PDF/image: render pages and inject as images (unchanged)
            val images = DocumentImageService.getDocumentImages(
                documentBytes = document.bytes,
                mimeType = document.mimeType,
                pageCount = args.maxPagesOverride ?: DocumentImageService.DEFAULT_PAGE_COUNT,
                dpi = args.dpiOverride ?: Dpi.default,
            )
            llm.writeSession {
                appendPrompt {
                    user {
                        images.images.forEach { image ->
                            image(
                                ContentPart.Image(
                                    content = AttachmentContent.Binary.Bytes(image.imageBytes),
                                    format = "png",
                                    mimeType = image.mimeType,
                                )
                            )
                        }
                    }
                }
            }
        }
        args
    }
}
