package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.workspace_iban
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * Counterparty display section - shows extracted counterparty info as facts.
 * Fact validation pattern: display-by-default, click to edit via ContactBlock.
 *
 * @param state The document review state
 * @param onIntent Intent handler
 * @param onCorrectContact Callback to open contact picker/sheet
 */
@Composable
internal fun CounterpartyCard(
    state: DocumentReviewState,
    onIntent: (DocumentReviewIntent) -> Unit,
    onCorrectContact: () -> Unit,
    onCreateContact: () -> Unit,
    isAccountantReadOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val isReadOnly = isAccountantReadOnly || state.isDocumentConfirmed || state.isDocumentRejected
    val contact = state.effectiveContact

    Column(modifier = modifier.fillMaxWidth()) {
        // Subtle micro-label
        MicroLabel(text = stringResource(Res.string.cashflow_contact_label))

        // Use ContactBlock for unified display + edit behavior
        ContactBlock(
            displayState = contact,
            onEditClick = onCorrectContact,
            isReadOnly = isReadOnly
        )

        // Show extracted IBAN/address below only for Detected contacts
        if (contact is ResolvedContact.Detected) {
            val hasExtractedData = contact.iban != null || contact.address != null
            if (hasExtractedData) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
                ) {
                    contact.iban?.let { iban ->
                        FactField(
                            label = stringResource(Res.string.workspace_iban),
                            value = iban
                        )
                    }
                    contact.address?.let { address ->
                        FactField(
                            label = stringResource(Res.string.contacts_address),
                            value = address
                        )
                    }
                }
            }
        }
    }
}
