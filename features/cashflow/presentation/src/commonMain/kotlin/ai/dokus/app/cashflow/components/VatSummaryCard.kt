package ai.dokus.app.cashflow.components

import tech.dokus.foundation.app.state.DokusState
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerLine
import tech.dokus.domain.Money
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 24.dp, vertical = 12.dp)
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
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VAT column
        VatAmountColumn(
            label = "VAT",
            sublabel = data.quarterInfo ?: "(by the end of quarter)",
            amount = data.vatAmount
        )

        // Net amount column
        AmountColumn(
            label = "Net amount",
            amount = data.netAmount
        )

        // Predicted Net amount column
        AmountColumn(
            label = "Predicted Net amount",
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
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // VAT column skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShimmerLine(modifier = Modifier.width(140.dp), height = 12.dp)
            ShimmerLine(modifier = Modifier.width(60.dp), height = 16.dp)
        }

        // Net amount column skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShimmerLine(modifier = Modifier.width(70.dp), height = 12.dp)
            ShimmerLine(modifier = Modifier.width(50.dp), height = 16.dp)
        }

        // Predicted Net amount column skeleton
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ShimmerLine(modifier = Modifier.width(120.dp), height = 12.dp)
            ShimmerLine(modifier = Modifier.width(50.dp), height = 16.dp)
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
private fun formatAmount(amount: Money): String {
    return try {
        val value = amount.toDouble()
        val formattedValue = when {
            value >= 1000 -> {
                val thousands = (value / 1000).toInt()
                "€${thousands}k"
            }

            value == 0.0 -> "€000"
            else -> "€${value.toInt()}"
        }
        formattedValue
    } catch (e: Exception) {
        "€000"
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
