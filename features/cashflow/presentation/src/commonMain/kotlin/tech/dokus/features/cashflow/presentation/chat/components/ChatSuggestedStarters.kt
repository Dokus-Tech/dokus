package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_ask_anything
import tech.dokus.aura.resources.chat_starter_biggest_expenses
import tech.dokus.aura.resources.chat_starter_overdue_invoices
import tech.dokus.aura.resources.chat_starter_q4_summary
import tech.dokus.aura.resources.chat_starter_unmatched_payments
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * Empty state with suggested starter questions.
 * Shown when no session is active and the chat is fresh.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChatSuggestedStarters(
    onStarterClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val starters = listOf(
        stringResource(Res.string.chat_starter_biggest_expenses),
        stringResource(Res.string.chat_starter_overdue_invoices),
        stringResource(Res.string.chat_starter_unmatched_payments),
        stringResource(Res.string.chat_starter_q4_summary),
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        ) {
            Text(
                text = stringResource(Res.string.chat_ask_anything),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.textMuted,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                modifier = Modifier.padding(horizontal = Constraints.Spacing.xxLarge),
            ) {
                starters.forEach { starter ->
                    FilterChip(
                        selected = false,
                        onClick = { onStarterClick(starter) },
                        label = {
                            Text(
                                text = starter,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        },
                    )
                }
            }
        }
    }
}
