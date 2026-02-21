package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.cashflow_needs_attention
import tech.dokus.aura.resources.cashflow_needs_input
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.domain.Money
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Mobile header for document detail screen.
 * Shows back button, description, total amount, and attention indicator.
 *
 * Uses two-level attention:
 * - isBlocking (hard block) -> "needs input"
 * - hasAttention && !isBlocking (soft signal) -> "needs attention"
 */
@Composable
internal fun DocumentDetailMobileHeader(
    description: String,
    total: Money?,
    hasAttention: Boolean,
    isBlocking: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.small,
                    vertical = Constraints.Spacing.small
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = FeatherIcons.ArrowLeft,
                    contentDescription = stringResource(Res.string.action_back)
                )
            }

            // Title + understanding line
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                UnderstandingLine(
                    total = total,
                    hasAttention = hasAttention,
                    isBlocking = isBlocking
                )
            }
        }
    }
}

@Composable
private fun UnderstandingLine(
    total: Money?,
    hasAttention: Boolean,
    isBlocking: Boolean,
    modifier: Modifier = Modifier
) {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)
    val amountText = total?.let { "$currencySymbol${it.toDisplayString()}" } ?: "—"

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = amountText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (hasAttention) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.statusWarning, CircleShape)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                // Different label based on severity
                text = stringResource(
                    if (isBlocking) Res.string.cashflow_needs_input
                    else Res.string.cashflow_needs_attention
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.statusWarning
            )
        }
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DocumentDetailMobileHeaderPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentDetailMobileHeader(
            description = "Invoice INV-2024-001",
            total = Money.parseOrThrow("1250.00"),
            hasAttention = true,
            isBlocking = false,
            onBackClick = {}
        )
    }
}
