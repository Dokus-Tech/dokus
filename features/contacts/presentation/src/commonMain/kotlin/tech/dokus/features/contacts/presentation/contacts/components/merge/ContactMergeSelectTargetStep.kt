package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_merge_from_label
import tech.dokus.aura.resources.contacts_merge_search_min_length
import tech.dokus.aura.resources.contacts_merge_search_no_results
import tech.dokus.aura.resources.contacts_merge_search_placeholder
import tech.dokus.aura.resources.contacts_merge_select_target_prompt
import tech.dokus.aura.resources.contacts_searching
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun ContactMergeSelectTargetStep(
    sourceContact: ContactDto,
    searchQuery: String,
    searchResults: List<ContactDto>,
    isSearching: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onTargetSelected: (ContactDto) -> Unit,
) {
    Column(
        modifier = Modifier.heightIn(
            min = Constraints.SearchField.minWidth,
            max = Constraints.DialogSize.maxWidth
        )
    ) {
        Text(
            text = stringResource(Res.string.contacts_merge_from_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ContactMergeMiniCard(
            contact = sourceContact,
            isSelected = false,
            onClick = null
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.contacts_merge_search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(Constraints.IconSize.smallMedium)
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

        when {
            isSearching -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Constraints.Spacing.large),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    DokusLoader(size = DokusLoaderSize.Small)
                    Spacer(modifier = Modifier.height(Constraints.Spacing.small))
                    Text(
                        text = stringResource(Res.string.contacts_searching),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            searchQuery.length < 2 -> {
                Text(
                    text = stringResource(Res.string.contacts_merge_search_min_length),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Constraints.Spacing.large)
                )
            }
            searchResults.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.contacts_merge_search_no_results, searchQuery),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = Constraints.Spacing.large)
                )
            }
            else -> {
                Text(
                    text = stringResource(Res.string.contacts_merge_select_target_prompt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(Constraints.Spacing.small))

                LazyColumn(
                    modifier = Modifier.heightIn(max = Constraints.SearchField.minWidth),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
                ) {
                    items(searchResults) { contact ->
                        ContactMergeMiniCard(
                            contact = contact,
                            isSelected = false,
                            onClick = { onTargetSelected(contact) }
                        )
                    }
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
private fun ContactMergeSelectTargetStepPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    val now = kotlinx.datetime.LocalDateTime(2026, 1, 15, 10, 0)
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        ContactMergeSelectTargetStep(
            sourceContact = ContactDto(
                id = tech.dokus.domain.ids.ContactId.generate(),
                tenantId = tech.dokus.domain.ids.TenantId.generate(),
                name = tech.dokus.domain.Name("Old Company NV"),
                vatNumber = tech.dokus.domain.ids.VatNumber("BE0123456789"),
                createdAt = now,
                updatedAt = now
            ),
            searchQuery = "",
            searchResults = emptyList(),
            isSearching = false,
            onSearchQueryChange = {},
            onTargetSelected = {}
        )
    }
}
