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
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.DokusLoaderSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_saving_contact
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.presentation.review.components.details.ContactBlock
import tech.dokus.features.cashflow.presentation.review.components.details.MicroLabel
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized

// UI dimension constants
private val ErrorSurfaceCornerRadius = 8.dp

/**
 * Contact selection section for the Document Review screen.
 * Simplified fact-validation UI: shows contact as truth, click to edit.
 *
 * - When contact exists: fact display with hover-to-edit
 * - When no contact: subtle amber border prompt
 * - Loading: spinner when binding contact
 *
 * @param documentType The document type (used for label context)
 * @param selectionState Current selection state
 * @param selectedContactSnapshot Contact details when selected
 * @param isBindingContact Whether binding operation is in progress
 * @param isReadOnly Whether the document is confirmed (read-only mode)
 * @param validationError Error message for contact binding failures
 * @param onAcceptSuggestion Callback when user accepts suggested contact
 * @param onChooseDifferent Callback when user wants to choose a different contact
 * @param onSelectContact Callback to open contact picker/sheet
 * @param onClearContact Callback to clear selected contact
 * @param onCreateNewContact Callback to open contact creation sheet
 */
@Composable
fun ContactSelectionSection(
    documentType: DocumentType,
    selectionState: ContactSelectionState,
    selectedContactSnapshot: ContactSnapshot?,
    isBindingContact: Boolean,
    isReadOnly: Boolean,
    validationError: DokusException?,
    onAcceptSuggestion: () -> Unit,
    onChooseDifferent: () -> Unit,
    onSelectContact: () -> Unit,
    onClearContact: () -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle micro-label
        MicroLabel(text = stringResource(Res.string.cashflow_contact_label))

        // Validation error (if any)
        if (validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(ErrorSurfaceCornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constrains.Spacing.small),
            ) {
                Text(
                    text = validationError.localized,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Constrains.Spacing.small),
                )
            }
        }

        // Content based on state
        AnimatedContent(
            targetState = isBindingContact,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ContactContent",
        ) { binding ->
            if (binding) {
                // Loading state
                ContactLoadingState()
            } else {
                // Use the unified ContactBlock component
                ContactBlock(
                    contact = selectedContactSnapshot,
                    onEditClick = onSelectContact,
                    isReadOnly = isReadOnly
                )
            }
        }
    }
}

@Composable
private fun ContactLoadingState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(Constrains.Spacing.medium),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DokusLoader(size = DokusLoaderSize.Small)
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = stringResource(Res.string.cashflow_saving_contact),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
