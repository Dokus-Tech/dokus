package tech.dokus.app.screens.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_requests_age
import tech.dokus.aura.resources.console_requests_client
import tech.dokus.aura.resources.console_requests_created
import tech.dokus.aura.resources.console_requests_period
import tech.dokus.aura.resources.console_requests_period_label
import tech.dokus.aura.resources.console_requests_status
import tech.dokus.aura.resources.console_requests_subtitle
import tech.dokus.aura.resources.console_requests_summary
import tech.dokus.aura.resources.console_requests_title
import tech.dokus.aura.resources.console_requests_type
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ClientColumnSpec = DokusTableColumnSpec(weight = 1.7f)
private val TypeColumnSpec = DokusTableColumnSpec(weight = 1.4f)
private val PeriodColumnSpec = DokusTableColumnSpec(weight = 1f)
private val CreatedColumnSpec = DokusTableColumnSpec(weight = 0.9f)
private val StatusColumnSpec = DokusTableColumnSpec(weight = 1f)
private val AgeColumnSpec = DokusTableColumnSpec(weight = 0.7f)
private val ActionColumnSpec = DokusTableColumnSpec(weight = 0.9f)

private data class ConsoleRequestItem(
    val client: String,
    val type: String,
    val period: String,
    val created: String,
    val status: String,
    val age: String,
    val action: String,
)

@Composable
internal fun ConsoleRequestsScreen(
    modifier: Modifier = Modifier,
) {
    val items = previewRequestItems()
    val isLargeScreen = LocalScreenSize.current.isLarge

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Constraints.Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)) {
                Text(
                    text = stringResource(Res.string.console_requests_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.console_requests_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DokusCardSurface {
                Text(
                    text = stringResource(Res.string.console_requests_period_label),
                    modifier = Modifier.padding(
                        horizontal = Constraints.Spacing.medium,
                        vertical = Constraints.Spacing.small
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Text(
            text = stringResource(Res.string.console_requests_summary),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isLargeScreen) {
            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    DokusTableRow(contentPadding = PaddingValues(horizontal = Constraints.Spacing.medium)) {
                        DokusTableCell(column = ClientColumnSpec) {
                            HeaderText(stringResource(Res.string.console_requests_client))
                        }
                        DokusTableCell(column = TypeColumnSpec) {
                            HeaderText(stringResource(Res.string.console_requests_type))
                        }
                        DokusTableCell(column = PeriodColumnSpec) {
                            HeaderText(stringResource(Res.string.console_requests_period))
                        }
                        DokusTableCell(column = CreatedColumnSpec) {
                            HeaderText(stringResource(Res.string.console_requests_created))
                        }
                        DokusTableCell(column = StatusColumnSpec) {
                            HeaderText(stringResource(Res.string.console_requests_status))
                        }
                        DokusTableCell(column = AgeColumnSpec) {
                            HeaderText(stringResource(Res.string.console_requests_age))
                        }
                        DokusTableCell(column = ActionColumnSpec) {
                            HeaderText("")
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(items) { item ->
                            DokusTableRow(contentPadding = PaddingValues(horizontal = Constraints.Spacing.medium)) {
                                DokusTableCell(column = ClientColumnSpec) {
                                    Text(
                                        text = item.client,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                DokusTableCell(column = TypeColumnSpec) {
                                    BodyText(item.type)
                                }
                                DokusTableCell(column = PeriodColumnSpec) {
                                    BodyText(item.period)
                                }
                                DokusTableCell(column = CreatedColumnSpec) {
                                    BodyText(item.created)
                                }
                                DokusTableCell(column = StatusColumnSpec) {
                                    BodyText(item.status)
                                }
                                DokusTableCell(column = AgeColumnSpec) {
                                    BodyText(item.age)
                                }
                                DokusTableCell(column = ActionColumnSpec) {
                                    Text(
                                        text = item.action,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                items(items) { item ->
                    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(Constraints.Spacing.medium),
                            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                        ) {
                            Text(
                                text = item.client,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            BodyText("${item.type} · ${item.period} · ${item.created}")
                            BodyText("${item.status} · ${item.age}")
                            Text(
                                text = item.action,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun BodyText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

private fun previewRequestItems(): List<ConsoleRequestItem> = listOf(
    ConsoleRequestItem("PixelForge BV", "Bank statement", "Feb 2026", "Feb 10", "Pending", "18d", "Copy link"),
    ConsoleRequestItem("PixelForge BV", "Credit card", "Feb 2026", "Feb 14", "Viewed", "14d", "Copy link"),
    ConsoleRequestItem("TechConsult BVBA", "Bank statement", "Jan 2026", "Feb 3", "Pending", "25d", "Copy link"),
    ConsoleRequestItem("TechConsult BVBA", "Purchase invoices", "Feb 2026", "Feb 12", "Pending", "16d", "Copy link"),
    ConsoleRequestItem("TechConsult BVBA", "Receipts", "Q4 2025", "Jan 28", "Expired", "31d", "Resend"),
    ConsoleRequestItem("Atelier Gent", "Bank statement", "Feb 2026", "Feb 20", "Viewed", "8d", "Copy link"),
    ConsoleRequestItem("Flux Mobility", "Sales invoices", "Feb 2026", "Feb 22", "Pending", "6d", "Copy link"),
)

@Preview(name = "Console Requests Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun ConsoleRequestsDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleRequestsScreen()
    }
}

@Preview(name = "Console Requests Mobile", widthDp = 390, heightDp = 844)
@Composable
private fun ConsoleRequestsMobilePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleRequestsScreen()
    }
}

