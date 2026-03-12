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
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_add_contact
import tech.dokus.aura.resources.contacts_select_contact
import tech.dokus.aura.resources.contacts_title
import tech.dokus.domain.Money
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.common.PaginationState
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.features.contacts.mvi.ContactDetailsState
import tech.dokus.features.contacts.mvi.ContactsIntent
import tech.dokus.features.contacts.mvi.ContactsState
import tech.dokus.features.contacts.presentation.contacts.components.ContactsList
import tech.dokus.features.contacts.presentation.contacts.route.ContactDetailsRoute
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.foundation.app.network.LocalServerConnection
import tech.dokus.foundation.app.network.ServerConnectionState
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIconPosition
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.ScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ContentPaddingHorizontal = 16.dp
private val SpacingMedium = 12.dp
private val SpacingDefault = 16.dp
private val MasterPaneWidth = 360.dp
private val DividerWidth = 1.dp
private val BottomPadding = 16.dp

/**
 * Main contacts screen with desktop master-detail and mobile list behavior.
 */
@Composable
internal fun ContactsScreen(
    state: ContactsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (ContactsIntent) -> Unit,
    onSelectContact: (ContactDto) -> Unit,
    onOpenContact: (ContactDto) -> Unit,
    onCreateContact: () -> Unit,
    detailContent: @Composable (ContactId) -> Unit = { contactId ->
        ContactDetailsRoute(
            contactId = contactId.toString(),
            showBackButton = false
        )
    }
) {
    val isLargeScreen = LocalScreenSize.isLarge
    val selectedContactId = state.selectedContactId
    val contacts = state.contacts.lastData?.data.orEmpty()

    val isOnline = rememberIsOnline()

    LaunchedEffect(isLargeScreen, selectedContactId, contacts.firstOrNull()?.id) {
        if (!isLargeScreen) return@LaunchedEffect
        if (selectedContactId != null) return@LaunchedEffect
        val first = contacts.firstOrNull() ?: return@LaunchedEffect
        onSelectContact(first)
    }

    val contactsState = state.contacts

    Scaffold(
        topBar = {
            if (!isLargeScreen) {
                PTopAppBar(Res.string.contacts_title) {
                    PButton(
                        text = stringResource(Res.string.contacts_add_contact),
                        variant = PButtonVariant.Outline,
                        icon = Lucide.Plus,
                        iconPosition = PIconPosition.Trailing,
                        onClick = onCreateContact,
                        isEnabled = isOnline
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        if (isLargeScreen) {
            DesktopContactsContent(
                contactsState = contactsState,
                selectedContactId = selectedContactId,
                contentPadding = contentPadding,
                onContactClick = onSelectContact,
                onLoadMore = { onIntent(ContactsIntent.LoadMore) },
                detailContent = detailContent
            )
        } else {
            MobileContactsContent(
                contactsState = contactsState,
                contentPadding = contentPadding,
                onContactClick = onOpenContact,
                onLoadMore = { onIntent(ContactsIntent.LoadMore) },
                onAddContactClick = onCreateContact
            )
        }
    }
}

@Composable
private fun DesktopContactsContent(
    contactsState: DokusState<PaginationState<ContactDto>>,
    selectedContactId: ContactId?,
    contentPadding: PaddingValues,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    detailContent: @Composable (ContactId) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier
                .width(MasterPaneWidth)
                .fillMaxHeight()
                .padding(horizontal = ContentPaddingHorizontal)
        ) {
            Spacer(modifier = Modifier.height(SpacingDefault))

            ContactsList(
                state = contactsState,
                onContactClick = onContactClick,
                onLoadMore = onLoadMore,
                onAddContactClick = null,
                contentPadding = PaddingValues(bottom = BottomPadding),
                modifier = Modifier.weight(1f),
                selectedContactId = selectedContactId,
                isDesktop = true,
            )
        }

        VerticalDivider(
            modifier = Modifier.fillMaxHeight(),
            thickness = DividerWidth,
            color = MaterialTheme.colorScheme.outlineVariant
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(horizontal = ContentPaddingHorizontal)
        ) {
            if (selectedContactId != null) {
                detailContent(selectedContactId)
            } else {
                NoContactSelectedPlaceholder()
            }
        }
    }
}

@Composable
private fun MobileContactsContent(
    contactsState: DokusState<PaginationState<ContactDto>>,
    contentPadding: PaddingValues,
    onContactClick: (ContactDto) -> Unit,
    onLoadMore: () -> Unit,
    onAddContactClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = ContentPaddingHorizontal)
    ) {
        Spacer(modifier = Modifier.height(SpacingMedium))
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

@Preview(widthDp = 1024, heightDp = 640)
@Composable
private fun ContactsDesktopMasterDetailPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 3, 3, 10, 0)
    val contactId = ContactId.generate()
    val contacts = desktopPreviewContacts(now, contactId)

    TestWrapper(parameters) {
        ServerConnectionPreviewProvider {
            CompositionLocalProvider(LocalScreenSize provides ScreenSize.LARGE) {
                ContactsScreen(
                    state = ContactsState(
                        contacts = DokusState.success(
                            PaginationState(
                                data = contacts,
                                currentPage = 1,
                                hasMorePages = true
                            )
                        ),
                        selectedContactId = contactId
                    ),
                    snackbarHostState = remember { SnackbarHostState() },
                    onIntent = {},
                    onSelectContact = {},
                    onOpenContact = {},
                    onCreateContact = {},
                    detailContent = {
                        ContactDetailsScreen(
                            state = ContactDetailsState(
                                contactId = contactId,
                                contact = DokusState.success(contacts.first()),
                                activityState = DokusState.success(
                                    ContactActivitySummary(contactId = contactId)
                                ),
                                invoiceSnapshotState = DokusState.success(
                                    ContactInvoiceSnapshot(
                                        documentsCount = 2,
                                        totalVolume = Money(53801),
                                        outstanding = Money(24901),
                                        recentDocuments = emptyList()
                                    )
                                ),
                                peppolStatusState = DokusState.success(
                                    PeppolStatusResponse(
                                        status = PeppolStatusResponse.STATUS_FOUND,
                                        refreshed = false
                                    )
                                ),
                                notesState = DokusState.success(emptyList<ContactNoteDto>())
                            ),
                            showBackButton = false,
                            isOnline = true,
                            snackbarHostState = remember { SnackbarHostState() },
                            onIntent = {},
                            onBackClick = {},
                            onDocumentClick = {}
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ServerConnectionPreviewProvider(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalServerConnection provides ServerConnectionState(
            isConnected = true,
            lastCheckTime = null,
            onRetry = {}
        )
    ) {
        content()
    }
}

@Preview
@Composable
private fun ContactsMobileListPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 3, 3, 10, 0)
    val contacts = desktopPreviewContacts(now, ContactId.generate())
    TestWrapper(parameters) {
        ServerConnectionPreviewProvider {
            ContactsScreen(
                state = ContactsState(
                    contacts = DokusState.success(
                        PaginationState(
                            data = contacts,
                            currentPage = 1,
                            hasMorePages = false
                        )
                    )
                ),
                snackbarHostState = remember { SnackbarHostState() },
                onIntent = {},
                onSelectContact = {},
                onOpenContact = {},
                onCreateContact = {}
            )
        }
    }
}

private fun desktopPreviewContacts(now: LocalDateTime, selectedId: ContactId): List<ContactDto> {
    return listOf(
        ContactDto(
            id = selectedId,
            tenantId = TenantId.generate(),
            name = Name("Coolblue België NV"),
            vatNumber = VatNumber("BE0867686774"),
            invoiceCount = 2,
            derivedRoles = DerivedContactRoles(isVendor = true),
            createdAt = now,
            updatedAt = now
        ),
        ContactDto(
            id = ContactId.generate(),
            tenantId = TenantId.generate(),
            name = Name("KBC Bank NV"),
            vatNumber = VatNumber("BE0462920226"),
            invoiceCount = 4,
            derivedRoles = DerivedContactRoles(isVendor = true),
            createdAt = now,
            updatedAt = now
        ),
        ContactDto(
            id = ContactId.generate(),
            tenantId = TenantId.generate(),
            name = Name("SRL Accounting & Tax"),
            vatNumber = VatNumber("BE0123456789"),
            invoiceCount = 1,
            derivedRoles = DerivedContactRoles(isVendor = true),
            createdAt = now,
            updatedAt = now
        )
    )
}
