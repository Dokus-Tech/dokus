package tech.dokus.features.ai.graph.nodes

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
        args
    }
}
