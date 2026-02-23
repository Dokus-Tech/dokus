package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourceContentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourcePagesUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewPreview(
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val getDocumentSourcePages: GetDocumentSourcePagesUseCase,
    private val getDocumentSourceContent: GetDocumentSourceContentUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleLoadPreviewPages() {
        withState<DocumentReviewState.Content, _> {
            loadPreviewPages(
                documentId = documentId,
                contentType = document.document.contentType,
                dpi = PreviewConfig.dpi.value,
                maxPages = PreviewConfig.DEFAULT_MAX_PAGES
            )
        }
        withState<DocumentReviewState.AwaitingExtraction, _> {
            loadPreviewPagesForAwaiting(
                documentId = documentId,
                contentType = document.document.contentType,
                dpi = PreviewConfig.dpi.value,
                maxPages = PreviewConfig.DEFAULT_MAX_PAGES
            )
        }
    }

    suspend fun DocumentReviewCtx.handleLoadMorePages(maxPages: Int) {
        withState<DocumentReviewState.Content, _> {
            loadPreviewPages(
                documentId = documentId,
                contentType = document.document.contentType,
                dpi = PreviewConfig.dpi.value,
                maxPages = maxPages
            )
        }
    }

    suspend fun DocumentReviewCtx.handleOpenSourceModal(sourceId: DocumentSourceId) {
        withState<DocumentReviewState.Content, _> {
            val source = document.sources.firstOrNull { it.id == sourceId } ?: return@withState
            updateState {
                copy(
                    sourceViewerState = SourceEvidenceViewerState(
                        sourceId = source.id,
                        sourceName = source.filename ?: source.sourceChannel.name,
                        sourceType = source.sourceChannel,
                        sourceReceivedAt = source.arrivalAt,
                        previewState = DocumentPreviewState.Loading,
                    )
                )
            }
            launch {
                loadSelectedSourcePreview(
                    documentId = documentId,
                    sourceId = source.id,
                    sourceType = source.sourceChannel,
                    contentType = source.contentType.orEmpty(),
                    dpi = PreviewConfig.dpi.value,
                    maxPages = PreviewConfig.DEFAULT_MAX_PAGES,
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleCloseSourceModal() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(sourceViewerState = null) }
        }
    }

    suspend fun DocumentReviewCtx.handleToggleSourceTechnicalDetails() {
        withState<DocumentReviewState.Content, _> {
            val viewer = sourceViewerState ?: return@withState
            val next = !viewer.isTechnicalDetailsExpanded
            updateState {
                copy(
                    sourceViewerState = viewer.copy(
                        isTechnicalDetailsExpanded = next
                    )
                )
            }
            if (!next || viewer.rawContent != null || viewer.isLoadingRawContent) {
                return@withState
            }
            launch {
                loadSourceRawContent(documentId, viewer.sourceId)
            }
        }
    }

    private suspend fun DocumentReviewCtx.loadSelectedSourcePreview(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        sourceType: DocumentSource,
        contentType: String,
        dpi: Int,
        maxPages: Int
    ) {
        val isPdf = contentType.contains("pdf", ignoreCase = true)
        if (!isPdf) {
            withState<DocumentReviewState.Content, _> {
                val viewer = sourceViewerState ?: return@withState
                updateState {
                    copy(
                        sourceViewerState = viewer.copy(
                            previewState = if (sourceType == DocumentSource.Peppol) {
                                DocumentPreviewState.NoPreview
                            } else {
                                DocumentPreviewState.NotPdf
                            }
                        )
                    )
                }
            }
            return
        }

        getDocumentSourcePages(documentId, sourceId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
                    withState<DocumentReviewState.Content, _> {
                        val viewer = sourceViewerState ?: return@withState
                        updateState {
                            copy(
                                sourceViewerState = viewer.copy(
                                    previewState = if (response.pages.isEmpty()) {
                                        DocumentPreviewState.NoPreview
                                    } else {
                                        DocumentPreviewState.Ready(
                                            pages = response.pages,
                                            totalPages = response.totalPages,
                                            renderedPages = response.renderedPages,
                                            dpi = response.dpi,
                                            hasMore = response.totalPages > response.renderedPages
                                        )
                                    }
                                )
                            )
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load source preview pages for source=$sourceId" }
                    val exception = error.asDokusException
                    withState<DocumentReviewState.Content, _> {
                        val viewer = sourceViewerState ?: return@withState
                        updateState {
                            copy(
                                sourceViewerState = viewer.copy(
                                    previewState = DocumentPreviewState.Error(
                                        exception = if (exception is DokusException.Unknown) {
                                            DokusException.DocumentPreviewLoadFailed
                                        } else {
                                            exception
                                        },
                                        retry = { intent(DocumentReviewIntent.OpenSourceModal(sourceId)) }
                                    )
                                )
                            )
                        }
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.loadSourceRawContent(
        documentId: DocumentId,
        sourceId: DocumentSourceId
    ) {
        withState<DocumentReviewState.Content, _> {
            val viewer = sourceViewerState ?: return@withState
            updateState {
                copy(
                    sourceViewerState = viewer.copy(
                        isLoadingRawContent = true,
                        rawContentError = null,
                    )
                )
            }
        }

        getDocumentSourceContent(documentId, sourceId).fold(
            onSuccess = { bytes ->
                withState<DocumentReviewState.Content, _> {
                    val viewer = sourceViewerState ?: return@withState
                    val raw = runCatching { bytes.decodeToString() }.getOrNull()
                    updateState {
                        copy(
                            sourceViewerState = viewer.copy(
                                rawContent = raw,
                                isLoadingRawContent = false,
                                rawContentError = null,
                            )
                        )
                    }
                }
            },
            onFailure = { error ->
                logger.e(error) { "Failed to load source raw content for source=$sourceId" }
                withState<DocumentReviewState.Content, _> {
                    val viewer = sourceViewerState ?: return@withState
                    updateState {
                        copy(
                            sourceViewerState = viewer.copy(
                                isLoadingRawContent = false,
                                rawContentError = error.asDokusException,
                            )
                        )
                    }
                }
            }
        )
    }

    private suspend fun DocumentReviewCtx.loadPreviewPages(
        documentId: DocumentId,
        contentType: String,
        dpi: Int,
        maxPages: Int
    ) {
        if (!contentType.contains("pdf", ignoreCase = true)) {
            withState<DocumentReviewState.Content, _> {
                updateState { copy(previewState = DocumentPreviewState.NotPdf) }
            }
            return
        }

        withState<DocumentReviewState.Content, _> {
            updateState { copy(previewState = DocumentPreviewState.Loading) }
        }

        getDocumentPages(documentId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
                    withState<DocumentReviewState.Content, _> {
                        if (response.pages.isEmpty()) {
                            updateState { copy(previewState = DocumentPreviewState.NoPreview) }
                        } else {
                            updateState {
                                copy(
                                    previewState = DocumentPreviewState.Ready(
                                        pages = response.pages,
                                        totalPages = response.totalPages,
                                        renderedPages = response.renderedPages,
                                        dpi = response.dpi,
                                        hasMore = response.totalPages > response.renderedPages
                                    )
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load preview pages for document: $documentId" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.DocumentPreviewLoadFailed
                    } else {
                        exception
                    }
                    withState<DocumentReviewState.Content, _> {
                        updateState {
                            copy(
                                previewState = DocumentPreviewState.Error(
                                    exception = displayException,
                                    retry = { intent(DocumentReviewIntent.RetryLoadPreview) }
                                ),
                            )
                        }
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.loadPreviewPagesForAwaiting(
        documentId: DocumentId,
        contentType: String,
        dpi: Int,
        maxPages: Int
    ) {
        if (!contentType.contains("pdf", ignoreCase = true)) {
            withState<DocumentReviewState.AwaitingExtraction, _> {
                updateState { copy(previewState = DocumentPreviewState.NotPdf) }
            }
            return
        }

        withState<DocumentReviewState.AwaitingExtraction, _> {
            updateState { copy(previewState = DocumentPreviewState.Loading) }
        }

        getDocumentPages(documentId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
                    withState<DocumentReviewState.AwaitingExtraction, _> {
                        if (response.pages.isEmpty()) {
                            updateState { copy(previewState = DocumentPreviewState.NoPreview) }
                        } else {
                            updateState {
                                copy(
                                    previewState = DocumentPreviewState.Ready(
                                        pages = response.pages,
                                        totalPages = response.totalPages,
                                        renderedPages = response.renderedPages,
                                        dpi = response.dpi,
                                        hasMore = response.totalPages > response.renderedPages
                                    )
                                )
                            }
                        }
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load preview pages for document: $documentId" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.DocumentPreviewLoadFailed
                    } else {
                        exception
                    }
                    withState<DocumentReviewState.AwaitingExtraction, _> {
                        updateState {
                            copy(
                                previewState = DocumentPreviewState.Error(
                                    exception = displayException,
                                    retry = { intent(DocumentReviewIntent.RetryLoadPreview) }
                                ),
                            )
                        }
                    }
                }
            )
    }
}
