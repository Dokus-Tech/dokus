package tech.dokus.features.cashflow.presentation.documents.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_empty_title
import tech.dokus.aura.resources.documents_empty_upload_cta
import tech.dokus.aura.resources.documents_filter_no_match
import tech.dokus.aura.resources.documents_search_no_results
import tech.dokus.aura.resources.documents_upload
import tech.dokus.aura.resources.search_placeholder
import tech.dokus.features.cashflow.presentation.documents.components.DocumentDisplayStatus
import tech.dokus.features.cashflow.presentation.documents.components.DocumentRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentStatusFilterChips
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact

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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (state) {
                is DocumentsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
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

    // Load more when near the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val totalItems = listState.layoutInfo.totalItemsCount
            state.documents.hasMorePages &&
                !state.documents.isLoadingMore &&
                lastVisibleItem >= totalItems - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            onIntent(DocumentsIntent.LoadMore)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Top bar: Search + Upload button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PSearchFieldCompact(
                value = state.searchQuery,
                onValueChange = { onIntent(DocumentsIntent.UpdateSearchQuery(it)) },
                placeholder = stringResource(Res.string.search_placeholder),
                onClear = { onIntent(DocumentsIntent.UpdateSearchQuery("")) },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onUploadClick) {
                Icon(
                    imageVector = Icons.Default.Upload,
                    contentDescription = stringResource(Res.string.documents_upload),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Status filter chips
        DocumentStatusFilterChips(
            selectedStatus = state.statusFilter,
            onStatusSelected = { onIntent(DocumentsIntent.UpdateStatusFilter(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Documents list or empty state
        when {
            documents.isEmpty() && state.searchQuery.isEmpty() && state.statusFilter == null -> {
                // No documents at all
                DocumentsEmptyState(
                    title = stringResource(Res.string.documents_empty_title),
                    showUploadCta = true,
                    onUploadClick = onUploadClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            documents.isEmpty() && state.searchQuery.isNotEmpty() -> {
                // Search returned no results
                DocumentsEmptyState(
                    title = stringResource(Res.string.documents_search_no_results, state.searchQuery),
                    showUploadCta = false,
                    onUploadClick = onUploadClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            documents.isEmpty() -> {
                // Filter returned no results
                DocumentsEmptyState(
                    title = stringResource(Res.string.documents_filter_no_match),
                    showUploadCta = false,
                    onUploadClick = onUploadClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                // Documents list in a card surface
                DokusCardSurface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = documents,
                            key = { _, doc -> doc.document.id.toString() }
                        ) { index, document ->
                            DocumentRow(
                                document = document,
                                onClick = { onIntent(DocumentsIntent.OpenDocument(document.document.id)) }
                            )

                            // Add divider between rows (not after last item)
                            if (index < documents.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    thickness = 1.dp
                                )
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
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Empty state for the documents list.
 */
@Composable
private fun DocumentsEmptyState(
    title: String,
    showUploadCta: Boolean,
    onUploadClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (showUploadCta) {
                OutlinedButton(onClick = onUploadClick) {
                    Icon(
                        imageVector = Icons.Default.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(Res.string.documents_empty_upload_cta))
                }
            }
        }
    }
}
