package tech.dokus.features.contacts.presentation.contacts.components.autocomplete

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_add_new_contact
import tech.dokus.aura.resources.contacts_autocomplete_no_results
import tech.dokus.aura.resources.contacts_autocomplete_no_results_for
import tech.dokus.aura.resources.contacts_searching
import tech.dokus.domain.model.contact.ContactDto

/**
 * Dropdown showing search results and "Add new contact" option.
 */
@Composable
internal fun ContactDropdownMenu(
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
                        DokusLoader(size = DokusLoaderSize.Small)
                        Spacer(modifier = Modifier.width(ContentSpacing))
                        Text(
                            text = stringResource(Res.string.contacts_searching),
                            style = MaterialTheme.typography.bodyMedium,
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
                            style = MaterialTheme.typography.bodyMedium,
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
                            ContactSuggestionItem(
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
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactDropdownMenuPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactDropdownMenu(
            searchQuery = "Acme",
            searchResults = emptyList(),
            isSearching = false,
            onContactSelected = {},
            onAddNewContact = {}
        )
    }
}
