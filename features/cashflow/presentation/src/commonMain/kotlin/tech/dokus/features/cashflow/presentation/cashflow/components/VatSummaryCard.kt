package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_amount_compact_thousands
import tech.dokus.aura.resources.cashflow_amount_with_currency
import tech.dokus.aura.resources.currency_symbol_eur
import tech.dokus.aura.resources.vat_net_amount
import tech.dokus.aura.resources.vat_predicted_net_amount
import tech.dokus.aura.resources.vat_quarter_sublabel
import tech.dokus.aura.resources.vat_summary_title
import tech.dokus.domain.Money
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerLine
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

// Layout constants
private val CardPadding = 16.dp
private val ColumnSpacing = 24.dp
private val SkeletonVerticalSpacing = 4.dp

// Skeleton dimensions
private val VatSkeletonLabelWidth = 140.dp
private val VatSkeletonAmountWidth = 60.dp
private val NetSkeletonLabelWidth = 70.dp
private val NetSkeletonAmountWidth = 50.dp
private val PredictedSkeletonLabelWidth = 120.dp
private val SkeletonLabelHeight = 12.dp
private val SkeletonAmountHeight = 16.dp

// Amount formatting threshold
private const val ThousandsThreshold = 1000

/**
 * A card component displaying VAT summary information with three columns:
 * - VAT (by the end of quarter)
 * - Net amount
 * - Predicted Net amount
 *
 * Handles loading, success, and error states independently.
 *
 * @param state The DokusState containing VAT summary data
 * @param modifier Optional modifier for the card
 */
@Composable
fun VatSummaryCard(
    state: DokusState<VatSummaryData>,
    modifier: Modifier = Modifier
) {
    DokusCardSurface(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding)
        ) {
            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    VatSummaryCardSkeleton()
                }

                is DokusState.Success -> {
                    VatSummaryCardContent(data = state.data)
                }

                is DokusState.Error -> {
                    VatSummaryCardError(state = state)
                }
            }
        }
    }
}

/**
 * Content displayed when data is loaded successfully.
 */
@Composable
private fun VatSummaryCardContent(
    data: VatSummaryData,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ColumnSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VAT column
        VatAmountColumn(
            label = stringResource(Res.string.vat_summary_title),
            sublabel = data.quarterInfo ?: stringResource(Res.string.vat_quarter_sublabel),
            amount = data.vatAmount
        )

        // Net amount column
        AmountColumn(
            label = stringResource(Res.string.vat_net_amount),
            amount = data.netAmount
        )

        // Predicted Net amount column
        AmountColumn(
            label = stringResource(Res.string.vat_predicted_net_amount),
            amount = data.predictedNetAmount
        )
    }
}

/**
 * Skeleton displayed during loading state.
 */
@Composable
private fun VatSummaryCardSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ColumnSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VAT column skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(SkeletonVerticalSpacing)
        ) {
            ShimmerLine(modifier = Modifier.width(VatSkeletonLabelWidth), height = SkeletonLabelHeight)
            ShimmerLine(modifier = Modifier.width(VatSkeletonAmountWidth), height = SkeletonAmountHeight)
        }

        // Net amount column skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(SkeletonVerticalSpacing)
        ) {
            ShimmerLine(modifier = Modifier.width(NetSkeletonLabelWidth), height = SkeletonLabelHeight)
            ShimmerLine(modifier = Modifier.width(NetSkeletonAmountWidth), height = SkeletonAmountHeight)
        }

        // Predicted Net amount column skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(SkeletonVerticalSpacing)
        ) {
            ShimmerLine(modifier = Modifier.width(PredictedSkeletonLabelWidth), height = SkeletonLabelHeight)
            ShimmerLine(modifier = Modifier.width(NetSkeletonAmountWidth), height = SkeletonAmountHeight)
        }
    }
}

/**
 * Error state with inline retry.
 */
@Composable
private fun VatSummaryCardError(
    state: DokusState.Error<*>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        DokusErrorContent(
            exception = state.exception,
            retryHandler = state.retryHandler,
            compact = true
        )
    }
}

/**
 * Column displaying VAT amount with a label and sublabel.
 * The sublabel appears in gray text.
 */
@Composable
private fun VatAmountColumn(
    label: String,
    sublabel: String,
    amount: Money,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Label with sublabel in gray
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                    append(label)
                }
                append(" ")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append(sublabel)
                }
            },
            style = MaterialTheme.typography.labelSmall
        )

        // Amount
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Column displaying a simple amount with a label.
 */
@Composable
private fun AmountColumn(
    label: String,
    amount: Money,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        // Amount
        Text(
            text = formatAmount(amount),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Formats a Money value for display.
 * Converts the string value to a formatted currency string.
 *
 * @param amount The Money value to format
 * @return Formatted string (e.g., "€1,234")
 */
@Composable
private fun formatAmount(amount: Money): String {
    val currencySymbol = stringResource(Res.string.currency_symbol_eur)
    val value = runCatching { amount.toDouble() }.getOrNull()
    return when {
        value == null -> stringResource(
            Res.string.cashflow_amount_with_currency,
            currencySymbol,
            "000"
        )
        value >= ThousandsThreshold -> {
            val thousands = (value / ThousandsThreshold).toInt()
            stringResource(Res.string.cashflow_amount_compact_thousands, currencySymbol, thousands)
        }
        value == 0.0 -> stringResource(
            Res.string.cashflow_amount_with_currency,
            currencySymbol,
            "000"
        )
        else -> stringResource(
            Res.string.cashflow_amount_with_currency,
            currencySymbol,
            value.toInt().toString()
        )
    }
}

/**
 * Data class for VAT summary information.
 *
 * @property vatAmount VAT amount due by the end of quarter
 * @property netAmount Current net amount
 * @property predictedNetAmount Predicted net amount
 * @property quarterInfo Optional quarter information text
 */
data class VatSummaryData(
    val vatAmount: Money,
    val netAmount: Money,
    val predictedNetAmount: Money,
    val quarterInfo: String? = null
) {
    companion object {
        val empty by lazy {
            VatSummaryData(
                vatAmount = Money.ZERO,
                netAmount = Money.ZERO,
                predictedNetAmount = Money.ZERO,
                quarterInfo = null
            )
        }
    }
}

/**
 * Extension function to create a VatSummaryData from individual values.
 */
fun createVatSummary(
    vat: String,
    net: String,
    predicted: String,
    quarter: String? = null
): VatSummaryData {
    return VatSummaryData(
        vatAmount = Money.parseOrThrow(vat),
        netAmount = Money.parseOrThrow(net),
        predictedNetAmount = Money.parseOrThrow(predicted),
        quarterInfo = quarter
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun VatSummaryCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        VatSummaryCard(
            state = DokusState.success(
                VatSummaryData(
                    vatAmount = Money.parseOrThrow("3250.00"),
                    netAmount = Money.parseOrThrow("12500.00"),
                    predictedNetAmount = Money.parseOrThrow("15000.00"),
                    quarterInfo = "Q2 2024"
                )
            )
        )
    }
}

@Preview
@Composable
private fun VatSummaryCardLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        VatSummaryCard(
            state = DokusState.loading()
        )
    }
}
