package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.peppol_status_card_last_update
import tech.dokus.aura.resources.peppol_status_card_not_sent
import tech.dokus.aura.resources.peppol_status_card_recipient_id
import tech.dokus.aura.resources.peppol_status_card_title
import tech.dokus.aura.resources.peppol_status_card_transmission
import tech.dokus.domain.model.DocDto
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun PeppolStatusCard(
    state: DocumentReviewState,
    modifier: Modifier = Modifier
) {
    val invoice = state.documentRecord?.draft?.content as? DocDto.Invoice.Confirmed ?: return
    val peppolInfo = invoice.peppol ?: return

    Column(modifier = modifier.fillMaxWidth()) {
        MicroLabel(text = stringResource(Res.string.peppol_status_card_title))

        FactField(
            label = stringResource(Res.string.peppol_status_card_transmission),
            value = peppolInfo.status.localized
        )

        FactField(
            label = stringResource(Res.string.peppol_status_card_recipient_id),
            value = peppolInfo.peppolId.toString()
        )

        FactField(
            label = stringResource(Res.string.peppol_status_card_last_update),
            value = peppolInfo.sentAt.toString()
        )
    }
}
