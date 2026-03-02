package tech.dokus.app.screens.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_activity_subtitle
import tech.dokus.aura.resources.console_activity_title
import tech.dokus.aura.resources.console_requests_period_label
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private data class ConsoleActivityItem(
    val title: String,
    val cta: String,
    val time: String,
    val accent: Color,
)

@Composable
internal fun ConsoleActivityScreen(
    modifier: Modifier = Modifier,
) {
    val items = previewActivityItems()

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
                    text = stringResource(Res.string.console_activity_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(Res.string.console_activity_subtitle),
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

        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(items) { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Constraints.Spacing.medium),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DokusCardSurface {
                                androidx.compose.foundation.layout.Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(item.accent.copy(alpha = 0.14f))
                                    }
                                    androidx.compose.foundation.Canvas(modifier = Modifier.size(8.dp)) {
                                        drawCircle(item.accent)
                                    }
                                }
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
                            ) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = item.cta,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }

                        Text(
                            text = item.time,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

private fun previewActivityItems(): List<ConsoleActivityItem> = listOf(
    ConsoleActivityItem(
        title = "BrainBridge AI uploaded 3 documents",
        cta = "View client →",
        time = "10 min ago",
        accent = Color(0xFF28D17C),
    ),
    ConsoleActivityItem(
        title = "Bakkerij Janssen — 2 new PEPPOL invoices received",
        cta = "View client →",
        time = "45 min ago",
        accent = Color(0xFFE8B61B),
    ),
    ConsoleActivityItem(
        title = "PixelForge BV — possible duplicate invoice detected",
        cta = "View client →",
        time = "1 hr ago",
        accent = Color(0xFFE8B61B),
    ),
    ConsoleActivityItem(
        title = "De Koffiebrander — PEPPOL delivery error on 2 invoices",
        cta = "View client →",
        time = "2 hrs ago",
        accent = Color(0xFFF04F67),
    ),
    ConsoleActivityItem(
        title = "Invoid BV fulfilled your bank statement request",
        cta = "View client →",
        time = "3 hrs ago",
        accent = Color(0xFF28D17C),
    ),
    ConsoleActivityItem(
        title = "TechConsult BVBA — no PEPPOL activity for 10 days",
        cta = "View client →",
        time = "Yesterday",
        accent = Color(0xFFE8B61B),
    ),
    ConsoleActivityItem(
        title = "Atelier Gent uploaded 1 document",
        cta = "View client →",
        time = "Yesterday",
        accent = Color(0xFF28D17C),
    ),
    ConsoleActivityItem(
        title = "Flux Mobility — 3 PEPPOL delivery errors detected",
        cta = "View client →",
        time = "2 days ago",
        accent = Color(0xFFF04F67),
    ),
)

@Preview(name = "Console Activity Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun ConsoleActivityDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleActivityScreen()
    }
}

@Preview(name = "Console Activity Mobile", widthDp = 390, heightDp = 844)
@Composable
private fun ConsoleActivityMobilePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleActivityScreen()
    }
}

