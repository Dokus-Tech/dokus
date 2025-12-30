package tech.dokus.contacts.components

import ai.dokus.app.contacts.usecases.ListContactsUseCase
import ai.dokus.app.contacts.usecases.MergeContactsUseCase
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.action_done
import tech.dokus.aura.resources.common_action_irreversible
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.contacts_address_line1
import tech.dokus.aura.resources.contacts_address_line2
import tech.dokus.aura.resources.contacts_bills
import tech.dokus.aura.resources.contacts_city
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_country
import tech.dokus.aura.resources.contacts_default_vat_rate
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_expenses
import tech.dokus.aura.resources.contacts_invoices
import tech.dokus.aura.resources.contacts_merge_all_items_belong
import tech.dokus.aura.resources.contacts_merge_bills_reassigned
import tech.dokus.aura.resources.contacts_merge_compare_fields
import tech.dokus.aura.resources.contacts_merge_confirm
import tech.dokus.aura.resources.contacts_merge_dialog_title
import tech.dokus.aura.resources.contacts_merge_expenses_reassigned
import tech.dokus.aura.resources.contacts_merge_from_label
import tech.dokus.aura.resources.contacts_merge_invoices_reassigned
import tech.dokus.aura.resources.contacts_merge_items_to_target
import tech.dokus.aura.resources.contacts_merge_move_items_info
import tech.dokus.aura.resources.contacts_merge_no_conflicts
import tech.dokus.aura.resources.contacts_merge_notes_reassigned
import tech.dokus.aura.resources.contacts_merge_resolve_conflict_plural
import tech.dokus.aura.resources.contacts_merge_resolve_conflict_single
import tech.dokus.aura.resources.contacts_merge_search_min_length
import tech.dokus.aura.resources.contacts_merge_search_no_results
import tech.dokus.aura.resources.contacts_merge_search_placeholder
import tech.dokus.aura.resources.contacts_merge_select_target
import tech.dokus.aura.resources.contacts_merge_select_target_prompt
import tech.dokus.aura.resources.contacts_merge_source_archive
import tech.dokus.aura.resources.contacts_merge_source_archived
import tech.dokus.aura.resources.contacts_merge_source_archived_check
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.aura.resources.contacts_merge_success
import tech.dokus.aura.resources.contacts_merge_success_message
import tech.dokus.aura.resources.contacts_merge_summary
import tech.dokus.aura.resources.contacts_merge_target_keep
import tech.dokus.aura.resources.contacts_merging
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_payment_terms_value
import tech.dokus.aura.resources.contacts_peppol_id
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_postal_code
import tech.dokus.aura.resources.contacts_searching
import tech.dokus.aura.resources.contacts_tags
import tech.dokus.aura.resources.contacts_vat_number
import ai.dokus.foundation.platform.Logger
import tech.dokus.foundation.aura.extensions.localized
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.exceptions.asDokusException
import org.koin.compose.koinInject
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactMergeResult

// ============================================================================
// MERGE DIALOG STATE
// ============================================================================

/**
 * Dialog step for the merge workflow.
 */
internal enum class MergeDialogStep {
    SelectTarget,    // Step 1: Select target contact
    CompareFields,   // Step 2: Compare and resolve conflicts
    Confirmation,    // Step 3: Confirm merge action
    Result           // Step 4: Show merge result
}

/**
 * Field conflict during merge.
 * When both source and target have different non-null values for a field.
 */
internal data class MergeFieldConflict(
    val fieldName: String,
    val fieldLabelRes: StringResource,
    val sourceValue: String?,
    val targetValue: String?,
    val keepSource: Boolean = false  // true = use source value, false = use target value
)

/**
 * Represents a merge error message that can come from resources or raw text.
 */
private sealed interface MergeError {
    data class Exception(val exception: DokusException) : MergeError
}

// ============================================================================
// MAIN DIALOG
// ============================================================================

/**
 * Dialog for merging duplicate contacts.
 *
 * Workflow:
 * 1. Select target contact to merge into (or pre-selected from duplicate detection)
 * 2. Compare fields side-by-side and resolve conflicts
 * 3. Confirm merge action with warning
 * 4. Show result with reassignment counts
 *
 * @param sourceContact The contact to merge from (will be archived)
 * @param sourceActivity Activity summary for the source contact (for displaying counts)
 * @param preselectedTarget Optional pre-selected target (from duplicate detection)
 * @param onMergeComplete Callback when merge completes successfully with result
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
internal fun ContactMergeDialog(
    sourceContact: ContactDto,
    sourceActivity: ContactActivitySummary?,
    preselectedTarget: ContactDto? = null,
    onMergeComplete: (ContactMergeResult) -> Unit,
    onDismiss: () -> Unit,
    listContacts: ListContactsUseCase = koinInject(),
    mergeContacts: MergeContactsUseCase = koinInject()
) {
    val scope = rememberCoroutineScope()
    val logger = remember { Logger.withTag("ContactMergeDialog") }

    // Dialog state
    var currentStep by remember { mutableStateOf(
        if (preselectedTarget != null) MergeDialogStep.CompareFields else MergeDialogStep.SelectTarget
    ) }
    var selectedTarget by remember { mutableStateOf(preselectedTarget) }
    var mergeResult by remember { mutableStateOf<ContactMergeResult?>(null) }
    var isMerging by remember { mutableStateOf(false) }
    var mergeError by remember { mutableStateOf<MergeError?>(null) }

    // Field conflicts (computed when target is selected)
    val fieldConflicts = remember { mutableStateListOf<MergeFieldConflict>() }

    // Compute conflicts when target changes
    LaunchedEffect(selectedTarget) {
        selectedTarget?.let { target ->
            fieldConflicts.clear()
            fieldConflicts.addAll(computeFieldConflicts(sourceContact, target))
        }
    }

    // Perform merge operation
    fun performMerge() {
        val target = selectedTarget ?: return

        isMerging = true
        mergeError = null

        scope.launch {
            logger.d { "Merging contact ${sourceContact.id} into ${target.id}" }

            mergeContacts(
                sourceContactId = sourceContact.id,
                targetContactId = target.id
            ).fold(
                onSuccess = { result ->
                    logger.i { "Merge successful: ${result.invoicesReassigned} invoices, ${result.billsReassigned} bills reassigned" }
                    mergeResult = result
                    currentStep = MergeDialogStep.Result
                    isMerging = false
                },
                onFailure = { error ->
                    logger.e(error) { "Merge failed" }
                    val exception = error.asDokusException
                    val displayException = if (exception is DokusException.Unknown) {
                        DokusException.ContactMergeFailed
                    } else {
                        exception
                    }
                    mergeError = MergeError.Exception(displayException)
                    isMerging = false
                }
            )
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isMerging) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.MergeType,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = when (currentStep) {
                    MergeDialogStep.SelectTarget -> stringResource(Res.string.contacts_merge_select_target)
                    MergeDialogStep.CompareFields -> stringResource(Res.string.contacts_merge_compare_fields)
                    MergeDialogStep.Confirmation -> stringResource(Res.string.contacts_merge_confirm)
                    MergeDialogStep.Result -> stringResource(Res.string.contacts_merge_success)
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            when (currentStep) {
                MergeDialogStep.SelectTarget -> {
                    SelectTargetStep(
                        sourceContact = sourceContact,
                        onTargetSelected = { target ->
                            selectedTarget = target
                            currentStep = MergeDialogStep.CompareFields
                        },
                        listContacts = listContacts
                    )
                }

                MergeDialogStep.CompareFields -> {
                    selectedTarget?.let { target ->
                        CompareFieldsStep(
                            sourceContact = sourceContact,
                            targetContact = target,
                            conflicts = fieldConflicts,
                            onConflictResolutionChange = { index, keepSource ->
                                fieldConflicts[index] = fieldConflicts[index].copy(keepSource = keepSource)
                            }
                        )
                    }
                }

                MergeDialogStep.Confirmation -> {
                    selectedTarget?.let { target ->
                        ConfirmationStep(
                            sourceContact = sourceContact,
                            targetContact = target,
                            sourceActivity = sourceActivity,
                            mergeError = mergeError,
                            isMerging = isMerging
                        )
                    }
                }

                MergeDialogStep.Result -> {
                    mergeResult?.let { result ->
                        ResultStep(
                            result = result,
                            targetContact = selectedTarget
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (currentStep) {
                MergeDialogStep.SelectTarget -> {
                    // No confirm button - user selects from list
                }

                MergeDialogStep.CompareFields -> {
                    TextButton(
                        onClick = { currentStep = MergeDialogStep.Confirmation }
                    ) {
                        Text(
                            text = stringResource(Res.string.action_continue),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                MergeDialogStep.Confirmation -> {
                    if (isMerging) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(Res.string.contacts_merging),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        TextButton(onClick = { performMerge() }) {
                            Text(
                                text = stringResource(Res.string.contacts_merge_dialog_title),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                MergeDialogStep.Result -> {
                    TextButton(
                        onClick = {
                            mergeResult?.let { onMergeComplete(it) }
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = stringResource(Res.string.action_done),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        },
        dismissButton = {
            if (!isMerging && currentStep != MergeDialogStep.Result) {
                TextButton(
                    onClick = {
                        when (currentStep) {
                            MergeDialogStep.SelectTarget -> onDismiss()
                            MergeDialogStep.CompareFields -> {
                                if (preselectedTarget != null) {
                                    onDismiss()
                                } else {
                                    currentStep = MergeDialogStep.SelectTarget
                                }
                            }
                            MergeDialogStep.Confirmation -> currentStep = MergeDialogStep.CompareFields
                            MergeDialogStep.Result -> {}
                        }
                    }
                ) {
                    Text(
                        text = if (currentStep == MergeDialogStep.SelectTarget) {
                            stringResource(Res.string.action_cancel)
                        } else {
                            stringResource(Res.string.action_back)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    )
}

// ============================================================================
// STEP 1: SELECT TARGET CONTACT
// ============================================================================

/**
 * Step for selecting the target contact to merge into.
 * Shows a search field and list of matching contacts.
 */
@Composable
private fun SelectTargetStep(
    sourceContact: ContactDto,
    onTargetSelected: (ContactDto) -> Unit,
    listContacts: ListContactsUseCase
) {
    val scope = rememberCoroutineScope()
    val logger = remember { Logger.withTag("ContactMergeDialog") }

    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Debounced search
    LaunchedEffect(searchQuery) {
        searchJob?.cancel()
        searchJob = scope.launch {
            if (searchQuery.length >= 2) {
                delay(300)
                isSearching = true
                listContacts(
                    search = searchQuery,
                    isActive = true,
                    limit = 20
                ).fold(
                    onSuccess = { contacts ->
                        // Filter out the source contact
                        searchResults = contacts.filter { it.id != sourceContact.id }
                        isSearching = false
                    },
                    onFailure = { error ->
                        logger.e(error) { "Search failed" }
                        searchResults = emptyList()
                        isSearching = false
                    }
                )
            } else {
                searchResults = emptyList()
            }
        }
    }

    Column(
        modifier = Modifier.heightIn(min = 200.dp, max = 400.dp)
    ) {
        // Source contact info
        Text(
            text = stringResource(Res.string.contacts_merge_from_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ContactMiniCard(
            contact = sourceContact,
            isSelected = false,
            onClick = null
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Search field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
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

        // Search results
        when {
            isSearching -> {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
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
                        ContactMiniCard(
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
// STEP 2: COMPARE FIELDS
// ============================================================================

/**
 * Step for comparing source and target fields.
 * Shows side-by-side comparison with conflict resolution.
 */
@Composable
private fun CompareFieldsStep(
    sourceContact: ContactDto,
    targetContact: ContactDto,
    conflicts: List<MergeFieldConflict>,
    onConflictResolutionChange: (Int, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .heightIn(min = 200.dp, max = 400.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header with contact names
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_source_archive),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = sourceContact.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_target_keep),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = targetContact.name.value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (conflicts.isEmpty()) {
            // No conflicts
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.contacts_merge_no_conflicts),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Show conflicts
            Text(
                text = if (conflicts.size == 1) {
                    stringResource(Res.string.contacts_merge_resolve_conflict_single, conflicts.size)
                } else {
                    stringResource(Res.string.contacts_merge_resolve_conflict_plural, conflicts.size)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            conflicts.forEachIndexed { index, conflict ->
                ConflictResolutionRow(
                    conflict = conflict,
                    onKeepSourceChange = { keepSource ->
                        onConflictResolutionChange(index, keepSource)
                    }
                )

                if (index < conflicts.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info about what happens
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(Res.string.contacts_merge_move_items_info),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

/**
 * Row for resolving a single field conflict.
 */
@Composable
private fun ConflictResolutionRow(
    conflict: MergeFieldConflict,
    onKeepSourceChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(conflict.fieldLabelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Source value option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onKeepSourceChange(true) }
                    .border(
                        width = if (conflict.keepSource) 2.dp else 1.dp,
                        color = if (conflict.keepSource) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                color = if (conflict.keepSource) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = conflict.keepSource,
                        onClick = { onKeepSourceChange(true) }
                    )
                    Text(
                        text = formatConflictValue(conflict.fieldName, conflict.sourceValue),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (conflict.sourceValue != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            // Target value option
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onKeepSourceChange(false) }
                    .border(
                        width = if (!conflict.keepSource) 2.dp else 1.dp,
                        color = if (!conflict.keepSource) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    ),
                shape = RoundedCornerShape(8.dp),
                color = if (!conflict.keepSource) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = !conflict.keepSource,
                        onClick = { onKeepSourceChange(false) }
                    )
                    Text(
                        text = formatConflictValue(conflict.fieldName, conflict.targetValue),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = if (conflict.targetValue != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun formatConflictValue(fieldName: String, value: String?): String {
    if (value == null) {
        return stringResource(Res.string.common_empty_value)
    }
    return when (fieldName) {
        "defaultPaymentTerms" -> {
            val days = value.toIntOrNull()
            if (days != null) {
                stringResource(Res.string.contacts_payment_terms_value, days)
            } else {
                value
            }
        }
        "defaultVatRate" -> stringResource(Res.string.common_percent_value, value)
        else -> value
    }
}

// ============================================================================
// STEP 3: CONFIRMATION
// ============================================================================

/**
 * Step for confirming the merge action.
 * Shows warning and counts of items to be reassigned.
 */
@Composable
private fun ConfirmationStep(
    sourceContact: ContactDto,
    targetContact: ContactDto,
    sourceActivity: ContactActivitySummary?,
    mergeError: MergeError?,
    isMerging: Boolean
) {
    Column {
        // Warning banner
        DokusCardSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = DokusCardVariant.Soft,
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = stringResource(Res.string.common_action_irreversible),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            Res.string.contacts_merge_source_archived,
                            sourceContact.name.value
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items to be reassigned
        if (sourceActivity != null) {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                variant = DokusCardVariant.Soft,
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(
                            Res.string.contacts_merge_items_to_target,
                            targetContact.name.value
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_invoices),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${sourceActivity.invoiceCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_bills),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${sourceActivity.billCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_expenses),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${sourceActivity.expenseCount}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Error message
        if (mergeError != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val errorText = when (mergeError) {
                    is MergeError.Exception -> mergeError.exception.localized
                }
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

// ============================================================================
// STEP 4: RESULT
// ============================================================================

/**
 * Step showing the merge result.
 * Displays counts of reassigned items.
 */
@Composable
private fun ResultStep(
    result: ContactMergeResult,
    targetContact: ContactDto?
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success icon
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(Res.string.contacts_merge_success_message),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        targetContact?.let {
            Text(
                text = stringResource(Res.string.contacts_merge_all_items_belong, it.name.value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reassignment summary
        DokusCardSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = DokusCardVariant.Soft,
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(Res.string.contacts_merge_summary),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                ReassignmentRow(
                    stringResource(Res.string.contacts_merge_invoices_reassigned),
                    result.invoicesReassigned
                )
                ReassignmentRow(
                    stringResource(Res.string.contacts_merge_bills_reassigned),
                    result.billsReassigned
                )
                ReassignmentRow(
                    stringResource(Res.string.contacts_merge_expenses_reassigned),
                    result.expensesReassigned
                )
                ReassignmentRow(
                    stringResource(Res.string.contacts_merge_notes_reassigned),
                    result.notesReassigned
                )

                if (result.sourceArchived) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.contacts_merge_source_archived_check),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ReassignmentRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// ============================================================================
// HELPER COMPONENTS
// ============================================================================

/**
 * Mini card for displaying a contact in selection lists.
 */
@Composable
private fun ContactMiniCard(
    contact: ContactDto,
    isSelected: Boolean,
    onClick: (() -> Unit)?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                },
                shape = RoundedCornerShape(8.dp)
            ),
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            contact.email?.let { email ->
                Text(
                    text = email.value,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            contact.vatNumber?.let { vat ->
                Text(
                    text = stringResource(Res.string.common_vat_value, vat.value),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================================
// HELPER FUNCTIONS
// ============================================================================

/**
 * Compute field conflicts between source and target contacts.
 * A conflict exists when both contacts have different non-null values for the same field.
 */
private fun computeFieldConflicts(
    source: ContactDto,
    target: ContactDto
): List<MergeFieldConflict> {
    val conflicts = mutableListOf<MergeFieldConflict>()

    // Helper to add conflict if both values are different and non-null
    fun addConflictIfDifferent(
        fieldName: String,
        fieldLabelRes: StringResource,
        sourceValue: String?,
        targetValue: String?
    ) {
        if (sourceValue != null && targetValue != null && sourceValue != targetValue) {
            conflicts.add(
                MergeFieldConflict(
                    fieldName = fieldName,
                    fieldLabelRes = fieldLabelRes,
                    sourceValue = sourceValue,
                    targetValue = targetValue,
                    keepSource = false  // Default to keeping target
                )
            )
        }
    }

    // Check each field for conflicts
    addConflictIfDifferent("email", Res.string.contacts_email, source.email?.value, target.email?.value)
    addConflictIfDifferent("phone", Res.string.contacts_phone, source.phone, target.phone)
    addConflictIfDifferent(
        "vatNumber",
        Res.string.contacts_vat_number,
        source.vatNumber?.value,
        target.vatNumber?.value
    )
    addConflictIfDifferent(
        "companyNumber",
        Res.string.contacts_company_number,
        source.companyNumber,
        target.companyNumber
    )
    addConflictIfDifferent(
        "contactPerson",
        Res.string.contacts_contact_person,
        source.contactPerson,
        target.contactPerson
    )
    addConflictIfDifferent(
        "addressLine1",
        Res.string.contacts_address_line1,
        source.addressLine1,
        target.addressLine1
    )
    addConflictIfDifferent(
        "addressLine2",
        Res.string.contacts_address_line2,
        source.addressLine2,
        target.addressLine2
    )
    addConflictIfDifferent("city", Res.string.contacts_city, source.city, target.city)
    addConflictIfDifferent("postalCode", Res.string.contacts_postal_code, source.postalCode, target.postalCode)
    addConflictIfDifferent("country", Res.string.contacts_country, source.country, target.country)
    addConflictIfDifferent("peppolId", Res.string.contacts_peppol_id, source.peppolId, target.peppolId)
    addConflictIfDifferent("tags", Res.string.contacts_tags, source.tags, target.tags)

    // Payment terms (Int comparison)
    if (source.defaultPaymentTerms != target.defaultPaymentTerms) {
        conflicts.add(
            MergeFieldConflict(
                fieldName = "defaultPaymentTerms",
                fieldLabelRes = Res.string.contacts_payment_terms,
                sourceValue = source.defaultPaymentTerms.toString(),
                targetValue = target.defaultPaymentTerms.toString(),
                keepSource = false
            )
        )
    }

    // VAT rate (VatRate comparison)
    val sourceVatRate = source.defaultVatRate?.toString()
    val targetVatRate = target.defaultVatRate?.toString()
    if (sourceVatRate != null && targetVatRate != null && sourceVatRate != targetVatRate) {
        conflicts.add(
            MergeFieldConflict(
                fieldName = "defaultVatRate",
                fieldLabelRes = Res.string.contacts_default_vat_rate,
                sourceValue = sourceVatRate,
                targetValue = targetVatRate,
                keepSource = false
            )
        )
    }

    return conflicts
}
