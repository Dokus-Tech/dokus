package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import tech.dokus.domain.enums.Currency
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * Document identity block showing vendor name, total amount, and date.
 *
 * These three fields are the "who, how much, when" — the minimum info
 * needed to recognize a document without reading it.
 */
@Composable
internal fun ReviewIdentityBlock(
    vendorName: String,
    totalAmount: String,
    dateDisplay: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        // Vendor name — large, bold
        Text(
            text = vendorName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Amount + date on the same line
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
        ) {
            Text(
                text = totalAmount,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 28.sp,
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = dateDisplay,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }
    }
}

/**
 * Extract vendor display name from state.
 */
internal fun resolveVendorName(state: DocumentDetailState): String {
    return when (val c = state.effectiveContact) {
        is ResolvedContact.Linked -> c.name
        is ResolvedContact.Suggested -> c.name
        is ResolvedContact.Detected -> c.name
        is ResolvedContact.Unknown -> state.documentRecord?.document?.filename ?: ""
    }
}

/**
 * Format total amount with currency sign for display.
 */
internal fun resolveDisplayAmount(state: DocumentDetailState): String {
    val amount = state.totalAmount ?: return "\u2014"
    val currency = resolveCurrency(state.draftData)
    return "${currency.displaySign}${amount.toDisplayString()}"
}

private fun resolveCurrency(draftData: DocDto?): Currency = when (draftData) {
    is DocDto.Invoice -> draftData.currency
    is DocDto.CreditNote -> draftData.currency
    is DocDto.Receipt -> draftData.currency
    is DocDto.BankStatement,
    is DocDto.ClassifiedDoc,
    null -> Currency.Eur
}
