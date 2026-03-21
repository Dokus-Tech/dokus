@file:Suppress("TooManyFunctions")

package tech.dokus.features.cashflow.presentation.detail.mvi.preview

import kotlinx.coroutines.launch
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.model.DEFAULT_MAX_PAGES
import tech.dokus.domain.model.Dpi
import tech.dokus.features.cashflow.presentation.detail.DocumentPreviewState
import tech.dokus.features.cashflow.presentation.detail.SourceEvidenceViewerState
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourceContentUseCase
import tech.dokus.features.cashflow.usecases.GetDocumentSourcePagesUseCase
import tech.dokus.foundation.platform.Logger

private typealias PreviewCtx = PipelineContext<DocumentPreviewChildState, DocumentPreviewIntent, DocumentPreviewAction>

internal class DocumentPreviewContainer(
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val getDocumentSourcePages: GetDocumentSourcePagesUseCase,
    private val getDocumentSourceContent: GetDocumentSourceContentUseCase,
) : Container<DocumentPreviewChildState, DocumentPreviewIntent, DocumentPreviewAction> {

    private val logger = Logger.forClass<DocumentPreviewContainer>()

    override val store: Store<DocumentPreviewChildState, DocumentPreviewIntent, DocumentPreviewAction> =
        store(DocumentPreviewChildState()) {
            reduce { intent ->
                when (intent) {
                    is DocumentPreviewIntent.SetDocumentContext -> handleSetDocumentContext(intent)
                    is DocumentPreviewIntent.LoadPages -> handleLoadPreviewPages()
                    is DocumentPreviewIntent.LoadMorePages -> handleLoadMorePages(intent.maxPages)
                    is DocumentPreviewIntent.RetryLoad -> handleLoadPreviewPages()
                    is DocumentPreviewIntent.OpenSourceModal -> handleOpenSourceModal(intent.sourceId)
                    is DocumentPreviewIntent.CloseSourceModal -> handleCloseSourceModal()
                    is DocumentPreviewIntent.ToggleSourceTechnicalDetails -> handleToggleSourceTechnicalDetails()
                }
            }
        }

    private suspend fun PreviewCtx.handleSetDocumentContext(
        intent: DocumentPreviewIntent.SetDocumentContext,
    ) {
        updateState {
            copy(
                documentId = intent.documentId,
                documentRecord = intent.documentRecord,
                hasContent = intent.hasContent,
                hasPendingMatchReview = intent.hasPendingMatchReview,
                previewState = intent.previewState ?: previewState,
                incomingPreviewState = intent.incomingPreviewState ?: incomingPreviewState,
                sourceViewerState = if (intent.resetSourceViewer) null else sourceViewerState,
            )
        }
    }

    private suspend fun PreviewCtx.handleLoadPreviewPages() {
        var activeDocumentId: DocumentId? = null
        var contentType: String? = null
        var incomingSourceId: DocumentSourceId? = null
        var incomingSourceType: DocumentSource? = null
        var incomingContentType: String? = null
        var hasPendingMatch = false

        withState {
            activeDocumentId = documentId
            contentType = documentRecord?.sources?.firstOrNull()?.contentType
            val record = documentRecord
            val incomingSource = record
                ?.pendingMatchReview
                ?.incomingSourceId
                ?.let { sourceId -> record.sources.firstOrNull { it.id == sourceId } }
            incomingSourceId = incomingSource?.id
            incomingSourceType = incomingSource?.sourceChannel
            incomingContentType = incomingSource?.contentType
            hasPendingMatch = hasPendingMatchReview
        }

        val documentId = activeDocumentId ?: return
        if (contentType != null) {
            loadPreviewPages(
                documentId = documentId,
                contentType = requireNotNull(contentType),
                dpi = Dpi.default,
                maxPages = DEFAULT_MAX_PAGES,
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
                        incomingPreviewState = if (hasPendingMatch) {
                            DocumentPreviewState.NoPreview
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }

    private suspend fun PreviewCtx.handleLoadMorePages(maxPages: Int) {
        withState {
            if (!hasContent) return@withState
            val activeDocumentId = documentId ?: return@withState
            val contentType = documentRecord?.sources?.firstOrNull()?.contentType ?: return@withState
            loadPreviewPages(
                documentId = activeDocumentId,
                contentType = contentType,
                dpi = Dpi.default,
                maxPages = maxPages,
            )
        }
    }

    private suspend fun PreviewCtx.handleOpenSourceModal(sourceId: DocumentSourceId) {
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

    private suspend fun PreviewCtx.handleCloseSourceModal() {
        updateState { copy(sourceViewerState = null) }
    }

    private suspend fun PreviewCtx.handleToggleSourceTechnicalDetails() {
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

    // === Private loading helpers ===

    private fun previewUnavailableState(sourceType: DocumentSource): DocumentPreviewState {
        return if (sourceType == DocumentSource.Peppol) {
            DocumentPreviewState.NoPreview
        } else {
            DocumentPreviewState.NotPdf
        }
    }

    private suspend fun PreviewCtx.loadPreviewPages(
        documentId: DocumentId,
        contentType: String,
        dpi: Dpi,
        maxPages: Int,
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
                                retry = { intent(DocumentPreviewIntent.RetryLoad) }
                            ),
                        )
                    }
                }
            )
    }

    private suspend fun PreviewCtx.loadIncomingPreviewPages(
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
                                retry = { intent(DocumentPreviewIntent.RetryLoad) }
                            ),
                        )
                    }
                }
            )
    }

    private suspend fun PreviewCtx.loadSelectedSourcePreview(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
        sourceType: DocumentSource,
        contentType: String,
        dpi: Dpi,
        maxPages: Int,
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
                                        retry = { intent(DocumentPreviewIntent.OpenSourceModal(sourceId)) }
                                    )
                                )
                            )
                        }
                    }
                }
            )
    }

    private suspend fun PreviewCtx.loadSourceRawContent(
        documentId: DocumentId,
        sourceId: DocumentSourceId,
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
}
