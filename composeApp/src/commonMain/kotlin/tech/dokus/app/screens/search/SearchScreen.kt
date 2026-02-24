package tech.dokus.app.screens.search

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.search_contacts_count
import tech.dokus.aura.resources.search_documents_count
import tech.dokus.aura.resources.search_from_anywhere
import tech.dokus.aura.resources.search_input_placeholder
import tech.dokus.aura.resources.search_no_results
import tech.dokus.aura.resources.search_scope_all
import tech.dokus.aura.resources.search_scope_contacts
import tech.dokus.aura.resources.search_scope_documents
import tech.dokus.aura.resources.search_scope_transactions
import tech.dokus.aura.resources.search_section_contacts
import tech.dokus.aura.resources.search_section_documents
import tech.dokus.aura.resources.search_section_transactions
import tech.dokus.aura.resources.search_suggestions_label
import tech.dokus.aura.resources.search_total
import tech.dokus.aura.resources.search_transactions_count
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.ids.CashflowEntryId
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.SearchAggregates
import tech.dokus.domain.model.SearchContactHit
import tech.dokus.domain.model.SearchCounts
import tech.dokus.domain.model.SearchDocumentHit
import tech.dokus.domain.model.SearchSuggestion
import tech.dokus.domain.model.SearchTransactionHit
import tech.dokus.domain.model.UnifiedSearchResponse
import tech.dokus.domain.model.UnifiedSearchScope
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.components.text.MobilePageTitle
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val SearchHorizontalPaddingDesktop = 32.dp
private val SearchHorizontalPaddingMobile = 16.dp
private val SearchInputTopPaddingDesktop = 28.dp
private val SearchInputUnderlineThickness = 2.dp

@Composable
internal fun SearchScreen(
    state: SearchState,
    snackbarHostState: SnackbarHostState,
    onQueryChange: (String) -> Unit,
    onScopeSelected: (UnifiedSearchScope) -> Unit,
    onSuggestionClick: (String) -> Unit,
    onDocumentClick: (DocumentId) -> Unit,
    onContactClick: (ContactId) -> Unit,
    onTransactionClick: (CashflowEntryId) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLargeScreen = LocalScreenSize.current.isLarge
    val focusRequester = remember { FocusRequester() }
    val horizontalPadding = if (isLargeScreen) SearchHorizontalPaddingDesktop else SearchHorizontalPaddingMobile
    val response = state.response
    var textFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = state.query,
                selection = TextRange(state.query.length)
            )
        )
    }

    LaunchedEffect(state.query) {
        if (state.query != textFieldValue.text) {
            textFieldValue = TextFieldValue(
                text = state.query,
                selection = TextRange(state.query.length)
            )
        }
    }

    LaunchedEffect(state.focusRequestId) {
        if (state.focusRequestId > 0L) {
            textFieldValue = textFieldValue.copy(selection = TextRange(textFieldValue.text.length))
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = horizontalPadding)
        ) {
            if (!isLargeScreen) {
                Spacer(modifier = Modifier.height(16.dp))
                MobilePageTitle(title = stringResource(Res.string.action_search))
            } else {
                Spacer(modifier = Modifier.height(SearchInputTopPaddingDesktop))
            }

            SearchInputField(
                value = textFieldValue,
                focusRequester = focusRequester,
                onValueChange = { nextValue ->
                    textFieldValue = nextValue
                    if (nextValue.text != state.query) {
                        onQueryChange(nextValue.text)
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (state.query.isBlank()) {
                SuggestionsSection(
                    suggestions = state.suggestions,
                    isLoading = state.isLoading && !state.hasInitialized,
                    onSuggestionClick = onSuggestionClick,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                SearchScopeTabs(
                    selectedScope = state.scope,
                    counts = state.counts ?: SearchCounts(),
                    onScopeSelected = onScopeSelected
                )

                Spacer(modifier = Modifier.height(18.dp))

                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusLoader(size = DokusLoaderSize.Small)
                    }
                } else if (state.visibleResultCount == 0L || response == null) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = stringResource(Res.string.search_no_results),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.clickable(onClick = onRetry)
                        )
                    }
                } else {
                    SearchResultsSections(
                        state = state,
                        response = response,
                        onDocumentClick = onDocumentClick,
                        onContactClick = onContactClick,
                        onTransactionClick = onTransactionClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchInputField(
    value: TextFieldValue,
    focusRequester: FocusRequester,
    onValueChange: (TextFieldValue) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.displaySmall.copy(
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    if (value.text.isBlank()) {
                        Text(
                            text = stringResource(Res.string.search_input_placeholder),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (value.text.isNotBlank()) {
                        Text(
                            text = "esc",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.CenterEnd)
                        )
                    }
                    innerTextField()
                }
            }
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.primary,
            thickness = SearchInputUnderlineThickness
        )
    }
}

@Composable
private fun SearchScopeTabs(
    selectedScope: UnifiedSearchScope,
    counts: SearchCounts,
    onScopeSelected: (UnifiedSearchScope) -> Unit,
) {
    val tabs = listOf(
        SearchScopeTab(UnifiedSearchScope.All, Res.string.search_scope_all, counts.all),
        SearchScopeTab(UnifiedSearchScope.Documents, Res.string.search_scope_documents, counts.documents),
        SearchScopeTab(UnifiedSearchScope.Contacts, Res.string.search_scope_contacts, counts.contacts),
        SearchScopeTab(UnifiedSearchScope.Transactions, Res.string.search_scope_transactions, counts.transactions),
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(tabs, key = { it.scope.name }) { tab ->
            val isSelected = tab.scope == selectedScope
            val color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            Column(
                modifier = Modifier.clickable { onScopeSelected(tab.scope) }
            ) {
                Text(
                    text = "${stringResource(tab.labelRes).uppercase()} ${tab.count}",
                    style = MaterialTheme.typography.labelLarge,
                    color = color,
                    maxLines = 1,
                    softWrap = false,
                )
                Spacer(modifier = Modifier.height(7.dp))
                HorizontalDivider(
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    thickness = SearchInputUnderlineThickness
                )
            }
        }
    }
}

@Composable
private fun SuggestionsSection(
    suggestions: List<SearchSuggestion>,
    isLoading: Boolean,
    onSuggestionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(Res.string.search_suggestions_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DokusLoader(size = DokusLoaderSize.Small)
                }
            }

            suggestions.isEmpty() -> {
                Text(
                    text = stringResource(Res.string.search_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            else -> {
                suggestions.forEachIndexed { index, suggestion ->
                    SearchSimpleRow(
                        title = suggestion.label,
                        trailing = suggestion.countHint.takeIf { it > 0L }?.toString().orEmpty(),
                        onClick = { onSuggestionClick(suggestion.label) }
                    )
                    if (index < suggestions.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        SearchKeyboardHint()
    }
}

@Composable
private fun SearchResultsSections(
    state: SearchState,
    response: UnifiedSearchResponse,
    onDocumentClick: (DocumentId) -> Unit,
    onContactClick: (ContactId) -> Unit,
    onTransactionClick: (CashflowEntryId) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (state.scope == UnifiedSearchScope.All || state.scope == UnifiedSearchScope.Documents) {
            if (response.documents.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        titleRes = Res.string.search_section_documents,
                        count = response.counts.documents,
                        countLabelRes = Res.string.search_documents_count,
                    )
                }
                items(response.documents, key = { it.documentId.toString() }) { hit ->
                    SearchResultRow(
                        title = hit.filename,
                        subtitle = listOfNotNull(hit.counterpartyName, hit.documentType?.name).joinToString(" · "),
                        trailing = hit.status?.name ?: "",
                        onClick = { onDocumentClick(hit.documentId) }
                    )
                }
            }
        }

        if (state.scope == UnifiedSearchScope.All || state.scope == UnifiedSearchScope.Contacts) {
            if (response.contacts.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        titleRes = Res.string.search_section_contacts,
                        count = response.counts.contacts,
                        countLabelRes = Res.string.search_contacts_count,
                    )
                }
                items(response.contacts, key = { it.contactId.toString() }) { hit ->
                    SearchResultRow(
                        title = hit.name,
                        subtitle = hit.email ?: hit.vatNumber ?: hit.companyNumber.orEmpty(),
                        trailing = if (hit.isActive) "Active" else "Archived",
                        onClick = { onContactClick(hit.contactId) }
                    )
                }
            }
        }

        if (state.scope == UnifiedSearchScope.All || state.scope == UnifiedSearchScope.Transactions) {
            if (response.transactions.isNotEmpty()) {
                item {
                    SearchSectionHeader(
                        titleRes = Res.string.search_section_transactions,
                        count = response.counts.transactions,
                        countLabelRes = Res.string.search_transactions_count,
                        trailing = "${stringResource(Res.string.search_total)} €${state.aggregates.transactionTotal.toDisplayString()}",
                    )
                }
                items(response.transactions, key = { it.entryId.toString() }) { hit ->
                    SearchResultRow(
                        title = hit.displayText,
                        subtitle = listOf(hit.status.name, hit.date.toString()).joinToString(" · "),
                        trailing = "€${hit.amount.toDisplayString()}",
                        trailingColor = when {
                            hit.amount.minor < 0L -> MaterialTheme.colorScheme.error
                            hit.amount.minor > 0L -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = { onTransactionClick(hit.entryId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(
    titleRes: StringResource,
    count: Long,
    countLabelRes: StringResource,
    trailing: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${stringResource(titleRes).uppercase()} ${count.toInt()}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.weight(1f))
        trailing?.takeIf { it.isNotBlank() }?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (trailing.isNullOrBlank()) {
            Text(
                text = stringResource(countLabelRes, count.toInt()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchResultRow(
    title: String,
    subtitle: String,
    trailing: String,
    trailingColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (trailing.isNotBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.displaySmall,
                    color = trailingColor,
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    }
}

@Composable
private fun SearchSimpleRow(
    title: String,
    trailing: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (trailing.isNotBlank()) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SearchKeyboardHint() {
    Row(
        modifier = Modifier.padding(top = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "⌘K",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(end = 8.dp)
        )
        Text(
            text = stringResource(Res.string.search_from_anywhere),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class SearchScopeTab(
    val scope: UnifiedSearchScope,
    val labelRes: StringResource,
    val count: Long,
)

private fun previewResponse(query: String, scope: UnifiedSearchScope): UnifiedSearchResponse {
    val docs = listOf(
        SearchDocumentHit(
            documentId = DocumentId.parse("00000000-0000-0000-0000-000000000111"),
            filename = "KBC Bank - February.pdf",
            documentType = DocumentType.Receipt,
            status = DocumentStatus.Confirmed,
            counterpartyName = "KBC Bank NV",
            counterpartyVat = "BE0462920226",
        ),
        SearchDocumentHit(
            documentId = DocumentId.parse("00000000-0000-0000-0000-000000000112"),
            filename = "Tesla Belgium - January.pdf",
            documentType = DocumentType.Receipt,
            status = DocumentStatus.Confirmed,
            counterpartyName = "Tesla Belgium",
            counterpartyVat = null,
        )
    )
    val contacts = listOf(
        SearchContactHit(
            contactId = ContactId.parse("00000000-0000-0000-0000-000000000211"),
            name = "KBC Bank NV",
            email = "help@kbc.be",
            vatNumber = "BE0462920226",
            companyNumber = "0462920226",
            isActive = true,
        )
    )
    val transactions = listOf(
        SearchTransactionHit(
            entryId = CashflowEntryId.parse("00000000-0000-0000-0000-000000000311"),
            displayText = "KBC Bank NV",
            status = CashflowEntryStatus.Paid,
            date = LocalDate(2026, 2, 15),
            amount = Money.fromDouble(-289.00),
            direction = CashflowDirection.Out,
            contactName = "KBC Bank NV",
            documentFilename = "KBC Bank - February.pdf",
        ),
        SearchTransactionHit(
            entryId = CashflowEntryId.parse("00000000-0000-0000-0000-000000000312"),
            displayText = "KBC Bank NV",
            status = CashflowEntryStatus.Open,
            date = LocalDate(2026, 2, 18),
            amount = Money.fromDouble(1585.52),
            direction = CashflowDirection.In,
            contactName = "KBC Bank NV",
            documentFilename = null,
        ),
    )

    val scopedDocs = if (scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Documents) docs else emptyList()
    val scopedContacts = if (scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Contacts) contacts else emptyList()
    val scopedTx = if (scope == UnifiedSearchScope.All || scope == UnifiedSearchScope.Transactions) transactions else emptyList()

    return UnifiedSearchResponse(
        query = query,
        scope = scope,
        counts = SearchCounts(
            all = docs.size.toLong() + contacts.size + transactions.size,
            documents = docs.size.toLong(),
            contacts = contacts.size.toLong(),
            transactions = transactions.size.toLong(),
        ),
        documents = scopedDocs,
        contacts = scopedContacts,
        transactions = scopedTx,
        suggestions = listOf(
            SearchSuggestion(label = "KBC Bank", countHint = 4),
            SearchSuggestion(label = "overdue invoices", countHint = 8),
            SearchSuggestion(label = "Tesla Belgium", countHint = 3),
            SearchSuggestion(label = "January", countHint = 5),
        ),
        aggregates = SearchAggregates(
            transactionTotal = Money.fromDouble(1296.52),
            incomingTotal = Money.fromDouble(1585.52),
            outgoingTotal = Money.fromDouble(289.00),
        )
    )
}

@Preview(name = "Search Mobile Suggestions", widthDp = 390, heightDp = 844)
@Composable
private fun SearchScreenMobileSuggestionsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "",
                suggestions = previewResponse("", UnifiedSearchScope.All).suggestions,
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Desktop All", widthDp = 1366, heightDp = 900)
@Composable
private fun SearchScreenDesktopAllPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "kbc",
                scope = UnifiedSearchScope.All,
                response = previewResponse("kbc", UnifiedSearchScope.All),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Desktop Documents", widthDp = 1366, heightDp = 900)
@Composable
private fun SearchScreenDesktopDocumentsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "january",
                scope = UnifiedSearchScope.Documents,
                response = previewResponse("january", UnifiedSearchScope.Documents),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Desktop Contacts", widthDp = 1366, heightDp = 900)
@Composable
private fun SearchScreenDesktopContactsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "tax",
                scope = UnifiedSearchScope.Contacts,
                response = previewResponse("tax", UnifiedSearchScope.Contacts),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Desktop Transactions", widthDp = 1366, heightDp = 900)
@Composable
private fun SearchScreenDesktopTransactionsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "kbc",
                scope = UnifiedSearchScope.Transactions,
                response = previewResponse("kbc", UnifiedSearchScope.Transactions),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Mobile Results", widthDp = 390, heightDp = 844)
@Composable
private fun SearchScreenMobileResultsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "kbc",
                scope = UnifiedSearchScope.All,
                response = previewResponse("kbc", UnifiedSearchScope.All),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Desktop Empty", widthDp = 1366, heightDp = 900)
@Composable
private fun SearchScreenDesktopNoResultsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "does-not-exist",
                scope = UnifiedSearchScope.All,
                response = UnifiedSearchResponse(
                    query = "does-not-exist",
                    scope = UnifiedSearchScope.All,
                    counts = SearchCounts(),
                ),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}

@Preview(name = "Search Mobile Empty", widthDp = 390, heightDp = 844)
@Composable
private fun SearchScreenMobileNoResultsPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SearchScreen(
            state = SearchState(
                query = "missing",
                scope = UnifiedSearchScope.All,
                response = UnifiedSearchResponse(
                    query = "missing",
                    scope = UnifiedSearchScope.All,
                    counts = SearchCounts(),
                ),
                hasInitialized = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onQueryChange = {},
            onScopeSelected = {},
            onSuggestionClick = {},
            onDocumentClick = {},
            onContactClick = {},
            onTransactionClick = {},
            onRetry = {},
        )
    }
}
