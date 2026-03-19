@file:Suppress("UnusedParameter") // reserved params

package tech.dokus.features.cashflow.presentation.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_saving_contact
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.review.components.details.ContactBlock
import tech.dokus.features.cashflow.presentation.review.components.details.MicroLabel
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// UI dimension constants
private val ErrorSurfaceCornerRadius = 8.dp

/**
 * Contact selection section for the Document Review screen.
 * Shows contact as truth via [ResolvedContact], click to edit.
 *
 * @param resolvedContact The effective contact state
 * @param isBindingContact Whether binding operation is in progress
 * @param isReadOnly Whether the document is confirmed (read-only mode)
 * @param validationError Error message for contact binding failures
 * @param onSelectContact Callback to open contact picker/sheet
 */
@Composable
fun ContactSelectionSection(
    resolvedContact: ResolvedContact,
    isBindingContact: Boolean,
    isReadOnly: Boolean,
    validationError: DokusException?,
    onSelectContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.cashflow_contact_label))

        if (validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(ErrorSurfaceCornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constraints.Spacing.small),
            ) {
                Text(
                    text = validationError.localized,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Constraints.Spacing.small),
                )
            }
        }

        AnimatedContent(
            targetState = isBindingContact,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ContactContent",
        ) { binding ->
            if (binding) {
                ContactLoadingState()
            } else {
                ContactBlock(
                    displayState = resolvedContact,
                    onEditClick = onSelectContact,
                    isReadOnly = isReadOnly,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ContactSelectionSectionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ContactSelectionSection(
            resolvedContact = ResolvedContact.Unknown,
            isBindingContact = false,
            isReadOnly = false,
            validationError = null,
            onSelectContact = {},
        )
    }
}

@Composable
private fun ContactLoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constraints.Spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DokusLoader(size = DokusLoaderSize.Small)
            Spacer(modifier = Modifier.width(Constraints.Spacing.small))
            Text(
                text = stringResource(Res.string.cashflow_saving_contact),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
