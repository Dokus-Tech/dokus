package tech.dokus.features.contacts.presentation.contacts.screen

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
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_select_contact
import tech.dokus.aura.resources.nav_contacts
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.mvi.ContactActiveFilter
import tech.dokus.features.contacts.mvi.ContactRoleFilter
import tech.dokus.features.contacts.mvi.ContactSortOption
import tech.dokus.features.contacts.mvi.ContactsIntent
import tech.dokus.features.contacts.mvi.ContactsState
import tech.dokus.features.contacts.presentation.contacts.components.ContactsFilters
import tech.dokus.features.contacts.presentation.contacts.components.ContactsFiltersMobile
import tech.dokus.features.contacts.presentation.contacts.components.ContactsHeaderActions
import tech.dokus.features.contacts.presentation.contacts.components.ContactsHeaderSearch
import tech.dokus.features.contacts.presentation.contacts.components.ContactsList
import tech.dokus.features.contacts.presentation.contacts.route.ContactDetailsRoute
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// UI dimension constants
private val ContentPaddingHorizontal = 16.dp
private val SpacingMedium = 12.dp
private val SpacingDefault = 16.dp
private val MasterPaneWidth = 240.dp
private val DividerWidth = 1.dp
private val BottomPadding = 16.dp

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
    state: ContactsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactsIntent) -> Unit,
    onSelectContact: (ContactDto) -> Unit,
    onOpenContact: (ContactDto) -> Unit,
    onCreateContact: () -> Unit
) {
    val isLargeScreen = LocalScreenSize.isLarge

    // Extract state values from Content state (with defaults for Loading/Error)
    val contentState = state as? ContactsState.Content
    val searchQuery = contentState?.searchQuery ?: ""
    val selectedContactId = contentState?.selectedContactId
    val sortOption = contentState?.sortOption ?: ContactSortOption.Default
    val roleFilter = contentState?.roleFilter ?: ContactRoleFilter.All
    val activeFilter = contentState?.activeFilter ?: ContactActiveFilter.All

    // Search expansion state for mobile
    var isSearchExpanded by rememberSaveable { mutableStateOf(isLargeScreen) }
    val searchExpanded = isLargeScreen || isSearchExpanded

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
                            onIntent(
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
                            onCreateContact()
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
                    onSelectContact(contact)
                },
                onLoadMore = { onIntent(ContactsIntent.LoadMore) },
                onAddContactClick = {
                    // Navigate to new CreateContactScreen (VAT-first flow)
                    onCreateContact()
                },
                onSortOptionSelected = { onIntent(ContactsIntent.UpdateSortOption(it)) },
                onRoleFilterSelected = { onIntent(ContactsIntent.UpdateRoleFilter(it)) },
                onActiveFilterSelected = {
                    onIntent(ContactsIntent.UpdateActiveFilter(it))
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
                    onOpenContact(contact)
                },
                onLoadMore = { onIntent(ContactsIntent.LoadMore) },
                onAddContactClick = {
                    // Mobile: Navigate to full-screen form
                    onCreateContact()
                },
                onSortOptionSelected = { onIntent(ContactsIntent.UpdateSortOption(it)) },
                onRoleFilterSelected = { onIntent(ContactsIntent.UpdateRoleFilter(it)) },
                onActiveFilterSelected = {
                    onIntent(
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
        // Left panel: Contacts list with filters (240dp)
        Column(
            modifier = Modifier
                .width(MasterPaneWidth)
                .fillMaxHeight()
                .padding(horizontal = ContentPaddingHorizontal)
        ) {
            // Filter controls
            Spacer(modifier = Modifier.height(SpacingDefault))
            ContactsFilters(
                selectedSortOption = sortOption,
                selectedRoleFilter = roleFilter,
                selectedActiveFilter = activeFilter,
                onSortOptionSelected = onSortOptionSelected,
                onRoleFilterSelected = onRoleFilterSelected,
                onActiveFilterSelected = onActiveFilterSelected
            )
            Spacer(modifier = Modifier.height(SpacingMedium))

            // Contacts list
            ContactsList(
                state = contactsState,
                onContactClick = onContactClick,
                onLoadMore = onLoadMore,
                onAddContactClick = onAddContactClick,
                contentPadding = PaddingValues(bottom = BottomPadding),
                modifier = Modifier.weight(1f),
                selectedContactId = selectedContactId,
                isDesktop = true,
            )
        }

        // Vertical divider
        HorizontalDivider(
            modifier = Modifier
                .fillMaxHeight()
                .width(DividerWidth),
            color = MaterialTheme.colorScheme.outlineVariant
        )

        // Right panel: Contact details or placeholder (flex)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            if (selectedContactId != null) {
                // Show contact details
                ContactDetailsRoute(
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
            .padding(horizontal = ContentPaddingHorizontal)
    ) {
        // Filter controls
        Spacer(modifier = Modifier.height(SpacingDefault))
        ContactsFiltersMobile(
            selectedSortOption = sortOption,
            selectedRoleFilter = roleFilter,
            selectedActiveFilter = activeFilter,
            onSortOptionSelected = onSortOptionSelected,
            onRoleFilterSelected = onRoleFilterSelected,
            onActiveFilterSelected = onActiveFilterSelected
        )
        Spacer(modifier = Modifier.height(SpacingMedium))

        // Contacts list
        ContactsList(
            state = contactsState,
            onContactClick = onContactClick,
            onLoadMore = onLoadMore,
            onAddContactClick = onAddContactClick,
            contentPadding = PaddingValues(bottom = BottomPadding),
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
        Text(
            text = stringResource(Res.string.contacts_select_contact),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// Preview skipped: Flaky IllegalStateException in parallel Roborazzi runs
