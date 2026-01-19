package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_contact_label
import tech.dokus.aura.resources.contacts_search_placeholder
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.presentation.contacts.components.autocomplete.ContactDropdownMenu
import tech.dokus.features.contacts.presentation.contacts.components.autocomplete.ContactSearchField
import tech.dokus.features.contacts.presentation.contacts.components.autocomplete.DebounceDelayMs
import tech.dokus.features.contacts.presentation.contacts.components.autocomplete.MinSearchLength
import tech.dokus.features.contacts.presentation.contacts.components.autocomplete.SearchLimit
import tech.dokus.features.contacts.usecases.FindContactsByNameUseCase
import tech.dokus.features.contacts.usecases.FindContactsByVatUseCase
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.platform.Logger

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
    findContactsByName: FindContactsByNameUseCase = koinInject(),
    findContactsByVat: FindContactsByVatUseCase = koinInject()
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

                val vatNumber = VatNumber(searchQuery)
                val searchResult = if (vatNumber.isValid) {
                    findContactsByVat(vatNumber, limit = SearchLimit)
                } else {
                    findContactsByName(searchQuery, limit = SearchLimit)
                }

                searchResult.fold(
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
            ContactSearchField(
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
                    ContactDropdownMenu(
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
    findContactsByName: FindContactsByNameUseCase = koinInject(),
    findContactsByVat: FindContactsByVatUseCase = koinInject()
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
        findContactsByName = findContactsByName,
        findContactsByVat = findContactsByVat
    )
}
