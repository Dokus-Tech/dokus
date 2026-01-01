package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.action_done
import tech.dokus.aura.resources.contacts_merge_compare_fields
import tech.dokus.aura.resources.contacts_merge_confirm
import tech.dokus.aura.resources.contacts_merge_dialog_title
import tech.dokus.aura.resources.contacts_merge_select_target
import tech.dokus.aura.resources.contacts_merge_success
import tech.dokus.aura.resources.contacts_merging
import tech.dokus.features.contacts.presentation.contacts.model.MergeDialogStep
import tech.dokus.features.contacts.mvi.ContactMergeIntent
import tech.dokus.features.contacts.mvi.ContactMergeState

@Composable
internal fun ContactMergeDialog(
    state: ContactMergeState,
    onIntent: (ContactMergeIntent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isMerging) onIntent(ContactMergeIntent.Dismiss)
        },
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MergeType,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = when (state.step) {
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
            when (state.step) {
                MergeDialogStep.SelectTarget -> ContactMergeSelectTargetStep(
                    sourceContact = state.sourceContact,
                    searchQuery = state.searchQuery,
                    searchResults = state.searchResults,
                    isSearching = state.isSearching,
                    onSearchQueryChange = { onIntent(ContactMergeIntent.UpdateSearchQuery(it)) },
                    onTargetSelected = { onIntent(ContactMergeIntent.SelectTarget(it)) }
                )
                MergeDialogStep.CompareFields -> state.targetContact?.let { target ->
                    ContactMergeCompareFieldsStep(
                        sourceContact = state.sourceContact,
                        targetContact = target,
                        conflicts = state.conflicts,
                        onConflictResolutionChange = { index, keepSource ->
                            onIntent(ContactMergeIntent.UpdateConflict(index, keepSource))
                        }
                    )
                }
                MergeDialogStep.Confirmation -> state.targetContact?.let { target ->
                    ContactMergeConfirmationStep(
                        sourceContact = state.sourceContact,
                        targetContact = target,
                        sourceActivity = state.sourceActivity,
                        mergeError = state.mergeError,
                    )
                }
                MergeDialogStep.Result -> state.mergeResult?.let { result ->
                    ContactMergeResultStep(
                        result = result,
                        targetContact = state.targetContact
                    )
                }
            }
        },
        confirmButton = {
            when (state.step) {
                MergeDialogStep.SelectTarget -> Unit
                MergeDialogStep.CompareFields -> {
                    TextButton(onClick = { onIntent(ContactMergeIntent.Continue) }) {
                        Text(
                            text = stringResource(Res.string.action_continue),
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                MergeDialogStep.Confirmation -> {
                    if (state.isMerging) {
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
                        TextButton(onClick = { onIntent(ContactMergeIntent.ConfirmMerge) }) {
                            Text(
                                text = stringResource(Res.string.contacts_merge_dialog_title),
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                MergeDialogStep.Result -> {
                    TextButton(onClick = { onIntent(ContactMergeIntent.Complete) }) {
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
            if (!state.isMerging && state.step != MergeDialogStep.Result) {
                TextButton(onClick = { onIntent(ContactMergeIntent.Back) }) {
                    Text(
                        text = if (state.step == MergeDialogStep.SelectTarget) {
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
