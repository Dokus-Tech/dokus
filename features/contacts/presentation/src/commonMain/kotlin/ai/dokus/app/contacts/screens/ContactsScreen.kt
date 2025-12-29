package ai.dokus.app.contacts.screens

import ai.dokus.app.contacts.components.ContactsFilters
import ai.dokus.app.contacts.components.ContactsFiltersMobile
import ai.dokus.app.contacts.components.ContactsHeaderActions
import ai.dokus.app.contacts.components.ContactsHeaderSearch
import ai.dokus.app.contacts.components.ContactsList
import ai.dokus.app.contacts.viewmodel.ContactActiveFilter
import ai.dokus.app.contacts.viewmodel.ContactRoleFilter
import ai.dokus.app.contacts.viewmodel.ContactSortOption
import ai.dokus.app.contacts.viewmodel.ContactsAction
import ai.dokus.app.contacts.viewmodel.ContactsContainer
import ai.dokus.app.contacts.viewmodel.ContactsIntent
import ai.dokus.app.contacts.viewmodel.ContactsState
import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.contacts_select_contact
import ai.dokus.app.resources.generated.contacts_select_contact_hint
import ai.dokus.foundation.design.components.common.PTopAppBarSearchAction
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.app.network.ConnectionSnackbarEffect
import tech.dokus.foundation.app.state.DokusState

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
    container: ContactsContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.isLarge

    // Subscribe to state and handle actions
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ContactsAction.NavigateToContactDetails -> {
                navController.navigateTo(
                    ContactsDestination.ContactDetails(action.contactId.toString())
                )
            }

            is ContactsAction.NavigateToCreateContact -> {
                navController.navigateTo(ContactsDestination.CreateContact)
            }

            is ContactsAction.NavigateToEditContact -> {
                navController.navigateTo(
                    ContactsDestination.EditContact(action.contactId.toString())
                )
            }

            is ContactsAction.ShowError -> {
                // TODO: Show snackbar error
            }

            is ContactsAction.ShowSuccess -> {
                // TODO: Show snackbar success
            }
        }
    }

    // Extract state values from Content state (with defaults for Loading/Error)
    val contentState = state as? ContactsState.Content
    val searchQuery = contentState?.searchQuery ?: ""
    val selectedContactId = contentState?.selectedContactId
    val sortOption = contentState?.sortOption ?: ContactSortOption.Default
    val roleFilter = contentState?.roleFilter ?: ContactRoleFilter.All
    val activeFilter = contentState?.activeFilter ?: ContactActiveFilter.All

    // Snackbar for connection status changes
    val snackbarHostState = remember { SnackbarHostState() }
    ConnectionSnackbarEffect(snackbarHostState)

    // Search expansion state for mobile
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val searchExpanded = isLargeScreen || isSearchExpanded

    // Load contacts on first composition
    LaunchedEffect(Unit) {
        container.store.intent(ContactsIntent.Refresh)
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
                        onSearchQueryChange = {
                            container.store.intent(
                                ContactsIntent.UpdateSearchQuery(
                                    it
                                )
                            )
                        },
                        isSearchExpanded = searchExpanded,
                        isLargeScreen = isLargeScreen,
                        onExpandSearch = { isSearchExpanded = true }
                    )
                },
                actions = {
                    ContactsHeaderActions(
                        onAddContactClick = {
                            // Navigate to new CreateContactScreen (VAT-first flow)
                            navController.navigateTo(ContactsDestination.CreateContact)
                        }
                    )
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        // Convert state to DokusState for ContactsList
        val contactsState: DokusState<PaginationState<ContactDto>> =
            when (state) {
                is ContactsState.Loading -> DokusState.loading()
                is ContactsState.Content -> DokusState.success((state as ContactsState.Content).contacts)
                is ContactsState.Error -> {
                    val errorState = state as ContactsState.Error
                    DokusState.error(errorState.exception, errorState.retryHandler)
                }
            }

        if (isLargeScreen) {
            // Desktop: Master-detail layout with inline create form
            DesktopContactsContent(
                contactsState = contactsState,
                selectedContactId = selectedContactId,
                sortOption = sortOption,
                roleFilter = roleFilter,
                activeFilter = activeFilter,
                contentPadding = contentPadding,
                onContactClick = { contact ->
                    container.store.intent(ContactsIntent.SelectContact(contact.id))
                },
                onLoadMore = { container.store.intent(ContactsIntent.LoadMore) },
                onAddContactClick = {
                    // Navigate to new CreateContactScreen (VAT-first flow)
                    navController.navigateTo(ContactsDestination.CreateContact)
                },
                onSortOptionSelected = { container.store.intent(ContactsIntent.UpdateSortOption(it)) },
                onRoleFilterSelected = { container.store.intent(ContactsIntent.UpdateRoleFilter(it)) },
                onActiveFilterSelected = {
                    container.store.intent(ContactsIntent.UpdateActiveFilter(it))
                },
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
                onLoadMore = { container.store.intent(ContactsIntent.LoadMore) },
                onAddContactClick = {
                    // Mobile: Navigate to full-screen form
                    navController.navigateTo(ContactsDestination.CreateContact)
                },
                onSortOptionSelected = { container.store.intent(ContactsIntent.UpdateSortOption(it)) },
                onRoleFilterSelected = { container.store.intent(ContactsIntent.UpdateRoleFilter(it)) },
                onActiveFilterSelected = {
                    container.store.intent(
                        ContactsIntent.UpdateActiveFilter(
                            it
                        )
                    )
                }
            )
        }
    }
}

/**
 * Desktop layout with master-detail panels.
 * Shows contact list on left (40%) and either contact details, create form, or placeholder on right (60%).
 */
@Composable
private fun DesktopContactsContent(
    contactsState: DokusState<PaginationState<ContactDto>>,
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
    onActiveFilterSelected: (ContactActiveFilter) -> Unit,
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

        // Right panel: Contact details or placeholder (60%)
        // Create contact is now handled via CreateContactScreen navigation
        Box(
            modifier = Modifier
                .weight(0.6f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
        ) {
            if (selectedContactId != null) {
                // Show contact details
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
    contactsState: DokusState<PaginationState<ContactDto>>,
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
