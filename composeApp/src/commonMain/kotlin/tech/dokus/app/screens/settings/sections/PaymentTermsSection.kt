package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_payment_days_description
import tech.dokus.aura.resources.workspace_payment_terms
import tech.dokus.aura.resources.workspace_payment_terms_description
import tech.dokus.aura.resources.workspace_payment_terms_section
import tech.dokus.aura.resources.workspace_payment_terms_text
import tech.dokus.foundation.aura.components.fields.PTextFieldFree
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.components.settings.DataRow
import tech.dokus.foundation.aura.components.settings.SettingsSection
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun PaymentTermsSection(
    formState: WorkspaceSettingsState.Content.FormState,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
) {
    val subtitle = if (!expanded) "${formState.defaultPaymentTerms} days" else null

    SettingsSection(
        title = stringResource(Res.string.workspace_payment_terms_section),
        subtitle = subtitle,
        expanded = expanded,
        onToggle = onToggle,
        editMode = editMode,
        onEdit = onEdit,
        onSave = onSave,
        onCancel = onCancel,
    ) {
        if (editMode) {
            Text(
                text = stringResource(Res.string.workspace_payment_terms_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constraints.Spacing.medium))

            // Payment Terms (Days)
            Text(
                text = stringResource(Res.string.workspace_payment_days_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )
            Spacer(Modifier.height(Constraints.Spacing.xSmall))
            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_payment_terms),
                value = formState.defaultPaymentTerms.toString(),
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateDefaultPaymentTerms(it)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constraints.Spacing.medium))

            // Payment Terms Text
            PTextFieldFree(
                fieldName = stringResource(Res.string.workspace_payment_terms_text),
                value = formState.paymentTermsText,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdatePaymentTermsText(it)) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            DataRow(
                label = stringResource(Res.string.workspace_payment_terms),
                value = "${formState.defaultPaymentTerms} days",
            )

            if (formState.paymentTermsText.isNotBlank()) {
                DataRow(
                    label = stringResource(Res.string.workspace_payment_terms_text),
                    value = formState.paymentTermsText,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PaymentTermsSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PaymentTermsSection(
            formState = WorkspaceSettingsState.Content.FormState(
                defaultPaymentTerms = 30,
                paymentTermsText = "Payment due within 30 days of invoice date.",
            ),
            expanded = true,
            onToggle = {},
            editMode = false,
            onEdit = {},
            onSave = {},
            onCancel = {},
            onIntent = {},
        )
    }
}
