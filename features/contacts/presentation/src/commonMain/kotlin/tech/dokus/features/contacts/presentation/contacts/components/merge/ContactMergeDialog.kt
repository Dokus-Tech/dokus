package tech.dokus.features.contacts.presentation.contacts.components.merge

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.Name
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.mvi.ContactMergeIntent
import tech.dokus.features.contacts.mvi.ContactMergeState
import tech.dokus.features.contacts.presentation.contacts.model.MergeDialogStep
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun ContactMergeDialog(
    state: ContactMergeState,
    onIntent: (ContactMergeIntent) -> Unit,
) {
    val title = when (state.step) {
        MergeDialogStep.SelectTarget -> stringResource(Res.string.contacts_merge_select_target)
        MergeDialogStep.CompareFields -> stringResource(Res.string.contacts_merge_compare_fields)
        MergeDialogStep.Confirmation -> stringResource(Res.string.contacts_merge_confirm)
        MergeDialogStep.Result -> stringResource(Res.string.contacts_merge_success)
    }

    val primaryAction = when (state.step) {
        MergeDialogStep.SelectTarget -> null // No primary action, selection triggers next step
        MergeDialogStep.CompareFields -> DokusDialogAction(
            text = stringResource(Res.string.action_continue),
            onClick = { onIntent(ContactMergeIntent.Continue) },
            enabled = true
        )
        MergeDialogStep.Confirmation -> DokusDialogAction(
            text = stringResource(Res.string.contacts_merge_dialog_title),
            onClick = { onIntent(ContactMergeIntent.ConfirmMerge) },
            isLoading = state.isMerging,
            isDestructive = true,
            enabled = !state.isMerging
        )
        MergeDialogStep.Result -> DokusDialogAction(
            text = stringResource(Res.string.action_done),
            onClick = { onIntent(ContactMergeIntent.Complete) },
            enabled = true
        )
    }

    val secondaryAction = when {
        state.step == MergeDialogStep.Result -> null
        state.isMerging -> null
        state.step == MergeDialogStep.SelectTarget -> DokusDialogAction(
            text = stringResource(Res.string.action_cancel),
            onClick = { onIntent(ContactMergeIntent.Back) }
        )
        else -> DokusDialogAction(
            text = stringResource(Res.string.action_back),
            onClick = { onIntent(ContactMergeIntent.Back) }
        )
    }

    // For SelectTarget step, we need a fallback primary action (the dialog requires one)
    val effectivePrimaryAction = primaryAction ?: DokusDialogAction(
        text = "",
        onClick = {},
        enabled = false
    )

    DokusDialog(
        onDismissRequest = {
            if (!state.isMerging) onIntent(ContactMergeIntent.Dismiss)
        },
        title = title,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.MergeType,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        },
        content = {
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
        primaryAction = effectivePrimaryAction,
        secondaryAction = secondaryAction,
        dismissOnBackPress = !state.isMerging,
        dismissOnClickOutside = !state.isMerging
    )
}

// Preview skipped: Dialog animation causes AppNotIdleException in Roborazzi

