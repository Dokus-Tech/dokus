package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_banking
import tech.dokus.aura.resources.workspace_bic
import tech.dokus.aura.resources.workspace_iban
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
internal fun BankingDetailsSection(
    formState: WorkspaceSettingsState.Content.FormState,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
) {
    val subtitle = if (!expanded && formState.iban.isNotBlank()) {
        formState.iban.take(10) + "..."
    } else null

    SettingsSection(
        title = stringResource(Res.string.workspace_banking),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        editMode = editMode,
        onEdit = onEdit,
        onSave = onSave,
        onCancel = onCancel,
    ) {
        if (editMode) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_iban),
                value = formState.iban,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateIban(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constraints.Spacing.small))

            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_bic),
                value = formState.bic,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateBic(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            DataRow(
                label = stringResource(Res.string.workspace_iban),
                value = formState.iban,
            )

            DataRow(
                label = stringResource(Res.string.workspace_bic),
                value = formState.bic,
            )
        }
    }
}
