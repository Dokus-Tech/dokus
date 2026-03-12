package tech.dokus.features.cashflow.presentation.documents.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_drop_to_upload
import tech.dokus.aura.resources.documents_empty_title
import tech.dokus.aura.resources.documents_filter_no_match
import tech.dokus.aura.resources.documents_upload
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.features.cashflow.presentation.common.components.pagination.rememberLoadMoreTrigger
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableSurface
import tech.dokus.features.cashflow.presentation.documents.components.DocumentFilterButtons
import tech.dokus.features.cashflow.presentation.documents.components.DocumentLocalUploadMobileRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentLocalUploadTableRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentMobileRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentTableHeaderRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentTableRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentsDropHintTableRow
import tech.dokus.features.cashflow.presentation.documents.model.DocumentsLocalUploadRow
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusEmptyState
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.local.LocalScreenSize

private val DocumentsState.documentItems: List<DocumentListItemDto>
    get() = documents.lastData?.data ?: emptyList()

private sealed interface DocumentsDisplayRow {
    data class Local(val row: DocumentsLocalUploadRow) : DocumentsDisplayRow
    data class Remote(val row: DocumentListItemDto) : DocumentsDisplayRow
}

@Composable
internal fun DocumentsScreen(
    state: DocumentsState,
    snackbarHostState: SnackbarHostState,
    localUploadRows: List<DocumentsLocalUploadRow>,
    isDesktopDropTargetActive: Boolean,
    desktopDropScrollToken: Int,
    onIntent: (DocumentsIntent) -> Unit,
    onUploadClick: () -> Unit,
    onMobileFabClick: () -> Unit,
    onRetryLocalUpload: (String) -> Unit,
    onDismissLocalUpload: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val remoteDocuments = state.documentItems
    val isLargeScreen = LocalScreenSize.current.isLarge
    var handledDropScrollToken by remember { mutableIntStateOf(0) }

    val displayRows = remember(localUploadRows, remoteDocuments) {
        buildList {
            localUploadRows.forEach { add(DocumentsDisplayRow.Local(it)) }
            remoteDocuments.forEach { add(DocumentsDisplayRow.Remote(it)) }
        }
    }

    val shouldLoadMore = rememberLoadMoreTrigger(
        listState = listState,
        hasMore = state.documents.lastData?.hasMorePages ?: false,
        isLoading = state.documents.isLoading(),
        buffer = 3
    )

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onIntent(DocumentsIntent.LoadMore)
        }
    }

    LaunchedEffect(desktopDropScrollToken, isLargeScreen, displayRows.size) {
        val shouldScroll = isLargeScreen &&
                desktopDropScrollToken > handledDropScrollToken &&
                displayRows.isNotEmpty()
        if (shouldScroll) {
            listState.animateScrollToItem(0)
            handledDropScrollToken = desktopDropScrollToken
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) {

        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DocumentsToolbar(
                    state = state,
                    totalCount = state.totalCount,
                    isLargeScreen = isLargeScreen,
                    onIntent = onIntent,
                    onUploadClick = onUploadClick
                )

                when {
                    displayRows.isEmpty() && state.documents.isLoading() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            DokusLoader(size = DokusLoaderSize.Small)
                        }
                    }

                    displayRows.isEmpty() && state.filter == DocumentFilter.All -> {
                        DokusEmptyState(
                            title = stringResource(Res.string.documents_empty_title),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    displayRows.isEmpty() -> {
                        DokusEmptyState(
                            title = stringResource(Res.string.documents_filter_no_match),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        if (isLargeScreen) {
                            DesktopDocumentsTable(
                                displayRows = displayRows,
                                listState = listState,
                                isLoadingMore = state.documents.isLoading(),
                                isRefreshing = state.documents.isLoading(),
                                isDropTargetActive = isDesktopDropTargetActive,
                                dropHintText = stringResource(Res.string.documents_drop_to_upload),
                                onOpenDocument = { documentId ->
                                    onIntent(DocumentsIntent.OpenDocument(documentId))
                                },
                                onRetryLocalUpload = onRetryLocalUpload,
                                onDismissLocalUpload = onDismissLocalUpload,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 16.dp)
                            )
                        } else {
                            MobileDocumentsList(
                                displayRows = displayRows,
                                listState = listState,
                                isLoadingMore = state.documents.isLoading(),
                                isRefreshing = state.documents.isLoading(),
                                onOpenDocument = { documentId ->
                                    onIntent(DocumentsIntent.OpenDocument(documentId))
                                },
                                onRetryLocalUpload = onRetryLocalUpload,
                                onDismissLocalUpload = onDismissLocalUpload,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }

            if (!isLargeScreen) {
                FloatingActionButton(
                    onClick = onMobileFabClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(Res.string.documents_upload),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentsToolbar(
    state: DocumentsState,
    totalCount: Int,
    isLargeScreen: Boolean,
    onIntent: (DocumentsIntent) -> Unit,
    onUploadClick: () -> Unit,
) {
    if (isLargeScreen) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DocumentFilterButtons(
                currentFilter = state.filter,
                totalCount = totalCount,
                needsAttentionCount = state.needsAttentionCount,
                confirmedCount = state.confirmedCount,
                onFilterSelected = { onIntent(DocumentsIntent.UpdateFilter(it)) },
            )

            Spacer(modifier = Modifier.weight(1f))

            PPrimaryButton(
                text = stringResource(Res.string.documents_upload),
                onClick = onUploadClick
            )
        }
    } else {
        DocumentFilterButtons(
            currentFilter = state.filter,
            totalCount = totalCount,
            needsAttentionCount = state.needsAttentionCount,
            confirmedCount = state.confirmedCount,
            onFilterSelected = { onIntent(DocumentsIntent.UpdateFilter(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
private fun DesktopDocumentsTable(
    displayRows: List<DocumentsDisplayRow>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    isDropTargetActive: Boolean,
    dropHintText: String,
    onOpenDocument: (DocumentId) -> Unit,
    onRetryLocalUpload: (String) -> Unit,
    onDismissLocalUpload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            DokusTableSurface(
                modifier = Modifier.fillMaxSize(),
                header = { DocumentTableHeaderRow() }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item(key = "desktop-drop-hint-row") {
                        DocumentsDropHintTableRow(text = dropHintText)
                        if (displayRows.isNotEmpty()) {
                            DokusTableDivider()
                        }
                    }

                    itemsIndexed(
                        items = displayRows,
                        key = { _, row ->
                            when (row) {
                                is DocumentsDisplayRow.Local -> "local-${row.row.taskId}"
                                is DocumentsDisplayRow.Remote -> row.row.documentId.toString()
                            }
                        }
                    ) { index, row ->
                        when (row) {
                            is DocumentsDisplayRow.Local -> {
                                DocumentLocalUploadTableRow(
                                    row = row.row,
                                    onRetry = onRetryLocalUpload,
                                    onDismiss = onDismissLocalUpload
                                )
                            }

                            is DocumentsDisplayRow.Remote -> {
                                DocumentTableRow(
                                    document = row.row,
                                    onClick = { onOpenDocument(row.row.documentId) }
                                )
                            }
                        }

                        if (index < displayRows.size - 1) {
                            DokusTableDivider()
                        }
                    }

                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                DokusLoader(size = DokusLoaderSize.Small)
                            }
                        }
                    }
                }
            }

            if (isDropTargetActive) {
                DesktopDropOverlay(text = dropHintText)
            }

            if (isRefreshing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    DokusLoader(size = DokusLoaderSize.Small)
                }
            }
        }
    }
}

@Composable
private fun MobileDocumentsList(
    displayRows: List<DocumentsDisplayRow>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    isRefreshing: Boolean,
    onOpenDocument: (DocumentId) -> Unit,
    onRetryLocalUpload: (String) -> Unit,
    onDismissLocalUpload: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            itemsIndexed(
                items = displayRows,
                key = { _, row ->
                    when (row) {
                        is DocumentsDisplayRow.Local -> "local-${row.row.taskId}"
                        is DocumentsDisplayRow.Remote -> row.row.documentId.toString()
                    }
                }
            ) { _, row ->
                when (row) {
                    is DocumentsDisplayRow.Local -> {
                        DocumentLocalUploadMobileRow(
                            row = row.row,
                            onRetry = onRetryLocalUpload,
                            onDismiss = onDismissLocalUpload
                        )
                    }

                    is DocumentsDisplayRow.Remote -> {
                        DocumentMobileRow(
                            document = row.row,
                            onClick = { onOpenDocument(row.row.documentId) }
                        )
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusLoader(size = DokusLoaderSize.Small)
                    }
                }
            }
        }

        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                DokusLoader(size = DokusLoaderSize.Small)
            }
        }
    }
}
