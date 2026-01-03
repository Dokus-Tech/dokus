package tech.dokus.features.contacts.presentation.contacts.components.create

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.contacts_add_contact
import tech.dokus.aura.resources.contacts_add_new_contact
import tech.dokus.aura.resources.contacts_add_without_vat
import tech.dokus.aura.resources.contacts_autocomplete_no_results_for
import tech.dokus.aura.resources.contacts_lookup_empty
import tech.dokus.aura.resources.contacts_lookup_hint
import tech.dokus.aura.resources.contacts_lookup_label
import tech.dokus.aura.resources.contacts_lookup_location
import tech.dokus.aura.resources.contacts_lookup_no_results
import tech.dokus.aura.resources.contacts_lookup_query_hint
import tech.dokus.aura.resources.contacts_lookup_search_failed
import tech.dokus.aura.resources.contacts_select_contact
import tech.dokus.aura.resources.country_belgium
import tech.dokus.aura.resources.country_france
import tech.dokus.aura.resources.country_netherlands
import tech.dokus.aura.resources.state_retry
import tech.dokus.domain.enums.Country
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.features.contacts.mvi.CreateContactIntent
import tech.dokus.features.contacts.mvi.CreateContactState
import tech.dokus.features.contacts.mvi.LookupUiState
import tech.dokus.features.contacts.usecases.FindContactsByNameUseCase
import tech.dokus.features.contacts.usecases.FindContactsByVatUseCase
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized

private const val SEARCH_DEBOUNCE_MS = 500L
private const val MIN_SEARCH_LENGTH = 3

/**
 * Lookup step content - search by company name or VAT number.
 *
 * The search query is kept as local UI state to avoid TextField race conditions.
 * Changes are observed via snapshotFlow and debounced before triggering search.
 */
@OptIn(FlowPreview::class)
@Composable
fun LookupStepContent(
    state: CreateContactState.LookupStep,
    onIntent: (CreateContactIntent) -> Unit,
    initialQuery: String? = null,
    onExistingContactSelected: ((String) -> Unit)? = null,
    findContactsByName: FindContactsByNameUseCase = koinInject(),
    findContactsByVat: FindContactsByVatUseCase = koinInject(),
    modifier: Modifier = Modifier,
) {
    // Keep query as local state to avoid MVI race conditions
    var query by rememberSaveable { mutableStateOf(initialQuery.orEmpty()) }
    var existingContacts by remember { mutableStateOf(emptyList<ContactDto>()) }
    var isExistingLoading by remember { mutableStateOf(false) }
    var selectedExistingId by remember { mutableStateOf<ContactId?>(null) }

    LaunchedEffect(initialQuery) {
        if (!initialQuery.isNullOrBlank() && query.isBlank()) {
            query = initialQuery
        }
    }

    // Observe query changes with debounce and trigger search
    LaunchedEffect(Unit) {
        snapshotFlow { query }
            .distinctUntilChanged()
            .debounce(SEARCH_DEBOUNCE_MS)
            .collect { searchQuery ->
                selectedExistingId = null
                if (searchQuery.length >= MIN_SEARCH_LENGTH) {
                    onIntent(CreateContactIntent.Search(searchQuery))
                    if (onExistingContactSelected != null) {
                        isExistingLoading = true
                        val vatNumber = VatNumber(searchQuery)
                        val searchResult = if (vatNumber.isValid) {
                            findContactsByVat(vatNumber, limit = 10)
                        } else {
                            findContactsByName(searchQuery, limit = 10)
                        }
                        searchResult
                            .onSuccess { existingContacts = it }
                            .onFailure { existingContacts = emptyList() }
                        isExistingLoading = false
                    }
                } else {
                    onIntent(CreateContactIntent.ClearSearch)
                    existingContacts = emptyList()
                    isExistingLoading = false
                }
            }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Constrains.Spacing.large)
    ) {
        // Header
        LookupHeader(
            onClose = { onIntent(CreateContactIntent.Cancel) }
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

        // Step indicator
        StepIndicator(
            currentStep = CreateContactStep.Search,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.large))

        // Search field - uses local state, NOT MVI state
        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_lookup_label),
            value = query,
            icon = FeatherIcons.Search,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Search
            ),
            showClearButton = query.isNotEmpty(),
            onClear = { query = "" },
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

        if (onExistingContactSelected != null) {
            ExistingContactsSection(
                query = query,
                contacts = existingContacts,
                isLoading = isExistingLoading,
                selectedId = selectedExistingId,
                onSelect = { selectedExistingId = it },
                onConfirm = {
                    selectedExistingId?.let { onExistingContactSelected(it.toString()) }
                },
                onCreateNew = { onIntent(CreateContactIntent.GoToManualEntry) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
        }

        // Duplicate VAT warning (hard block)
        val duplicateVat = state.duplicateVat
        if (duplicateVat != null) {
            DuplicateVatBanner(
                duplicate = duplicateVat,
                onViewContact = { onIntent(CreateContactIntent.ViewExistingContact(duplicateVat.contactId)) },
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
                    // Show hint text when query is empty
                    if (query.isEmpty() && duplicateVat == null) {
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
                        query = query,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is LookupUiState.Error -> {
                    LookupErrorState(
                        exception = lookupState.exception,
                        onRetry = { onIntent(CreateContactIntent.Search(query)) },
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
                text = if (onExistingContactSelected != null) {
                    stringResource(Res.string.contacts_add_new_contact)
                } else {
                    stringResource(Res.string.contacts_add_without_vat)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExistingContactsSection(
    query: String,
    contacts: List<ContactDto>,
    isLoading: Boolean,
    selectedId: ContactId?,
    onSelect: (ContactId) -> Unit,
    onConfirm: () -> Unit,
    onCreateNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.contacts_select_contact),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            query.length >= MIN_SEARCH_LENGTH && contacts.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.contacts_autocomplete_no_results_for, query),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                POutlinedButton(
                    text = stringResource(Res.string.contacts_add_new_contact),
                    onClick = onCreateNew,
                    modifier = Modifier.align(Alignment.Start)
                )
            }
            contacts.isNotEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
                ) {
                    contacts.forEach { contact ->
                        val isSelected = contact.id == selectedId
                        DokusCardSurface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(contact.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Constrains.Spacing.medium),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name.value,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    val secondary = listOfNotNull(
                                        contact.vatNumber?.value,
                                        contact.email?.value
                                    ).joinToString(" • ")
                                    if (secondary.isNotBlank()) {
                                        Text(
                                            text = secondary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    PPrimaryButton(
                        text = stringResource(Res.string.contacts_select_contact),
                        enabled = selectedId != null,
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
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
            text = stringResource(Res.string.contacts_add_contact),
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
            text = stringResource(Res.string.contacts_lookup_hint),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.contacts_lookup_query_hint),
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
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium)
        ) {
            Text(
                text = entity.name.value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val vatNumber = entity.vatNumber
            if (vatNumber != null) {
                Text(
                    text = vatNumber.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            val address = entity.address
            if (address != null) {
                Text(
                    text = stringResource(
                        Res.string.contacts_lookup_location,
                        address.city,
                        address.country.localizedName()
                    ),
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
            text = stringResource(Res.string.contacts_lookup_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(Res.string.contacts_lookup_no_results, query),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun LookupErrorState(
    exception: DokusException,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(Constrains.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(Res.string.contacts_lookup_search_failed),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            text = exception.localized,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
        TextButton(onClick = onRetry) {
            Text(stringResource(Res.string.state_retry))
        }
    }
}

@Composable
private fun Country.localizedName(): String =
    when (this) {
        Country.Belgium -> stringResource(Res.string.country_belgium)
        Country.Netherlands -> stringResource(Res.string.country_netherlands)
        Country.France -> stringResource(Res.string.country_france)
    }
