package tech.dokus.features.cashflow.presentation.documents.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
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
import tech.dokus.aura.resources.cashflow_no_documents
import tech.dokus.aura.resources.search_placeholder
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.features.cashflow.presentation.documents.components.DocumentRow
import tech.dokus.features.cashflow.presentation.documents.components.DocumentStatusFilterChips
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsIntent
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.features.cashflow.presentation.documents.mvi.DocumentsState
import tech.dokus.foundation.aura.components.common.DokusErrorContent

@Composable
internal fun DocumentsScreen(
    state: DocumentsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (DocumentsIntent) -> Unit,
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
                        onIntent = onIntent
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
        // Search field
        PSearchFieldCompact(
            value = state.searchQuery,
            onValueChange = { onIntent(DocumentsIntent.UpdateSearchQuery(it)) },
            placeholder = stringResource(Res.string.search_placeholder),
            onClear = { onIntent(DocumentsIntent.UpdateSearchQuery("")) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Status filter chips
        DocumentStatusFilterChips(
            selectedStatus = state.statusFilter,
            onStatusSelected = { onIntent(DocumentsIntent.UpdateStatusFilter(it)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (documents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(Res.string.cashflow_no_documents))
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(documents, key = { it.document.id.toString() }) { document ->
                    DocumentRow(
                        document = document,
                        onClick = { onIntent(DocumentsIntent.OpenDocument(document.document.id)) }
                    )
                }

                if (state.documents.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
        }
    }
}
