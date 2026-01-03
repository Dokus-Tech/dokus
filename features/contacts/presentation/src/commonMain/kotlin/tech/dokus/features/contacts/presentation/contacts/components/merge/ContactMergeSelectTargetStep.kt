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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_merge_from_label
import tech.dokus.aura.resources.contacts_merge_search_min_length
import tech.dokus.aura.resources.contacts_merge_search_no_results
import tech.dokus.aura.resources.contacts_merge_search_placeholder
import tech.dokus.aura.resources.contacts_merge_select_target_prompt
import tech.dokus.aura.resources.contacts_searching
import tech.dokus.domain.model.contact.ContactDto

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
        modifier = Modifier.heightIn(min = 200.dp, max = 400.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(Res.string.contacts_merge_search_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isSearching -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(8.dp))
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            searchResults.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.contacts_merge_search_no_results, searchQuery),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            else -> {
                Text(
                    text = stringResource(Res.string.contacts_merge_select_target_prompt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
