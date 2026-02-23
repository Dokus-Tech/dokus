@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.contacts_selected
import tech.dokus.aura.resources.error_failed_to_load_clients
import tech.dokus.aura.resources.invoice_no_clients_found
import tech.dokus.aura.resources.invoice_no_clients_match
import tech.dokus.aura.resources.invoice_search_clients
import tech.dokus.aura.resources.invoice_select_client
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.DokusStateSimple
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val AnimationDurationMs = 200
private const val SlideAnimationDurationMs = 300
private const val ScrimAlpha = 0.32f
private const val SelectedAlpha = 0.5f
private const val SidebarWidthDivisor = 3
private val SidebarMinWidth = 320.dp
private val SidebarMaxWidth = 400.dp
private val ContentPadding = 16.dp
private val ItemSpacing = 4.dp
private val ErrorIconSpacing = 8.dp
private val ListItemPadding = 12.dp
private val ListItemSpacing = 12.dp
private val ClientIconSize = 24.dp
private val SelectedIndicatorSize = 20.dp
private val ClientNameSpacing = 8.dp

/**
 * Side panel for selecting a client in the invoice creation flow.
 * Shows searchable list of clients with selection state.
 */
@Composable
fun InvoiceClientSidePanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    clientsState: DokusState<List<ContactDto>>,
    selectedClient: ContactDto?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectClient: (ContactDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(AnimationDurationMs)),
            exit = fadeOut(tween(AnimationDurationMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Sidebar
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(SlideAnimationDurationMs)
            ) + fadeIn(tween(SlideAnimationDurationMs)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(SlideAnimationDurationMs)
            ) + fadeOut(tween(SlideAnimationDurationMs)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val sidebarWidth = (maxWidth / SidebarWidthDivisor).coerceIn(SidebarMinWidth, SidebarMaxWidth)

                DokusCardSurface(
                    modifier = Modifier
                        .width(sidebarWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = MaterialTheme.shapes.medium.copy(
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(ContentPadding)
                    ) {
                        // Header
                        ClientSidePanelHeader(onClose = onDismiss)

                        Spacer(modifier = Modifier.height(ContentPadding))

                        // Search field
                        PTextFieldStandard(
                            fieldName = stringResource(Res.string.invoice_search_clients),
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(ContentPadding))

                        // Client list
                        when (clientsState) {
                            is DokusState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DokusLoader()
                                }
                            }

                            is DokusState.Error -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(ErrorIconSpacing)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = stringResource(Res.string.error_failed_to_load_clients),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            is DokusState.Success -> {
                                val filteredClients = clientsState.data.filter { client ->
                                    if (searchQuery.isBlank()) {
                                        true
                                    } else {
                                        client.name.value.contains(searchQuery, ignoreCase = true) ||
                                            client.email?.value?.contains(searchQuery, ignoreCase = true) == true
                                    }
                                }

                                if (filteredClients.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (searchQuery.isBlank()) {
                                                stringResource(Res.string.invoice_no_clients_found)
                                            } else {
                                                stringResource(Res.string.invoice_no_clients_match, searchQuery)
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(ItemSpacing)
                                    ) {
                                        items(
                                            items = filteredClients,
                                            key = { it.id.value.toString() }
                                        ) { client ->
                                            ClientListItem(
                                                client = client,
                                                isSelected = selectedClient?.id == client.id,
                                                onClick = {
                                                    onSelectClient(client)
                                                    onDismiss()
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Idle state
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    DokusLoader()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientSidePanelHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.invoice_select_client),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.action_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ClientListItem(
    client: ContactDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    // NOTE: PEPPOL status is now resolved via PeppolRecipientResolver at send time
    // The warning for missing PEPPOL ID is no longer shown in the client list

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = SelectedAlpha)
                    isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SelectedAlpha)
                    else -> MaterialTheme.colorScheme.surface
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(ListItemPadding),
        horizontalArrangement = Arrangement.spacedBy(ListItemSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Client icon
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(ClientIconSize)
        )

        // Client info
        Column(modifier = Modifier.weight(1f)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ClientNameSpacing),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = client.name.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            client.email?.let { email ->
                Text(
                    text = email.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (client.country != null) {
                Text(
                    text = client.country!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Selected indicator
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = stringResource(Res.string.contacts_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(SelectedIndicatorSize)
            )
        }
    }
}

@Preview
@Composable
private fun InvoiceClientSidePanelPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceClientSidePanel(
            isVisible = true,
            onDismiss = {},
            clientsState = DokusStateSimple.Loading(),
            selectedClient = null,
            searchQuery = "",
            onSearchQueryChange = {},
            onSelectClient = {}
        )
    }
}
