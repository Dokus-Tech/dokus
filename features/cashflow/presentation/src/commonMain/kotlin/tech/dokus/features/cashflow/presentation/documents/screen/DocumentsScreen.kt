package tech.dokus.features.cashflow.presentation.documents.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.presentation.documents.model.DocumentsLocalUploadRow
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun DocumentsScreen(
    state: DocumentsState,
    localUploadRows: List<DocumentsLocalUploadRow>,
    isDesktopDropTargetActive: Boolean,
    desktopDropScrollToken: Int,
    snackbarHostState: SnackbarHostState,
    onIntent: (DocumentsIntent) -> Unit,
    onUploadClick: () -> Unit,
    onMobileFabClick: () -> Unit,
    onRetryLocalUpload: (String) -> Unit,
    onDismissLocalUpload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) {
        Box(Modifier.fillMaxSize()) {
            when (state) {
                is DocumentsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusLoader()
                    }
                }

                is DocumentsState.Error -> {
                    DokusErrorContent(
                        exception = state.exception,
                        retryHandler = state.retryHandler,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }

                is DocumentsState.Content -> {
                    DocumentsContent(
                        state = state,
                        localUploadRows = localUploadRows,
                        isDesktopDropTargetActive = isDesktopDropTargetActive,
                        desktopDropScrollToken = desktopDropScrollToken,
                        onIntent = onIntent,
                        onUploadClick = onUploadClick,
                        onMobileFabClick = onMobileFabClick,
                        onRetryLocalUpload = onRetryLocalUpload,
                        onDismissLocalUpload = onDismissLocalUpload
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun DocumentsScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentsScreen(
            state = DocumentsState.Loading,
            localUploadRows = emptyList(),
            isDesktopDropTargetActive = false,
            desktopDropScrollToken = 0,
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            onUploadClick = {},
            onMobileFabClick = {},
            onRetryLocalUpload = {},
            onDismissLocalUpload = {},
        )
    }
}
