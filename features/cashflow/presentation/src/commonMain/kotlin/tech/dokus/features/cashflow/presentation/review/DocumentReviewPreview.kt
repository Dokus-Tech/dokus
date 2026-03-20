package tech.dokus.features.cashflow.presentation.review

import kotlinx.coroutines.launch
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DEFAULT_MAX_PAGES
import tech.dokus.domain.model.Dpi
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
        var activeDocumentId: DocumentId? = null
        var contentType: String? = null
        var incomingSourceId: DocumentSourceId? = null
        var incomingSourceType: DocumentSource? = null
        var incomingContentType: String? = null

        withState {
            activeDocumentId = documentId
            contentType = documentRecord?.sources?.firstOrNull()?.contentType
            val incomingSource = documentRecord
                ?.pendingMatchReview
                ?.incomingSourceId
                ?.let { sourceId -> documentRecord?.sources?.firstOrNull { it.id == sourceId } }
            incomingSourceId = incomingSource?.id
            incomingSourceType = incomingSource?.sourceChannel
            incomingContentType = incomingSource?.contentType
        }

        val documentId = activeDocumentId ?: return
        if (contentType != null) {
            loadPreviewPages(
                documentId = documentId,
                contentType = requireNotNull(contentType),
                dpi = Dpi.default,
                maxPages = DEFAULT_MAX_PAGES
            )
        }

        if (incomingSourceId != null && incomingSourceType != null) {
            loadIncomingPreviewPages(
                documentId = documentId,
                sourceId = requireNotNull(incomingSourceId),
                sourceType = requireNotNull(incomingSourceType),
                contentType = incomingContentType.orEmpty(),
                dpi = Dpi.default,
                maxPages = DEFAULT_MAX_PAGES,
            )
        } else {
            withState {
                updateState {
                    copy(
                        incomingPreviewState = if (hasPendingMatchReview) {
                            DocumentPreviewState.NoPreview
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private fun previewUnavailableState(sourceType: DocumentSource): DocumentPreviewState {
        return if (sourceType == DocumentSource.Peppol) {
            DocumentPreviewState.NoPreview
        } else {
            DocumentPreviewState.NotPdf
        }
    }

    suspend fun DocumentReviewCtx.handleLoadMorePages(maxPages: Int) {
        withState {
            if (!hasContent) return@withState
            val activeDocumentId = documentId ?: return@withState
            val contentType = documentRecord?.sources?.firstOrNull()?.contentType ?: return@withState
            loadPreviewPages(
                documentId = activeDocumentId,
                contentType = contentType,
                dpi = Dpi.default,
                maxPages = maxPages
            )
        }
    }

    suspend fun DocumentReviewCtx.handleOpenSourceModal(sourceId: DocumentSourceId) {
        withState {
            if (!hasContent) return@withState
            val activeDocumentId = documentId ?: return@withState
            val source = documentRecord?.sources?.firstOrNull { it.id == sourceId } ?: return@withState
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
                    documentId = activeDocumentId,
                    sourceId = source.id,
                    sourceType = source.sourceChannel,
                    contentType = source.contentType.orEmpty(),
                    dpi = Dpi.default,
                    maxPages = DEFAULT_MAX_PAGES,
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleCloseSourceModal() {
        updateState { copy(sourceViewerState = null) }
    }

    suspend fun DocumentReviewCtx.handleToggleSourceTechnicalDetails() {
        withState {
            val activeDocumentId = documentId ?: return@withState
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
                loadSourceRawContent(activeDocumentId, viewer.sourceId)
            }
        }
    }

    private suspend fun DocumentReviewCtx.loadSelectedSourcePreview(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        sourceType: DocumentSource,
        contentType: String,
        dpi: Dpi,
        maxPages: Int
    ) {
        val isPdf = contentType.contains("pdf", ignoreCase = true)
        if (!isPdf) {
            withState {
                val viewer = sourceViewerState ?: return@withState
                updateState {
                    copy(
                        sourceViewerState = viewer.copy(
                            previewState = previewUnavailableState(sourceType)
                        )
                    )
                }
            }
            return
        }

        getDocumentSourcePages(documentId, sourceId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
                    withState {
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
                    withState {
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
        withState {
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
                withState {
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
                withState {
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

    private suspend fun DocumentReviewCtx.loadIncomingPreviewPages(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        sourceType: DocumentSource,
        contentType: String,
        dpi: Dpi,
        maxPages: Int,
    ) {
        if (!contentType.contains("pdf", ignoreCase = true)) {
            updateState { copy(incomingPreviewState = previewUnavailableState(sourceType)) }
            return
        }

        updateState { copy(incomingPreviewState = DocumentPreviewState.Loading) }

        getDocumentSourcePages(documentId, sourceId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
                    updateState {
                        copy(
                            incomingPreviewState = if (response.pages.isEmpty()) {
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
                    }
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load incoming preview pages for source=$sourceId" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.DocumentPreviewLoadFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(
                            incomingPreviewState = DocumentPreviewState.Error(
                                exception = displayException,
                                retry = { intent(DocumentReviewIntent.RetryLoadPreview) }
                            ),
                        )
                    }
                }
            )
    }

    private suspend fun DocumentReviewCtx.loadPreviewPages(
        documentId: DocumentId,
        contentType: String,
        dpi: Dpi,
        maxPages: Int
    ) {
        if (!contentType.contains("pdf", ignoreCase = true)) {
            updateState { copy(previewState = DocumentPreviewState.NotPdf) }
            return
        }

        updateState { copy(previewState = DocumentPreviewState.Loading) }

        getDocumentPages(documentId, dpi, maxPages)
            .fold(
                onSuccess = { response ->
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
                },
                onFailure = { error ->
                    logger.e(error) { "Failed to load preview pages for document: $documentId" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.DocumentPreviewLoadFailed
                    } else {
                        exception
                    }
                    updateState {
                        copy(
                            previewState = DocumentPreviewState.Error(
                                exception = displayException,
                                retry = { intent(DocumentReviewIntent.RetryLoadPreview) }
                            ),
                        )
                    }
                }
            )
    }
}
