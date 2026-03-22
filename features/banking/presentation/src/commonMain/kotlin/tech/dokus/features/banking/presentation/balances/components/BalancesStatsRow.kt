package tech.dokus.features.banking.presentation.balances.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import tech.dokus.domain.Money
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_stats_accounts
import tech.dokus.aura.resources.banking_balances_stats_last_sync
import tech.dokus.aura.resources.banking_balances_stats_matched
import tech.dokus.aura.resources.banking_balances_stats_missing
import tech.dokus.aura.resources.banking_balances_time_days_ago
import tech.dokus.aura.resources.banking_balances_time_hours_ago
import tech.dokus.aura.resources.banking_balances_time_minutes_ago
import tech.dokus.domain.model.BankAccountSummaryDto
import tech.dokus.domain.model.BankTransactionSummaryDto
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private const val Separator = " \u00B7 "

@Composable
internal fun BalancesStatsRow(
    summary: BankAccountSummaryDto,
    transactionSummary: BankTransactionSummaryDto?,
    modifier: Modifier = Modifier,
) {
    val accountsText = stringResource(Res.string.banking_balances_stats_accounts, summary.accountCount)
    val missingText = stringResource(Res.string.banking_balances_stats_missing, summary.unmatchedCount)
    val matchedText = stringResource(Res.string.banking_balances_stats_matched, summary.matchedThisPeriod)

    val relativeTime = summary.lastSyncedAt?.let { formatRelativeTime(it) }
    val lastSyncText = relativeTime?.let { stringResource(Res.string.banking_balances_stats_last_sync, it) }

    val primaryColor = MaterialTheme.colorScheme.primary
    val mutedColor = MaterialTheme.colorScheme.textMuted

    val annotatedText = buildAnnotatedString {
        withStyle(SpanStyle(color = mutedColor)) {
            append(accountsText)
        }

        if (summary.unmatchedCount > 0) {
            withStyle(SpanStyle(color = mutedColor)) {
                append(Separator)
            }
            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
                append(missingText)
            }
        }

        withStyle(SpanStyle(color = mutedColor)) {
            append(Separator)
            append(matchedText)
        }

        if (lastSyncText != null) {
            withStyle(SpanStyle(color = mutedColor)) {
                append(Separator)
                append(lastSyncText)
            }
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

/**
 * Formats a [LocalDateTime] as a relative time string ("Xm ago", "Xh ago", "Xd ago").
 */
@Composable
internal fun formatRelativeTime(dateTime: LocalDateTime): String {
    val now = kotlin.time.Clock.System.now()
    val instant = dateTime.toInstant(TimeZone.currentSystemDefault())
    val durationMinutes = (now - instant).inWholeMinutes

    return when {
        durationMinutes < 60 -> stringResource(Res.string.banking_balances_time_minutes_ago, durationMinutes.toInt())
        durationMinutes < 1440 -> stringResource(Res.string.banking_balances_time_hours_ago, (durationMinutes / 60).toInt())
        else -> stringResource(Res.string.banking_balances_time_days_ago, (durationMinutes / 1440).toInt())
    }
}

@Preview(name = "Balances Stats Row")
@Composable
private fun BalancesStatsRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        BalancesStatsRow(
            summary = BankAccountSummaryDto(
                totalBalance = Money(1778042),
                accountCount = 2,
                unmatchedCount = 3,
                totalUnresolvedAmount = Money(842050),
                matchedThisPeriod = 12,
                lastSyncedAt = null,
            ),
            transactionSummary = null,
        )
    }
}
