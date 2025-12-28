package ai.dokus.app.cashflow.presentation.review

import tech.dokus.domain.model.DocumentPagePreviewDto

/**
 * UI state for PDF document preview rendering.
 */
sealed interface DocumentPreviewState {
    /**
     * Preview is loading (fetching page metadata from server).
     */
    data object Loading : DocumentPreviewState

    /**
     * Preview is ready with page metadata.
     *
     * @property pages List of page metadata with image URLs
     * @property totalPages Total number of pages in the document
     * @property renderedPages Number of pages included in this response
     * @property dpi Resolution of rendered pages
     * @property hasMore True if there are more pages available to load
     */
    data class Ready(
        val pages: List<DocumentPagePreviewDto>,
        val totalPages: Int,
        val renderedPages: Int,
        val dpi: Int,
        val hasMore: Boolean
    ) : DocumentPreviewState

    /**
     * Preview failed to load.
     *
     * @property message Error message to display
     * @property retry Callback to retry loading
     */
    data class Error(
        val message: String,
        val retry: () -> Unit
    ) : DocumentPreviewState

    /**
     * Document is not a PDF (image, etc.) - show file placeholder.
     */
    data object NotPdf : DocumentPreviewState

    /**
     * No preview available for this document.
     */
    data object NoPreview : DocumentPreviewState
}
