package ai.dokus.app.contacts.components

import ai.dokus.app.contacts.usecases.ListContactsUseCase
import ai.dokus.app.contacts.usecases.MergeContactsUseCase
import tech.dokus.domain.model.ContactActivitySummary
import tech.dokus.domain.model.ContactDto
import tech.dokus.domain.model.ContactMergeResult
import ai.dokus.foundation.platform.Logger
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import org.koin.compose.koinInject

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
    val fieldLabel: String,
    val sourceValue: String?,
    val targetValue: String?,
    val keepSource: Boolean = false  // true = use source value, false = use target value
)

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
    var mergeError by remember { mutableStateOf<String?>(null) }

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
                    mergeError = error.message ?: "Failed to merge contacts"
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
                    MergeDialogStep.SelectTarget -> "Select Target Contact"
                    MergeDialogStep.CompareFields -> "Review Merge"
                    MergeDialogStep.Confirmation -> "Confirm Merge"
                    MergeDialogStep.Result -> "Merge Complete"
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
                            text = "Continue",
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
                                text = "Merging...",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        TextButton(onClick = { performMerge() }) {
                            Text(
                                text = "Merge Contacts",
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
                            text = "Done",
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
                        text = if (currentStep == MergeDialogStep.SelectTarget) "Cancel" else "Back",
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
            text = "Merging from:",
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
            placeholder = { Text("Search contacts...") },
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
                        text = "Searching...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            searchQuery.length < 2 -> {
                Text(
                    text = "Type at least 2 characters to search",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            searchResults.isEmpty() -> {
                Text(
                    text = "No contacts found matching \"$searchQuery\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            else -> {
                Text(
                    text = "Select target contact:",
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
                    text = "Source (Archive)",
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
                    text = "Target (Keep)",
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
                        text = "No conflicts found. All data can be merged automatically.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            // Show conflicts
            Text(
                text = "Resolve ${conflicts.size} conflict${if (conflicts.size > 1) "s" else ""}:",
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
                text = "All invoices, bills, expenses, and notes from the source contact will be moved to the target contact.",
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
            text = conflict.fieldLabel,
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
                        text = conflict.sourceValue ?: "(empty)",
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
                        text = conflict.targetValue ?: "(empty)",
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
    mergeError: String?,
    isMerging: Boolean
) {
    Column {
        // Warning banner
        Surface(
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
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
                        text = "This action cannot be undone",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "\"${sourceContact.name.value}\" will be archived after the merge.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Items to be reassigned
        if (sourceActivity != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Items to be moved to \"${targetContact.name.value}\":",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Invoices",
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
                            text = "Bills",
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
                            text = "Expenses",
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
                Text(
                    text = mergeError,
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
            text = "Contacts merged successfully!",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        targetContact?.let {
            Text(
                text = "All items now belong to \"${it.name.value}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reassignment summary
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Summary",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                ReassignmentRow("Invoices reassigned", result.invoicesReassigned)
                ReassignmentRow("Bills reassigned", result.billsReassigned)
                ReassignmentRow("Expenses reassigned", result.expensesReassigned)
                ReassignmentRow("Notes reassigned", result.notesReassigned)

                if (result.sourceArchived) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "✓ Source contact archived",
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
                    text = "VAT: ${vat.value}",
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
        fieldLabel: String,
        sourceValue: String?,
        targetValue: String?
    ) {
        if (sourceValue != null && targetValue != null && sourceValue != targetValue) {
            conflicts.add(
                MergeFieldConflict(
                    fieldName = fieldName,
                    fieldLabel = fieldLabel,
                    sourceValue = sourceValue,
                    targetValue = targetValue,
                    keepSource = false  // Default to keeping target
                )
            )
        }
    }

    // Check each field for conflicts
    addConflictIfDifferent("email", "Email", source.email?.value, target.email?.value)
    addConflictIfDifferent("phone", "Phone", source.phone, target.phone)
    addConflictIfDifferent("vatNumber", "VAT Number", source.vatNumber?.value, target.vatNumber?.value)
    addConflictIfDifferent("companyNumber", "Company Number", source.companyNumber, target.companyNumber)
    addConflictIfDifferent("contactPerson", "Contact Person", source.contactPerson, target.contactPerson)
    addConflictIfDifferent("addressLine1", "Address Line 1", source.addressLine1, target.addressLine1)
    addConflictIfDifferent("addressLine2", "Address Line 2", source.addressLine2, target.addressLine2)
    addConflictIfDifferent("city", "City", source.city, target.city)
    addConflictIfDifferent("postalCode", "Postal Code", source.postalCode, target.postalCode)
    addConflictIfDifferent("country", "Country", source.country, target.country)
    addConflictIfDifferent("peppolId", "Peppol ID", source.peppolId, target.peppolId)
    addConflictIfDifferent("tags", "Tags", source.tags, target.tags)

    // Payment terms (Int comparison)
    if (source.defaultPaymentTerms != target.defaultPaymentTerms) {
        conflicts.add(
            MergeFieldConflict(
                fieldName = "defaultPaymentTerms",
                fieldLabel = "Payment Terms",
                sourceValue = "${source.defaultPaymentTerms} days",
                targetValue = "${target.defaultPaymentTerms} days",
                keepSource = false
            )
        )
    }

    // VAT rate (VatRate comparison)
    val sourceVatRate = source.defaultVatRate?.value?.toString()
    val targetVatRate = target.defaultVatRate?.value?.toString()
    if (sourceVatRate != null && targetVatRate != null && sourceVatRate != targetVatRate) {
        conflicts.add(
            MergeFieldConflict(
                fieldName = "defaultVatRate",
                fieldLabel = "Default VAT Rate",
                sourceValue = "$sourceVatRate%",
                targetValue = "$targetVatRate%",
                keepSource = false
            )
        )
    }

    return conflicts
}
