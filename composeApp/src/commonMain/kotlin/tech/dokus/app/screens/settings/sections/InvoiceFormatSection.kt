package tech.dokus.app.screens.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.app.screens.settings.components.generateInvoiceNumberPreview
import tech.dokus.app.viewmodel.WorkspaceSettingsIntent
import tech.dokus.app.viewmodel.WorkspaceSettingsState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.workspace_invoice_include_year
import tech.dokus.aura.resources.workspace_invoice_numbering
import tech.dokus.aura.resources.workspace_invoice_numbering_description
import tech.dokus.aura.resources.workspace_invoice_padding
import tech.dokus.aura.resources.workspace_invoice_prefix
import tech.dokus.aura.resources.workspace_invoice_prefix_description
import tech.dokus.aura.resources.workspace_invoice_preview
import tech.dokus.aura.resources.workspace_invoice_settings
import tech.dokus.aura.resources.workspace_invoice_settings_description
import tech.dokus.aura.resources.workspace_invoice_yearly_reset
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
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

// Invoice padding configuration options (number of digits)
private const val InvoicePaddingMin = 3
private const val InvoicePaddingDefault = 4
private const val InvoicePaddingMedium = 5
private const val InvoicePaddingMax = 6
private val InvoicePaddingOptions = listOf(
    InvoicePaddingMin,
    InvoicePaddingDefault,
    InvoicePaddingMedium,
    InvoicePaddingMax
)

@Composable
internal fun InvoiceFormatSection(
    formState: WorkspaceSettingsState.Content.FormState,
    expanded: Boolean,
    onToggle: () -> Unit,
    editMode: Boolean,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onIntent: (WorkspaceSettingsIntent) -> Unit,
) {
    val previewNumber = generateInvoiceNumberPreview(
        formState.invoicePrefix,
        formState.invoiceIncludeYear,
        formState.invoicePadding
    )
    val subtitle = if (!expanded) previewNumber else null

    SettingsSection(
        title = stringResource(Res.string.workspace_invoice_settings),
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
                text = stringResource(Res.string.workspace_invoice_settings_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constraints.Spacing.medium))

            // Invoice Prefix
            Text(
                text = stringResource(Res.string.workspace_invoice_prefix_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )
            Spacer(Modifier.height(Constraints.Spacing.xSmall))
            PTextFieldStandard(
                fieldName = stringResource(Res.string.workspace_invoice_prefix),
                value = formState.invoicePrefix,
                onValueChange = { onIntent(WorkspaceSettingsIntent.UpdateInvoicePrefix(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(Constraints.Spacing.medium))

            // Invoice Numbering Subsection
            Text(
                text = stringResource(Res.string.workspace_invoice_numbering),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = stringResource(Res.string.workspace_invoice_numbering_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted
            )

            Spacer(Modifier.height(Constraints.Spacing.small))

            // Include Year in Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.invoiceIncludeYear,
                    onCheckedChange = { onIntent(WorkspaceSettingsIntent.UpdateInvoiceIncludeYear(it)) }
                )
                Text(
                    text = stringResource(Res.string.workspace_invoice_include_year),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            // Reset Numbering Each Year
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = formState.invoiceYearlyReset,
                    onCheckedChange = { onIntent(WorkspaceSettingsIntent.UpdateInvoiceYearlyReset(it)) }
                )
                Text(
                    text = stringResource(Res.string.workspace_invoice_yearly_reset),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(Constraints.Spacing.small))

            // Number Padding Selector
            Text(
                text = stringResource(Res.string.workspace_invoice_padding),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textMuted
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
            ) {
                InvoicePaddingOptions.forEach { padding ->
                    TextButton(
                        onClick = { onIntent(WorkspaceSettingsIntent.UpdateInvoicePadding(padding)) }
                    ) {
                        Text(
                            text = padding.toString(),
                            color = if (formState.invoicePadding == padding) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.textMuted
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(Constraints.Spacing.small))

            // Preview in highlighted card
            InvoicePreviewCard(previewNumber)
        } else {
            DataRow(
                label = stringResource(Res.string.workspace_invoice_prefix),
                value = formState.invoicePrefix,
            )

            DataRow(
                label = stringResource(Res.string.workspace_invoice_preview),
                value = previewNumber,
            )

            DataRow(
                label = stringResource(Res.string.workspace_invoice_include_year),
                value = if (formState.invoiceIncludeYear) "Yes" else "No",
            )

            DataRow(
                label = stringResource(Res.string.workspace_invoice_yearly_reset),
                value = if (formState.invoiceYearlyReset) "Yes" else "No",
            )
        }
    }
}

@Preview
@Composable
private fun InvoiceFormatSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        InvoiceFormatSection(
            formState = WorkspaceSettingsState.Content.FormState(
                invoicePrefix = "INV",
                invoiceIncludeYear = true,
                invoicePadding = 4,
                invoiceYearlyReset = true,
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

@Composable
private fun InvoicePreviewCard(previewNumber: String) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier.padding(Constraints.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.workspace_invoice_preview),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.textMuted
            )
            Spacer(Modifier.width(Constraints.Spacing.small))
            Text(
                text = previewNumber,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
