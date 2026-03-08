package tech.dokus.features.cashflow.presentation.documents.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.features.cashflow.presentation.documents.model.DocumentsLocalUploadRow
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Preview(name = "Documents Desktop Upload Header", widthDp = 1280, heightDp = 900)
@Composable
private fun DocumentsDesktopUploadHeaderPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentsScreen(
            state = previewContentState(),
            localUploadRows = previewLocalRows(),
            isDesktopDropTargetActive = false,
            desktopDropScrollToken = 0,
            onIntent = {},
            onUploadClick = {},
            onMobileFabClick = {},
            onRetryLocalUpload = {},
            onDismissLocalUpload = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Documents Desktop Drop Overlay", widthDp = 1280, heightDp = 900)
@Composable
private fun DocumentsDesktopDropOverlayPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentsScreen(
            state = previewContentState(),
            localUploadRows = previewLocalRows(),
            isDesktopDropTargetActive = true,
            desktopDropScrollToken = 0,
            onIntent = {},
            onUploadClick = {},
            onMobileFabClick = {},
            onRetryLocalUpload = {},
            onDismissLocalUpload = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Documents Mobile FAB", widthDp = 390, heightDp = 844)
@Composable
private fun DocumentsMobileFabPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentsScreen(
            state = previewContentState(),
            localUploadRows = previewLocalRows(),
            isDesktopDropTargetActive = false,
            desktopDropScrollToken = 0,
            onIntent = {},
            onUploadClick = {},
            onMobileFabClick = {},
            onRetryLocalUpload = {},
            onDismissLocalUpload = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun previewContentState(): DocumentsState.Content {
    return DocumentsState.Content(
        documents = PaginationState(
            currentPage = 1,
            pageSize = 20,
            isLoadingMore = false,
            hasMorePages = false,
            data = emptyList()
        ),
        filter = DocumentFilter.All,
        needsAttentionCount = 1,
        confirmedCount = 8,
        isRefreshing = false
    )
}

private fun previewLocalRows(): List<DocumentsLocalUploadRow> {
    return listOf(
        DocumentsLocalUploadRow(
            taskId = "local-1",
            fileName = "receipt-feb-28.pdf",
            status = DocumentsLocalUploadRow.Status.ReadingDocument
        ),
        DocumentsLocalUploadRow(
            taskId = "local-2",
            fileName = "tesla-belgium.pdf",
            status = DocumentsLocalUploadRow.Status.Failed
        )
    )
}
