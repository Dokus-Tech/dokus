package tech.dokus.features.contacts.presentation.contacts.components.create

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.contacts_create_contact
import tech.dokus.aura.resources.contacts_lookup_hint
import tech.dokus.aura.resources.contacts_lookup_label
import tech.dokus.aura.resources.contacts_lookup_no_matches
import tech.dokus.aura.resources.contacts_lookup_query_hint
import tech.dokus.aura.resources.contacts_lookup_search_failed
import tech.dokus.aura.resources.contacts_resolve_counterparty_create
import tech.dokus.aura.resources.contacts_trust_external
import tech.dokus.aura.resources.contacts_trust_local
import tech.dokus.aura.resources.contacts_trust_verified
import tech.dokus.aura.resources.state_retry
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
    headerTitle: String,
    isResolveFlow: Boolean,
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
                if (searchQuery.length >= MIN_SEARCH_LENGTH) {
                    onIntent(CreateContactIntent.Search(searchQuery))
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
            title = headerTitle,
            onClose = { onIntent(CreateContactIntent.Cancel) }
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

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
            if (query.isEmpty() && duplicateVat == null) {
                LookupHint()
            } else if (query.length < MIN_SEARCH_LENGTH) {
                LookupHint()
            } else {
                UnifiedResultsList(
                    query = query,
                    lookupState = state.lookupState,
                    existingContacts = existingContacts,
                    isExistingLoading = isExistingLoading,
                    onSelectExisting = { contactId ->
                        if (onExistingContactSelected != null) {
                            onExistingContactSelected(contactId.toString())
                        } else {
                            onIntent(CreateContactIntent.ViewExistingContact(contactId))
                        }
                    },
                    onSelectRegistry = { onIntent(CreateContactIntent.SelectResult(it)) },
                    onRetryRegistry = { onIntent(CreateContactIntent.Search(query)) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        val hasResults = remember(existingContacts, state.lookupState) {
            val externalCount = (state.lookupState as? LookupUiState.Success)?.results?.size ?: 0
            existingContacts.isNotEmpty() || externalCount > 0
        }
        if (!hasResults && state.lookupState !is LookupUiState.Loading && !isExistingLoading) {
            PPrimaryButton(
                text = if (isResolveFlow) {
                    stringResource(Res.string.contacts_resolve_counterparty_create)
                } else {
                    stringResource(Res.string.contacts_create_contact)
                },
                onClick = { onIntent(CreateContactIntent.GoToManualEntry) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Constrains.Height.button)
            )
        } else {
            TextButton(
                onClick = { onIntent(CreateContactIntent.GoToManualEntry) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
                enabled = state.lookupState !is LookupUiState.Loading
            ) {
                Text(
                    text = stringResource(Res.string.contacts_create_contact),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun UnifiedResultsList(
    query: String,
    lookupState: LookupUiState,
    existingContacts: List<ContactDto>,
    isExistingLoading: Boolean,
    onSelectExisting: (ContactId) -> Unit,
    onSelectRegistry: (EntityLookup) -> Unit,
    onRetryRegistry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val queryVat = VatNumber(query).takeIf { it.isValid }
    val unifiedItems = buildUnifiedItems(existingContacts, lookupState, queryVat, query)
    val hasResults = unifiedItems.isNotEmpty()
    val isLoading = isExistingLoading || lookupState is LookupUiState.Loading
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        when {
            isLoading && !hasResults -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            !hasResults && lookupState is LookupUiState.Error -> {
                item {
                    LookupErrorState(
                        exception = lookupState.exception,
                        onRetry = onRetryRegistry,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            !hasResults -> {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Constrains.Spacing.xLarge),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_lookup_no_matches),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                items(unifiedItems, key = { it.key }) { item ->
                    when (item) {
                        is UnifiedLookupItem.Local -> {
                            LookupUnifiedRow(
                                name = item.name,
                                secondary = item.secondary,
                                trustTag = item.trustTag,
                                onClick = { onSelectExisting(item.contactId) }
                            )
                        }
                        is UnifiedLookupItem.External -> {
                            LookupUnifiedRow(
                                name = item.name,
                                secondary = item.secondary,
                                trustTag = item.trustTag,
                                onClick = { onSelectRegistry(item.entity) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LookupHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
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
private fun LookupUnifiedRow(
    name: String,
    secondary: String?,
    trustTag: TrustTag,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
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
                    text = name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!secondary.isNullOrBlank()) {
                    Text(
                        text = secondary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            TrustTagPill(tag = trustTag)
        }
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

private sealed interface UnifiedLookupItem {
    val key: String
    val name: String
    val secondary: String?
    val trustTag: TrustTag
    val hasVatExactMatch: Boolean
    val isLocal: Boolean
    val isVerified: Boolean
    val nameScore: Int
    val originalIndex: Int

    data class Local(
        override val key: String,
        override val name: String,
        override val secondary: String?,
        override val trustTag: TrustTag,
        override val hasVatExactMatch: Boolean,
        override val isLocal: Boolean,
        override val isVerified: Boolean,
        override val nameScore: Int,
        override val originalIndex: Int,
        val contactId: ContactId,
    ) : UnifiedLookupItem

    data class External(
        override val key: String,
        override val name: String,
        override val secondary: String?,
        override val trustTag: TrustTag,
        override val hasVatExactMatch: Boolean,
        override val isLocal: Boolean,
        override val isVerified: Boolean,
        override val nameScore: Int,
        override val originalIndex: Int,
        val entity: EntityLookup,
    ) : UnifiedLookupItem
}

private enum class TrustTag {
    Local,
    Verified,
    External,
}

@Composable
private fun TrustTagPill(tag: TrustTag, modifier: Modifier = Modifier) {
    val label = when (tag) {
        TrustTag.Local -> stringResource(Res.string.contacts_trust_local)
        TrustTag.Verified -> stringResource(Res.string.contacts_trust_verified)
        TrustTag.External -> stringResource(Res.string.contacts_trust_external)
    }
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun buildUnifiedItems(
    existingContacts: List<ContactDto>,
    lookupState: LookupUiState,
    queryVat: VatNumber?,
    query: String
): List<UnifiedLookupItem> {
    val normalizedQueryVat = queryVat?.normalized
    val queryCompanyNumber = queryVat?.companyNumber
    val queryCountryCode = queryVat?.countryCode
    val queryText = if (queryVat == null) query.trim().ifBlank { null } else null
    val externalResults = (lookupState as? LookupUiState.Success)?.results.orEmpty()
    val localItems = existingContacts.mapIndexed { index, contact ->
        val contactVat = contact.vatNumber
        val hasVatExactMatch = hasExactVatMatch(
            normalizedQueryVat = normalizedQueryVat,
            queryCompanyNumber = queryCompanyNumber,
            queryCountryCode = queryCountryCode,
            candidateVat = contactVat
        )
        val nameScore = nameScore(queryText, contact.name.value)
        UnifiedLookupItem.Local(
            key = "local-${contact.id}",
            name = contact.name.value,
            secondary = listOfNotNull(contact.vatNumber?.value, contact.email?.value).joinToString(" • ")
                .takeIf { it.isNotBlank() },
            trustTag = TrustTag.Local,
            hasVatExactMatch = hasVatExactMatch,
            isLocal = true,
            isVerified = false,
            nameScore = nameScore,
            originalIndex = index,
            contactId = contact.id
        )
    }
    val externalItems = externalResults.mapIndexed { index, entity ->
        val entityVat = entity.vatNumber
        val hasVatExactMatch = hasExactVatMatch(
            normalizedQueryVat = normalizedQueryVat,
            queryCompanyNumber = queryCompanyNumber,
            queryCountryCode = queryCountryCode,
            candidateVat = entityVat
        )
        val isVerified = hasVatExactMatch
        val nameScore = nameScore(queryText, entity.name.value)
        val secondary = buildList {
            add(entity.vatNumber.value)
            entity.address?.let { address ->
                add(
                    "${address.city}, ${address.country.name}"
                )
            }
        }.joinToString(" • ").takeIf { it.isNotBlank() }
        UnifiedLookupItem.External(
            key = "external-${entity.enterpriseNumber}",
            name = entity.name.value,
            secondary = secondary,
            trustTag = if (isVerified) TrustTag.Verified else TrustTag.External,
            hasVatExactMatch = hasVatExactMatch,
            isLocal = false,
            isVerified = isVerified,
            nameScore = nameScore,
            originalIndex = index,
            entity = entity
        )
    }
    return (localItems + externalItems).sortedWith(
        compareByDescending<UnifiedLookupItem> { it.hasVatExactMatch }
            .thenByDescending { if (it.hasVatExactMatch) it.isLocal else false }
            .thenByDescending { it.isVerified }
            .thenByDescending { it.nameScore }
            .thenBy { it.originalIndex }
    )
}

private fun hasExactVatMatch(
    normalizedQueryVat: String?,
    queryCompanyNumber: String?,
    queryCountryCode: String?,
    candidateVat: VatNumber?
): Boolean {
    if (normalizedQueryVat == null || candidateVat == null) return false
    if (candidateVat.normalized == normalizedQueryVat) return true

    val candidateCompanyNumber = candidateVat.companyNumber
    if (queryCompanyNumber == null || candidateCompanyNumber == null) return false
    if (queryCompanyNumber != candidateCompanyNumber) return false

    val candidateCountryCode = candidateVat.countryCode
    return queryCountryCode == null ||
        candidateCountryCode == null ||
        queryCountryCode == candidateCountryCode
}

private fun nameScore(query: String?, name: String): Int {
    if (query.isNullOrBlank()) return 0
    val normalizedQuery = query.trim().lowercase()
    val normalizedName = name.trim().lowercase()
    return when {
        normalizedName.startsWith(normalizedQuery) -> 2
        normalizedName.contains(normalizedQuery) -> 1
        else -> 0
    }
}
