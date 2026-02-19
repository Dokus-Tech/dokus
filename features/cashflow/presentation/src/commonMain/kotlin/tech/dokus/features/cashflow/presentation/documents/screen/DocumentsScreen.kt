package tech.dokus.features.cashflow.presentation.documents.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_empty_title
import tech.dokus.aura.resources.documents_empty_upload_cta
import tech.dokus.aura.resources.documents_filter_no_match
import tech.dokus.aura.resources.documents_upload
import tech.dokus.features.cashflow.presentation.common.components.empty.DokusEmptyState
import tech.dokus.features.cashflow.presentation.common.components.pagination.rememberLoadMoreTrigger
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableDivider
import tech.dokus.features.cashflow.presentation.common.components.table.DokusTableSurface
import tech.dokus.features.cashflow.presentation.documents.components.DocumentFilterButtons
import tech.dokus.features.cashflow.presentation.documents.components.DocumentMobileRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentTableHeaderRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentTableRow
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentFilter
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.local.LocalScreenSize

@Composable
internal fun DocumentsScreen(
    state: DocumentsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (DocumentsIntent) -> Unit,
    onUploadClick: () -> Unit,
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
                        onIntent = onIntent,
                        onUploadClick = onUploadClick
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentsContent(
    state: DocumentsState.Content,
    onIntent: (DocumentsIntent) -> Unit,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val documents = state.documents.data
    val isLargeScreen = LocalScreenSize.current.isLarge

    // Load more when near the end
    val shouldLoadMore = rememberLoadMoreTrigger(
        listState = listState,
        hasMore = state.documents.hasMorePages,
        isLoading = state.documents.isLoadingMore,
        buffer = 3
    )

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onIntent(DocumentsIntent.LoadMore)
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Filter tabs
        DocumentFilterButtons(
            currentFilter = state.filter,
            totalCount = documents.size,
            needsAttentionCount = state.needsAttentionCount,
            onFilterSelected = { onIntent(DocumentsIntent.UpdateFilter(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        // Documents list or empty state
        when {
            documents.isEmpty() && state.searchQuery.isEmpty() && state.filter == DocumentFilter.All -> {
                DokusEmptyState(
                    title = stringResource(Res.string.documents_empty_title),
                    subtitle = stringResource(Res.string.documents_empty_upload_cta),
                    modifier = Modifier.fillMaxSize(),
                    action = {
                        OutlinedButton(onClick = onUploadClick) {
                            Icon(
                                imageVector = Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.documents_upload))
                        }
                    }
                )
            }

            documents.isEmpty() -> {
                DokusEmptyState(
                    title = stringResource(Res.string.documents_filter_no_match),
                    modifier = Modifier.fillMaxSize()
                )
            }

            else -> {
                if (isLargeScreen) {
                    // Desktop: table in a card
                    DokusTableSurface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp),
                        header = { DocumentTableHeaderRow() }
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            itemsIndexed(
                                items = documents,
                                key = { _, doc -> doc.document.id.toString() }
                            ) { index, document ->
                                DocumentTableRow(
                                    document = document,
                                    onClick = { onIntent(DocumentsIntent.OpenDocument(document.document.id)) }
                                )

                                if (index < documents.size - 1) {
                                    DokusTableDivider()
                                }
                            }

                            if (state.documents.isLoadingMore) {
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
                } else {
                    // Mobile: individual cards per document
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(
                            items = documents,
                            key = { _, doc -> doc.document.id.toString() }
                        ) { _, document ->
                            DocumentMobileRow(
                                document = document,
                                onClick = { onIntent(DocumentsIntent.OpenDocument(document.document.id)) }
                            )
                        }

                        if (state.documents.isLoadingMore) {
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
            }
        }
    }
}
