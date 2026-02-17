@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package tech.dokus.features.cashflow.presentation.review.components

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.cashflow_contact_create_new
import tech.dokus.aura.resources.cashflow_contact_no_matches
import tech.dokus.aura.resources.cashflow_contact_search_hint
import tech.dokus.aura.resources.cashflow_contact_search_to_begin
import tech.dokus.aura.resources.cashflow_contact_suggestions
import tech.dokus.aura.resources.cashflow_who_issued_document
import tech.dokus.aura.resources.contacts_selected
import tech.dokus.aura.resources.error_failed_to_load_clients
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.presentation.review.ContactSuggestion
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted

// Animation constants
private const val AnimationDurationMs = 200
private const val SlideAnimationDurationMs = 300
private const val ScrimAlpha = 0.32f

// Sizing constants
private val SidebarWidth = 420.dp
private val ContentPadding = 16.dp
private val ItemSpacing = 4.dp
private val ListItemPadding = 12.dp
private val ListItemSpacing = 12.dp
private val ContactIconSize = 24.dp
private val SelectedIndicatorSize = 20.dp
private val DragHandleWidth = 32.dp
private val DragHandleHeight = 4.dp
private val DragHandlePadding = 12.dp
private val ContentMinHeight = 200.dp
private val SuggestionChipCornerRadius = 8.dp

// Alpha constants
private const val SelectedAlpha = 0.5f
private const val DragHandleAlpha = 0.4f
private const val SuggestionBgAlpha = 0.08f

/**
 * Responsive contact selection sheet for the Document Review screen.
 *
 * - Desktop: Right side sheet (420dp width) with slide animation
 * - Mobile: Bottom sheet with drag handle
 *
 * Features:
 * - Top 3 suggestions always visible above search
 * - Search field for filtering contacts
 * - Contact list with selection indicator
 * - "Create new contact" action at bottom
 *
 * @param isVisible Whether the sheet is visible
 * @param onDismiss Callback when sheet should be dismissed
 * @param suggestions Top contact suggestions (max 3 shown)
 * @param contactsState All contacts for search/browse
 * @param selectedContactId Currently selected contact ID
 * @param searchQuery Current search query
 * @param onSearchQueryChange Callback when search query changes
 * @param onSelectContact Callback when a contact is selected
 * @param onCreateNewContact Callback to create a new contact
 */
@Composable
fun ContactEditSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    suggestions: List<ContactSuggestion>,
    contactsState: DokusState<List<ContactDto>>,
    selectedContactId: ContactId?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectContact: (ContactId) -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLargeScreen = LocalScreenSize.current.isLarge

    if (isLargeScreen) {
        ContactEditSideSheet(
            isVisible = isVisible,
            onDismiss = onDismiss,
            suggestions = suggestions,
            contactsState = contactsState,
            selectedContactId = selectedContactId,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSelectContact = onSelectContact,
            onCreateNewContact = onCreateNewContact,
            modifier = modifier
        )
    } else {
        ContactEditBottomSheet(
            isVisible = isVisible,
            onDismiss = onDismiss,
            suggestions = suggestions,
            contactsState = contactsState,
            selectedContactId = selectedContactId,
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            onSelectContact = onSelectContact,
            onCreateNewContact = onCreateNewContact,
            modifier = modifier
        )
    }
}

/**
 * Desktop side sheet variant - slides in from the right.
 */
@Composable
private fun ContactEditSideSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    suggestions: List<ContactSuggestion>,
    contactsState: DokusState<List<ContactDto>>,
    selectedContactId: ContactId?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectContact: (ContactId) -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop scrim
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

        // Side sheet panel
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
            DokusCardSurface(
                modifier = Modifier
                    .width(SidebarWidth)
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
                ContactSheetContent(
                    onDismiss = onDismiss,
                    suggestions = suggestions,
                    contactsState = contactsState,
                    selectedContactId = selectedContactId,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSearchQueryChange,
                    onSelectContact = onSelectContact,
                    onCreateNewContact = onCreateNewContact,
                )
            }
        }
    }
}

/**
 * Mobile bottom sheet variant.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactEditBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    suggestions: List<ContactSuggestion>,
    contactsState: DokusState<List<ContactDto>>,
    selectedContactId: ContactId?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectContact: (ContactId) -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = { BottomSheetDragHandle() },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
        ) {
            ContactSheetContent(
                onDismiss = onDismiss,
                suggestions = suggestions,
                contactsState = contactsState,
                selectedContactId = selectedContactId,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSelectContact = onSelectContact,
                onCreateNewContact = onCreateNewContact,
            )
        }
    }
}

@Composable
private fun BottomSheetDragHandle(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = DragHandlePadding),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(DragHandleWidth)
                .height(DragHandleHeight),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = DragHandleAlpha),
            shape = RoundedCornerShape(DragHandleHeight / 2)
        ) {}
    }
}

/**
 * Shared content for both sheet variants.
 */
@Composable
private fun ContactSheetContent(
    onDismiss: () -> Unit,
    suggestions: List<ContactSuggestion>,
    contactsState: DokusState<List<ContactDto>>,
    selectedContactId: ContactId?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectContact: (ContactId) -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(ContentPadding)
    ) {
        // Header
        ContactSheetHeader(onClose = onDismiss)

        Spacer(modifier = Modifier.height(ContentPadding))

        // Top 3 suggestions - always visible above search
        if (suggestions.isNotEmpty()) {
            SuggestionsSection(
                suggestions = suggestions.take(3),
                selectedContactId = selectedContactId,
                onSelectContact = onSelectContact,
            )

            Spacer(modifier = Modifier.height(ContentPadding))
        }

        // Search field
        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_contact_search_hint),
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(ContentPadding))

        // Contact list
        ContactList(
            contactsState = contactsState,
            selectedContactId = selectedContactId,
            searchQuery = searchQuery,
            onSelectContact = onSelectContact,
            modifier = Modifier.weight(1f)
        )

        // Create new contact button
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        TextButton(
            onClick = onCreateNewContact,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = stringResource(Res.string.cashflow_contact_create_new),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ContactSheetHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(Res.string.cashflow_who_issued_document),
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

/**
 * Top suggestions section - always visible above search.
 */
@Composable
private fun SuggestionsSection(
    suggestions: List<ContactSuggestion>,
    selectedContactId: ContactId?,
    onSelectContact: (ContactId) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
    ) {
        Text(
            text = stringResource(Res.string.cashflow_contact_suggestions),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted
        )

        suggestions.forEach { suggestion ->
            SuggestionChip(
                suggestion = suggestion,
                isSelected = selectedContactId == suggestion.contactId,
                onClick = { onSelectContact(suggestion.contactId) }
            )
        }
    }
}

@Composable
private fun SuggestionChip(
    suggestion: ContactSuggestion,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SuggestionChipCornerRadius))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pointerHoverIcon(PointerIcon.Hand),
        color = when {
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = SelectedAlpha)
            isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SelectedAlpha)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = SuggestionBgAlpha)
        },
        shape = RoundedCornerShape(SuggestionChipCornerRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ListItemPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = suggestion.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                suggestion.vatNumber?.let { vat ->
                    Text(
                        text = vat,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

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
}

@Composable
private fun ContactList(
    contactsState: DokusState<List<ContactDto>>,
    selectedContactId: ContactId?,
    searchQuery: String,
    onSelectContact: (ContactId) -> Unit,
    modifier: Modifier = Modifier
) {
    when (contactsState) {
        is DokusState.Loading -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                DokusLoader()
            }
        }

        is DokusState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
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
            val filteredContacts = contactsState.data.filter { contact ->
                if (searchQuery.isBlank()) {
                    true
                } else {
                    contact.name.value.contains(searchQuery, ignoreCase = true) ||
                        contact.email?.value?.contains(searchQuery, ignoreCase = true) == true ||
                        contact.vatNumber?.value?.contains(searchQuery, ignoreCase = true) == true
                }
            }

            if (filteredContacts.isEmpty()) {
                Box(
                    modifier = modifier
                        .fillMaxWidth()
                        .height(ContentMinHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.cashflow_contact_no_matches),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(ItemSpacing)
                ) {
                    items(
                        items = filteredContacts,
                        key = { it.id.value.toString() }
                    ) { contact ->
                        ContactListItem(
                            contact = contact,
                            isSelected = selectedContactId == contact.id,
                            onClick = { onSelectContact(contact.id) }
                        )
                    }
                }
            }
        }

        else -> {
            // Idle state - show empty state prompt
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(ContentMinHeight),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_contact_search_to_begin),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactListItem(
    contact: ContactDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

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
        // Contact icon
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(ContactIconSize)
        )

        // Contact info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            contact.vatNumber?.let { vat ->
                Text(
                    text = vat.value,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = FontFamily.Monospace
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            contact.email?.let { email ->
                Text(
                    text = email.value,
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
