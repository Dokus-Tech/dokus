package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.features.cashflow.usecases.GetDocumentPagesUseCase
import tech.dokus.foundation.platform.Logger

internal class DocumentReviewPreview(
    private val getDocumentPages: GetDocumentPagesUseCase,
    private val logger: Logger,
) {
    suspend fun DocumentReviewCtx.handleOpenPreviewSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(showPreviewSheet = true) }
        }
    }

    suspend fun DocumentReviewCtx.handleClosePreviewSheet() {
        withState<DocumentReviewState.Content, _> {
            updateState { copy(showPreviewSheet = false) }
        }
    }

    suspend fun DocumentReviewCtx.handleLoadPreviewPages() {
        withState<DocumentReviewState.Content, _> {
            loadPreviewPages(
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
}
