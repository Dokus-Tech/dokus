package ai.dokus.app.screens

import ai.dokus.app.cashflow.components.PendingDocumentsCard
import ai.dokus.app.core.state.isLoading
import ai.dokus.app.core.state.isSuccess
import ai.dokus.app.viewmodel.DashboardViewModel
import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PIconPosition
import ai.dokus.foundation.design.components.common.PSearchFieldCompact
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.design.local.LocalScreenSize
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
    val pendingPaginationState by viewModel.pendingPaginationState.collectAsState()
    val isPendingLoading by viewModel.isPendingLoading.collectAsState()

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
                    paginationState = pendingPaginationState,
                    isLoading = isPendingLoading,
                    onDocumentClick = { /* TODO: Navigate to document edit/confirmation screen */ },
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
