package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_clear
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.contacts_add_new_contact
import tech.dokus.aura.resources.contacts_autocomplete_no_results
import tech.dokus.aura.resources.contacts_autocomplete_no_results_for
import tech.dokus.aura.resources.contacts_contact_label
import tech.dokus.aura.resources.contacts_customer
import tech.dokus.aura.resources.contacts_search_placeholder
import tech.dokus.aura.resources.contacts_searching
import tech.dokus.aura.resources.contacts_selected
import tech.dokus.aura.resources.contacts_supplier
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.usecases.ListContactsUseCase
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.platform.Logger

// UI dimension constants
private val DropdownTopPadding = 4.dp
private val DropdownElevation = 8.dp
private val DropdownCornerRadius = 8.dp
private val DropdownTonalElevation = 3.dp
private val DropdownMaxHeight = 300.dp
private val DropdownResultsMaxHeight = 240.dp
private val DropdownItemPadding = 12.dp
private val DropdownContentPadding = 16.dp
private val LoadingIndicatorSize = 20.dp
private val LoadingIndicatorStrokeWidth = 2.dp
private val ContentSpacing = 8.dp
private val BadgeCornerRadius = 4.dp
private val BadgePaddingHorizontal = 6.dp
private val BadgePaddingVertical = 2.dp
private val SelectedBadgeCornerRadius = 4.dp
private val AddIconSize = 20.dp
private val ClearButtonSize = 24.dp
private val ClearIconSize = 16.dp
private val RoleBadgeSpacing = 4.dp
private val RoleBadgeTopSpacing = 4.dp
private const val MinSearchLength = 2
private const val DebounceDelayMs = 300L
private const val SearchLimit = 10
private const val DisabledAlpha = 0.6f
private const val DividerAlpha = 0.5f
private const val BadgeBackgroundAlpha = 0.1f
private val FontSizeDefault = 16.sp

// ============================================================================
// DATA CLASSES
// ============================================================================

/**
 * Data class representing the auto-fill fields from a selected contact.
 * Use this to populate invoice/bill forms when a contact is selected.
 */
data class ContactAutoFillData(
    val contact: ContactDto,
    val vatNumber: String?,
    val addressLine1: String?,
    val addressLine2: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
    val defaultPaymentTerms: Int,
    val defaultVatRate: String?
)

// ============================================================================
// MAIN COMPONENT
// ============================================================================

/**
 * Autocomplete component for selecting contacts in invoice/bill forms.
 *
 * Features:
 * - Debounced search as user types (300ms delay)
 * - Searches by name, email, and VAT number
 * - Shows contact name, email, and role badges in dropdown
 * - Selecting a contact triggers auto-fill callback with contact data
 * - "Add new contact" option at bottom of dropdown
 * - Clear button to reset selection
 *
 * @param value The current text value in the field
 * @param onValueChange Callback when text changes
 * @param selectedContact The currently selected contact (if any)
 * @param onContactSelected Callback when a contact is selected with auto-fill data
 * @param onAddNewContact Callback when "Add new contact" is clicked
 * @param placeholder Placeholder text for the input field
 * @param label Label for the field
 * @param modifier Optional modifier
 * @param enabled Whether the field is enabled
 * @param isError Whether to show error state
 * @param errorMessage Error message to display
 */
@Composable
fun ContactAutocomplete(
    value: String,
    onValueChange: (String) -> Unit,
    selectedContact: ContactDto?,
    onContactSelected: (ContactAutoFillData) -> Unit,
    onAddNewContact: () -> Unit,
    placeholder: String? = null,
    label: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    listContacts: ListContactsUseCase = koinInject()
) {
    val scope = rememberCoroutineScope()
    val logger = remember { Logger.withTag("ContactAutocomplete") }
    val focusManager = LocalFocusManager.current
    val placeholderText = placeholder ?: stringResource(Res.string.contacts_search_placeholder)
    val labelText = label ?: stringResource(Res.string.contacts_contact_label)

    // State
    var searchQuery by remember { mutableStateOf(value) }
    var searchResults by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var showDropdown by remember { mutableStateOf(false) }
    var hasFocus by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Sync external value changes
    LaunchedEffect(value) {
        if (value != searchQuery) {
            searchQuery = value
        }
    }

    // Debounced search
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()

        // Don't search if a contact is already selected and the query matches
        if (selectedContact != null && searchQuery == selectedContact.name.value) {
            showDropdown = false
            return@LaunchedEffect
        }

        if (searchQuery.length >= MinSearchLength) {
            searchJob = scope.launch {
                delay(DebounceDelayMs) // Debounce delay
                isSearching = true

                listContacts(
                    search = searchQuery,
                    isActive = true,
                    limit = SearchLimit
                ).fold(
                    onSuccess = { contacts ->
                        searchResults = contacts
                        showDropdown = contacts.isNotEmpty() || searchQuery.isNotEmpty()
                        isSearching = false
                    },
                    onFailure = { error ->
                        logger.e(error) { "Contact search failed" }
                        searchResults = emptyList()
                        isSearching = false
                    }
                )
            }
        } else {
            searchResults = emptyList()
            showDropdown = false
        }
    }

    Column(modifier = modifier) {
        // Label
        Text(
            text = labelText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Constrains.Spacing.small)
        )

        // Input field with dropdown
        Box {
            ContactAutocompleteField(
                value = searchQuery,
                onValueChange = { newValue ->
                    searchQuery = newValue
                    onValueChange(newValue)
                    // Clear selection if user modifies the text
                    if (selectedContact != null && newValue != selectedContact.name.value) {
                        // Parent should handle clearing selection
                    }
                },
                placeholder = placeholderText,
                isError = isError,
                enabled = enabled,
                selectedContact = selectedContact,
                onClear = {
                    searchQuery = ""
                    onValueChange("")
                    searchResults = emptyList()
                    showDropdown = false
                },
                onFocusChanged = { focused ->
                    hasFocus = focused
                    if (focused && searchQuery.length >= MinSearchLength && selectedContact == null) {
                        showDropdown = true
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Dropdown popup
            if (showDropdown && hasFocus) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = { showDropdown = false },
                    properties = PopupProperties(focusable = false)
                ) {
                    ContactAutocompleteDropdown(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isSearching = isSearching,
                        onContactSelected = { contact ->
                            val autoFillData = ContactAutoFillData(
                                contact = contact,
                                vatNumber = contact.vatNumber?.value,
                                addressLine1 = contact.addressLine1,
                                addressLine2 = contact.addressLine2,
                                city = contact.city,
                                postalCode = contact.postalCode,
                                country = contact.country,
                                defaultPaymentTerms = contact.defaultPaymentTerms,
                                defaultVatRate = contact.defaultVatRate?.toString()
                            )
                            searchQuery = contact.name.value
                            onValueChange(contact.name.value)
                            onContactSelected(autoFillData)
                            showDropdown = false
                            focusManager.clearFocus()
                        },
                        onAddNewContact = {
                            showDropdown = false
                            onAddNewContact()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Error message
        if (isError && errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = Constrains.Spacing.xSmall)
            )
        }
    }
}

// ============================================================================
// INPUT FIELD
// ============================================================================

/**
 * The text input field for the autocomplete component.
 */
@Composable
private fun ContactAutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    enabled: Boolean,
    selectedContact: ContactDto?,
    onClear: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        selectedContact != null -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val textStyle = LocalTextStyle.current.copy(
        fontSize = FontSizeDefault,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
        }
    )

    Row(
        modifier = modifier
            .height(Constrains.Height.button)
            .border(Constrains.Stroke.thin, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = Constrains.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        // Search icon
        PIcon(
            icon = FeatherIcons.Search,
            description = stringResource(Res.string.action_search),
            modifier = Modifier.size(Constrains.IconSize.small)
        )

        // Text input
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = placeholder,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                singleLine = true,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                    },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner -> inner() }
            )
        }

        // Selected contact indicator or clear button
        if (selectedContact != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(SelectedBadgeCornerRadius)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_selected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical)
                )
            }
        }

        if (value.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(ClearButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(Res.string.action_clear),
                    modifier = Modifier.size(ClearIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ============================================================================
// DROPDOWN
// ============================================================================

/**
 * Dropdown showing search results and "Add new contact" option.
 */
@Composable
private fun ContactAutocompleteDropdown(
    searchQuery: String,
    searchResults: List<ContactDto>,
    isSearching: Boolean,
    onContactSelected: (ContactDto) -> Unit,
    onAddNewContact: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .padding(top = DropdownTopPadding)
            .shadow(elevation = DropdownElevation, shape = RoundedCornerShape(DropdownCornerRadius)),
        shape = RoundedCornerShape(DropdownCornerRadius),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = DropdownTonalElevation
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = DropdownMaxHeight)
        ) {
            when {
                isSearching -> {
                    // Loading state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DropdownContentPadding),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(LoadingIndicatorSize),
                            strokeWidth = LoadingIndicatorStrokeWidth
                        )
                        Spacer(modifier = Modifier.width(ContentSpacing))
                        Text(
                            text = stringResource(Res.string.contacts_searching),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                searchResults.isEmpty() && searchQuery.length >= MinSearchLength -> {
                    // No results state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(DropdownContentPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_autocomplete_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.contacts_autocomplete_no_results_for, searchQuery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    // Results list
                    LazyColumn(
                        modifier = Modifier.heightIn(max = DropdownResultsMaxHeight)
                    ) {
                        items(searchResults) { contact ->
                            ContactAutocompleteItem(
                                contact = contact,
                                onClick = { onContactSelected(contact) }
                            )
                        }
                    }
                }
            }

            // Add new contact option
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DividerAlpha)
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onAddNewContact),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DropdownItemPadding),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(AddIconSize),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(ContentSpacing))
                    Text(
                        text = stringResource(Res.string.contacts_add_new_contact),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// ============================================================================
// DROPDOWN ITEM
// ============================================================================

/**
 * Individual contact item in the autocomplete dropdown.
 * Shows contact name, email, and role badges.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactAutocompleteItem(
    contact: ContactDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(DropdownItemPadding)
        ) {
            // Name
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Email (if available)
            contact.email?.let { email ->
                Text(
                    text = email.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // VAT number (if available)
            contact.vatNumber?.let { vat ->
                Text(
                    text = stringResource(Res.string.common_vat_value, vat.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Role badges
            contact.derivedRoles?.let { roles ->
                if (roles.isCustomer || roles.isSupplier || roles.isVendor) {
                    Spacer(modifier = Modifier.height(RoleBadgeTopSpacing))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(RoleBadgeSpacing),
                        verticalArrangement = Arrangement.spacedBy(RoleBadgeSpacing)
                    ) {
                        if (roles.isCustomer) {
                            AutocompleteRoleBadge(
                                text = stringResource(Res.string.contacts_customer),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (roles.isSupplier) {
                            AutocompleteRoleBadge(
                                text = stringResource(Res.string.contacts_supplier),
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (roles.isVendor) {
                            AutocompleteRoleBadge(
                                text = stringResource(Res.string.contacts_vendor),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small badge showing contact role.
 */
@Composable
private fun AutocompleteRoleBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = BadgeBackgroundAlpha),
        shape = RoundedCornerShape(BadgeCornerRadius),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical)
        )
    }
}

// ============================================================================
// SIMPLIFIED VARIANT
// ============================================================================

/**
 * Simplified contact autocomplete that just returns the selected contact ID.
 * Use this when you only need the contact reference, not the auto-fill data.
 *
 * @param value The current text value
 * @param onValueChange Callback when text changes
 * @param selectedContact The currently selected contact (if any)
 * @param onContactSelected Callback when a contact is selected
 * @param onAddNewContact Callback when "Add new contact" is clicked
 * @param placeholder Placeholder text
 * @param label Field label
 * @param modifier Optional modifier
 */
@Composable
fun ContactAutocompleteSimple(
    value: String,
    onValueChange: (String) -> Unit,
    selectedContact: ContactDto?,
    onContactSelected: (ContactDto) -> Unit,
    onAddNewContact: () -> Unit,
    placeholder: String? = null,
    label: String? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    listContacts: ListContactsUseCase = koinInject()
) {
    ContactAutocomplete(
        value = value,
        onValueChange = onValueChange,
        selectedContact = selectedContact,
        onContactSelected = { autoFillData ->
            onContactSelected(autoFillData.contact)
        },
        onAddNewContact = onAddNewContact,
        placeholder = placeholder,
        label = label,
        modifier = modifier,
        enabled = enabled,
        isError = isError,
        errorMessage = errorMessage,
        listContacts = listContacts
    )
}
