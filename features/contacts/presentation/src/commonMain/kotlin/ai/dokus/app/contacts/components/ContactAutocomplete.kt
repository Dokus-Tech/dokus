package ai.dokus.app.contacts.components

import ai.dokus.app.contacts.repository.ContactRepository
import ai.dokus.foundation.design.components.PIcon
import ai.dokus.foundation.design.constrains.Constrains
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.platform.Logger
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
import androidx.compose.material.icons.filled.Search
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
import org.koin.compose.koinInject

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
    placeholder: String = "Search contacts...",
    label: String = "Contact",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    contactRepository: ContactRepository = koinInject()
) {
    val scope = rememberCoroutineScope()
    val logger = remember { Logger.forClass("ContactAutocomplete") }
    val focusManager = LocalFocusManager.current

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

        if (searchQuery.length >= 2) {
            searchJob = scope.launch {
                delay(300) // Debounce delay
                isSearching = true

                contactRepository.listContacts(
                    search = searchQuery,
                    isActive = true,
                    limit = 10
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
            text = label,
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
                placeholder = placeholder,
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
                    if (focused && searchQuery.length >= 2 && selectedContact == null) {
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
                                defaultVatRate = contact.defaultVatRate?.value?.toString()
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
        fontSize = 16.sp,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
            description = "Search",
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
                shape = RoundedCornerShape(4.dp)
            ) {
                Text(
                    text = "Selected",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        if (value.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Clear",
                    modifier = Modifier.size(16.dp),
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
            .padding(top = 4.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            when {
                isSearching -> {
                    // Loading state
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Searching...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                searchResults.isEmpty() && searchQuery.length >= 2 -> {
                    // No results state
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No contacts found",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "for \"$searchQuery\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                else -> {
                    // Results list
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 240.dp)
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
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
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
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add new contact",
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
                .padding(12.dp)
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
                    text = "VAT: ${vat.value}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Role badges
            contact.derivedRoles?.let { roles ->
                if (roles.isCustomer || roles.isSupplier || roles.isVendor) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (roles.isCustomer) {
                            AutocompleteRoleBadge(
                                text = "Customer",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (roles.isSupplier) {
                            AutocompleteRoleBadge(
                                text = "Supplier",
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                        if (roles.isVendor) {
                            AutocompleteRoleBadge(
                                text = "Vendor",
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
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    placeholder: String = "Search contacts...",
    label: String = "Contact",
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    contactRepository: ContactRepository = koinInject()
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
        contactRepository = contactRepository
    )
}
