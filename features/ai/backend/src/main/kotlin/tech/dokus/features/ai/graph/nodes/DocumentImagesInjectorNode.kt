package tech.dokus.features.ai.graph.nodes

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.ContentPart
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.ai.orchestrator.DocumentFetcher
import tech.dokus.features.ai.services.DocumentImageService
import tech.dokus.features.ai.tools.DocumentImagesFetcherTool

internal interface InputWithDocumentId {
    val documentId: DocumentId
}

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.documentImagesInjectorNode(
    fetcher: DocumentFetcher,
): AIAgentNodeDelegate<Input, Input> where Input : InputWithDocumentId, Input : InputWithTenantContext {
    return node<Input, Input> { args ->
        val document = fetcher(args.tenant.id, args.documentId).getOrElse {
            llm.writeSession {
                DocumentImagesFetcherTool.Output.Failure(it.localizedMessage)
            }
            return@node args
        }

        val images = DocumentImageService.getDocumentImages(
            document.bytes,
            document.mimeType,
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