package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageCircle
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_empty_description_all
import tech.dokus.aura.resources.chat_empty_description_single
import tech.dokus.aura.resources.chat_example_due_date
import tech.dokus.aura.resources.chat_example_format
import tech.dokus.aura.resources.chat_example_invoices_company
import tech.dokus.aura.resources.chat_example_spend_last_month
import tech.dokus.aura.resources.chat_example_total_amount
import tech.dokus.aura.resources.chat_prompt_all_documents
import tech.dokus.aura.resources.chat_prompt_single_document
import tech.dokus.aura.resources.chat_this_document
import tech.dokus.aura.resources.chat_try_asking
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun EmptyStateContent(
    isSingleDocMode: Boolean,
    documentName: String?,
    modifier: Modifier = Modifier,
) {
    val documentLabel = documentName ?: stringResource(Res.string.chat_this_document)
    val promptText = if (isSingleDocMode) {
        stringResource(Res.string.chat_prompt_single_document, documentLabel)
    } else {
        stringResource(Res.string.chat_prompt_all_documents)
    }
    val descriptionText = if (isSingleDocMode) {
        stringResource(Res.string.chat_empty_description_single)
    } else {
        stringResource(Res.string.chat_empty_description_all)
    }

    Column(
        modifier = modifier.padding(Constraints.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = FeatherIcons.MessageCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.small))

        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.large))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.chat_try_asking),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExampleQuestionChip(
                text = if (isSingleDocMode) {
                    stringResource(Res.string.chat_example_total_amount)
                } else {
                    stringResource(Res.string.chat_example_spend_last_month)
                }
            )
            ExampleQuestionChip(
                text = if (isSingleDocMode) {
                    stringResource(Res.string.chat_example_due_date)
                } else {
                    stringResource(Res.string.chat_example_invoices_company)
                }
            )
        }
    }
}

@Composable
private fun ExampleQuestionChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Constraints.Spacing.medium, vertical = Constraints.Spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.chat_example_format, text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun EmptyStateContentPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        EmptyStateContent(
            isSingleDocMode = false,
            documentName = null
        )
    }
}
