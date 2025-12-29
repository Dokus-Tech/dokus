package ai.dokus.app.contacts.components.create

import ai.dokus.app.contacts.viewmodel.CreateContactIntent
import ai.dokus.app.contacts.viewmodel.CreateContactState
import ai.dokus.app.contacts.viewmodel.DuplicateVatUi
import ai.dokus.app.contacts.viewmodel.LookupUiState
import ai.dokus.foundation.design.components.fields.PTextFieldStandard
import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import tech.dokus.domain.model.entity.EntityLookup

/**
 * Lookup step content - search by company name or VAT number.
 */
@Composable
fun LookupStepContent(
    state: CreateContactState.LookupStep,
    onIntent: (CreateContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Constrains.Spacing.large)
    ) {
        // Header
        LookupHeader(
            onClose = { onIntent(CreateContactIntent.Cancel) }
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.large))

        // Search field
        PTextFieldStandard(
            fieldName = "Search by company name or VAT number",
            value = state.query,
            icon = FeatherIcons.Search,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search
            ),
            showClearButton = state.query.isNotEmpty(),
            onClear = { onIntent(CreateContactIntent.QueryChanged("")) },
            onValueChange = { onIntent(CreateContactIntent.QueryChanged(it)) },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

        // Duplicate VAT warning (hard block)
        if (state.duplicateVat != null) {
            DuplicateVatBanner(
                duplicate = state.duplicateVat,
                onViewContact = { onIntent(CreateContactIntent.ViewExistingContact(state.duplicateVat.contactId)) },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
        }

        // Results area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (val lookupState = state.lookupState) {
                is LookupUiState.Idle -> {
                    // Show hint text
                    if (state.query.isEmpty() && state.duplicateVat == null) {
                        LookupHint()
                    }
                }
                is LookupUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is LookupUiState.Success -> {
                    LookupResultsList(
                        results = lookupState.results,
                        onSelect = { onIntent(CreateContactIntent.SelectResult(it)) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LookupUiState.Empty -> {
                    LookupEmptyState(
                        query = state.query,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LookupUiState.Error -> {
                    LookupErrorState(
                        message = lookupState.message,
                        onRetry = { onIntent(CreateContactIntent.QueryChanged(state.query)) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // "Add without VAT" link at bottom
        TextButton(
            onClick = { onIntent(CreateContactIntent.GoToManualEntry) },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Add without VAT lookup",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LookupHeader(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Add Contact",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LookupHint(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constrains.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = FeatherIcons.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
        Text(
            text = "Search for a company",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Enter a company name or VAT number",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LookupResultsList(
    results: List<EntityLookup>,
    onSelect: (EntityLookup) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        items(results) { entity ->
            LookupResultCard(
                entity = entity,
                onClick = { onSelect(entity) }
            )
        }
    }
}

@Composable
private fun LookupResultCard(
    entity: EntityLookup,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium)
        ) {
            Text(
                text = entity.name,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (entity.vatNumber != null) {
                Text(
                    text = entity.vatNumber.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entity.address != null) {
                Text(
                    text = "${entity.address.city}, ${entity.address.country.dbValue}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LookupEmptyState(
    query: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Constrains.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "No companies found",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "No results for \"$query\"",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LookupErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Constrains.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Search failed",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
        TextButton(onClick = onRetry) {
            Text("Retry")
        }
    }
}
