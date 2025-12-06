package ai.dokus.app.screens

import ai.dokus.app.cashflow.components.PendingDocumentsCard
import ai.dokus.app.core.state.DokusState
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.viewmodel.DashboardViewModel
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PIconPosition
import ai.dokus.foundation.design.components.common.PSearchFieldCompact
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.domain.model.MediaDto
import ai.dokus.foundation.navigation.destinations.AuthDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.koin.compose.viewmodel.koinViewModel

/**
 * Number of pending documents to display per page on the dashboard.
 */
private const val PENDING_PAGE_SIZE = 5

/**
 * Data class holding computed pending documents pagination state for the dashboard.
 */
private data class DashboardPendingPaginationData(
    val documents: List<MediaDto>,
    val isLoading: Boolean,
    val hasPreviousPage: Boolean,
    val hasNextPage: Boolean
)

@Composable
internal fun DashboardScreen(
    viewModel: DashboardViewModel = koinViewModel()
) {
    val navController = LocalNavController.current
    var searchQuery by remember { mutableStateOf("") }
    val isLargeScreen = LocalScreenSize.current.isLarge
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val searchExpanded = isLargeScreen || isSearchExpanded

    val currentTenantState by viewModel.currentTenantState.collectAsState()
    val currentTenant = currentTenantState.let { if (it.isSuccess()) it.data else null }

    // Pending documents state (for mobile only)
    val pendingDocumentsState by viewModel.pendingDocumentsState.collectAsState()
    val pendingCurrentPage by viewModel.pendingCurrentPage.collectAsState()

    // Compute pending documents pagination with derivedStateOf for performance
    val pendingPaginationData by remember(pendingDocumentsState, pendingCurrentPage) {
        derivedStateOf {
            val allDocs = (pendingDocumentsState as? DokusState.Success)?.data ?: emptyList()
            val totalPages = if (allDocs.isEmpty()) 1 else ((allDocs.size - 1) / PENDING_PAGE_SIZE) + 1
            val start = pendingCurrentPage * PENDING_PAGE_SIZE
            val end = minOf(start + PENDING_PAGE_SIZE, allDocs.size)
            val currentDocs = if (start < allDocs.size) {
                allDocs.subList(start, end)
            } else {
                emptyList()
            }
            DashboardPendingPaginationData(
                documents = currentDocs,
                isLoading = pendingDocumentsState is DokusState.Loading,
                hasPreviousPage = pendingCurrentPage > 0,
                hasNextPage = pendingCurrentPage < totalPages - 1
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.refreshTenant()
    }

    LaunchedEffect(isLargeScreen) {
        isSearchExpanded = isLargeScreen
    }

    Scaffold(
        topBar = {
            PTopAppBarSearchAction(
                searchContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!isLargeScreen && !searchExpanded) {
                            IconButton(
                                onClick = { isSearchExpanded = true },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.Search,
                                    contentDescription = "Search"
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = searchExpanded,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                        ) {
                            PSearchFieldCompact(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = "Search...",
                                modifier = if (isLargeScreen) Modifier else Modifier.fillMaxWidth()
                            )
                        }
                    }
                },
                actions = {
                    PButton(
                        text = currentTenant?.displayName?.value ?: "Select Tenant",
                        variant = PButtonVariant.Outline,
                        icon = Icons.Default.SwitchAccount,
                        iconPosition = PIconPosition.Trailing,
                        isLoading = currentTenantState.isLoading(),
                        onClick = { navController.navigateTo(AuthDestination.WorkspaceSelect) }
                    )
                }
            )
        }
    ) { contentPadding ->
        // Mobile dashboard content
        if (!isLargeScreen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Pending documents card - always show (displays empty state when no documents)
                PendingDocumentsCard(
                    documents = pendingPaginationData.documents,
                    isLoading = pendingPaginationData.isLoading,
                    hasPreviousPage = pendingPaginationData.hasPreviousPage,
                    hasNextPage = pendingPaginationData.hasNextPage,
                    onDocumentClick = { media ->
                        // TODO: Navigate to document edit/confirmation screen
                    },
                    onPreviousClick = viewModel::pendingDocumentsPreviousPage,
                    onNextClick = viewModel::pendingDocumentsNextPage,
                    modifier = Modifier.fillMaxWidth()
                )

                // Other dashboard widgets can be added here
            }
        } else {
            // Desktop dashboard content (pending documents shown in Cashflow screen)
            // Other desktop-specific content can be added here
        }
    }
}
