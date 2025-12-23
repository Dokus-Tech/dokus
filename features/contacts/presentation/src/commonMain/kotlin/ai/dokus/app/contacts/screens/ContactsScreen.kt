package ai.dokus.app.contacts.screens

import ai.dokus.app.contacts.components.ContactsFilters
import ai.dokus.app.contacts.components.ContactsFiltersMobile
import ai.dokus.app.contacts.components.ContactsHeaderActions
import ai.dokus.app.contacts.components.ContactsHeaderSearch
import ai.dokus.app.contacts.components.ContactsList
import ai.dokus.app.contacts.viewmodel.ContactActiveFilter
import ai.dokus.app.contacts.viewmodel.ContactRoleFilter
import ai.dokus.app.contacts.viewmodel.ContactSortOption
import ai.dokus.app.contacts.viewmodel.ContactsViewModel
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_select_contact
import ai.dokus.app.resources.generated.contacts_select_contact_hint
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.navigation.destinations.ContactsDestination
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.navigateTo
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * The main contacts screen showing a list of contacts with master-detail layout.
 *
 * Desktop layout:
 * - Left panel (40%): Contacts list with search and filters
 * - Right panel (60%): Contact details panel (updates when a contact is selected)
 *
 * Mobile layout:
 * - Full-screen contacts list
 * - Tapping a contact navigates to the ContactDetailsScreen
 */
@Composable
internal fun ContactsScreen(
    viewModel: ContactsViewModel = koinViewModel()
) {
    val contactsState by viewModel.state.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedContactId by viewModel.selectedContactId.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val roleFilter by viewModel.roleFilter.collectAsState()
    val activeFilter by viewModel.activeFilter.collectAsState()
    val navController = LocalNavController.current

    val isLargeScreen = LocalScreenSize.current.isLarge

    // Search expansion state for mobile
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val searchExpanded = isLargeScreen || isSearchExpanded

    // Load contacts on first composition
    LaunchedEffect(viewModel) {
        viewModel.refresh()
    }

    // Reset mobile search expansion when rotating to large screen (desktop)
    LaunchedEffect(isLargeScreen) {
        if (isLargeScreen) isSearchExpanded = false
    }

    Scaffold(
        topBar = {
            PTopAppBarSearchAction(
                searchContent = {
                    ContactsHeaderSearch(
                        searchQuery = searchQuery,
                        onSearchQueryChange = viewModel::updateSearchQuery,
                        isSearchExpanded = searchExpanded,
                        isLargeScreen = isLargeScreen,
                        onExpandSearch = { isSearchExpanded = true }
                    )
                },
                actions = {
                    ContactsHeaderActions(
                        onAddContactClick = {
                            navController.navigateTo(ContactsDestination.CreateContact)
                        }
                    )
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        if (isLargeScreen) {
            // Desktop: Master-detail layout
            DesktopContactsContent(
                contactsState = contactsState,
                selectedContactId = selectedContactId,
                sortOption = sortOption,
                roleFilter = roleFilter,
                activeFilter = activeFilter,
                contentPadding = contentPadding,
                onContactClick = { contact ->
                    viewModel.selectContact(contact.id)
                },
                onLoadMore = viewModel::loadNextPage,
                onAddContactClick = {
                    navController.navigateTo(ContactsDestination.CreateContact)
                },
                onSortOptionSelected = viewModel::updateSortOption,
                onRoleFilterSelected = viewModel::updateRoleFilter,
                onActiveFilterSelected = viewModel::updateActiveFilter
            )
        } else {
            // Mobile: Full-screen list
            MobileContactsContent(
                contactsState = contactsState,
                sortOption = sortOption,
                roleFilter = roleFilter,
                activeFilter = activeFilter,
                contentPadding = contentPadding,
                onContactClick = { contact ->
                    navController.navigateTo(
                        ContactsDestination.ContactDetails(contact.id.toString())
                    )
                },
                onLoadMore = viewModel::loadNextPage,
                onAddContactClick = {
                    navController.navigateTo(ContactsDestination.CreateContact)
                },
                onSortOptionSelected = viewModel::updateSortOption,
                onRoleFilterSelected = viewModel::updateRoleFilter,
                onActiveFilterSelected = viewModel::updateActiveFilter
            )
        }
    }
}

/**
 * Desktop layout with master-detail panels.
 */
@Composable
private fun DesktopContactsContent(
    contactsState: tech.dokus.foundation.app.state.DokusState<ai.dokus.foundation.domain.model.common.PaginationState<ContactDto>>,
    selectedContactId: ContactId?,
    sortOption: ContactSortOption,
    roleFilter: ContactRoleFilter,
    activeFilter: ContactActiveFilter,
    contentPadding: PaddingValues,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    onAddContactClick: () -> Unit,
    onSortOptionSelected: (ContactSortOption) -> Unit,
    onRoleFilterSelected: (ContactRoleFilter) -> Unit,
    onActiveFilterSelected: (ContactActiveFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        // Left panel: Contacts list with filters (40%)
        Column(
            modifier = Modifier
                .weight(0.4f)
                .fillMaxHeight()
                .padding(horizontal = 16.dp)
        ) {
            // Filter controls
            Spacer(modifier = Modifier.height(16.dp))
            ContactsFilters(
                selectedSortOption = sortOption,
                selectedRoleFilter = roleFilter,
                selectedActiveFilter = activeFilter,
                onSortOptionSelected = onSortOptionSelected,
                onRoleFilterSelected = onRoleFilterSelected,
                onActiveFilterSelected = onActiveFilterSelected
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Contacts list
            ContactsList(
                state = contactsState,
                onContactClick = onContactClick,
                onLoadMore = onLoadMore,
                onAddContactClick = onAddContactClick,
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            )
        }

        // Vertical divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Right panel: Contact details (60%)
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            if (selectedContactId != null) {
                // Show the actual ContactDetailsScreen embedded in the detail panel
                ContactDetailsScreen(
                    contactId = selectedContactId,
                    showBackButton = false
                )
            } else {
                // No contact selected placeholder
                NoContactSelectedPlaceholder()
            }
        }
    }
}

/**
 * Mobile layout with full-screen contacts list.
 */
@Composable
private fun MobileContactsContent(
    contactsState: tech.dokus.foundation.app.state.DokusState<ai.dokus.foundation.domain.model.common.PaginationState<ContactDto>>,
    sortOption: ContactSortOption,
    roleFilter: ContactRoleFilter,
    activeFilter: ContactActiveFilter,
    contentPadding: PaddingValues,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    onAddContactClick: () -> Unit,
    onSortOptionSelected: (ContactSortOption) -> Unit,
    onRoleFilterSelected: (ContactRoleFilter) -> Unit,
    onActiveFilterSelected: (ContactActiveFilter) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 16.dp)
    ) {
        // Filter controls
        Spacer(modifier = Modifier.height(16.dp))
        ContactsFiltersMobile(
            selectedSortOption = sortOption,
            selectedRoleFilter = roleFilter,
            selectedActiveFilter = activeFilter,
            onSortOptionSelected = onSortOptionSelected,
            onRoleFilterSelected = onRoleFilterSelected,
            onActiveFilterSelected = onActiveFilterSelected
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Contacts list
        ContactsList(
            state = contactsState,
            onContactClick = onContactClick,
            onLoadMore = onLoadMore,
            onAddContactClick = onAddContactClick,
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Placeholder when no contact is selected in the detail panel.
 */
@Composable
private fun NoContactSelectedPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .height(48.dp)
                    .width(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(Res.string.contacts_select_contact),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.contacts_select_contact_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}
