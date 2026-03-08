package tech.dokus.app.screens.console

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.console_export_all_documents_csv
import tech.dokus.aura.resources.console_export_description
import tech.dokus.aura.resources.console_export_docs_count
import tech.dokus.aura.resources.console_export_included_body
import tech.dokus.aura.resources.console_export_panel_title
import tech.dokus.aura.resources.console_export_purchase_invoices
import tech.dokus.aura.resources.console_export_sales_invoices
import tech.dokus.aura.resources.console_export_whats_included
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private data class ExportClientItem(
    val id: String,
    val name: String,
    val vat: String,
    val docs: Int,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ConsoleExportScreen(
    firmName: String,
    period: String,
    modifier: Modifier = Modifier,
) {
    val clients = remember { previewExportClients() }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val selectedClient = clients.firstOrNull { it.id == selectedId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Constraints.Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        Text(
            text = stringResource(Res.string.console_export_description, firmName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(clients, key = { _, c -> c.id }) { index, client ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val isHovered by interactionSource.collectIsHoveredAsState()
                    val isSelected = client.id == selectedId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .hoverable(interactionSource)
                            .background(
                                when {
                                    isSelected -> MaterialTheme.colorScheme.surfaceHover
                                    isHovered -> MaterialTheme.colorScheme.surfaceHover.copy(alpha = 0.5f)
                                    else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                                }
                            )
                            .clickable { selectedId = if (isSelected) null else client.id }
                            .padding(
                                horizontal = Constraints.Spacing.large,
                                vertical = Constraints.Spacing.medium,
                            ),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Canvas(modifier = Modifier.size(5.dp)) {
                                if (isSelected) {
                                    drawCircle(color = Color(0xFFD4A017))
                                }
                            }
                            Column(
                                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xxSmall),
                            ) {
                                Text(
                                    text = client.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = client.vat,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text(
                            text = stringResource(Res.string.console_export_docs_count, client.docs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (index < clients.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        if (selectedClient != null) {
            val panelTitle = stringResource(
                Res.string.console_export_panel_title,
                selectedClient.name.uppercase(),
                period.uppercase(),
            )
            val filename = "${selectedClient.name.replace(" ", "_")}_${period.replace(" ", "_")}.csv"
            val includedBody = stringResource(
                Res.string.console_export_included_body,
                period,
                filename,
            )

            DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Constraints.Spacing.large),
                    verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    Text(
                        text = panelTitle,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.04.sp,
                    )

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                    ) {
                        PPrimaryButton(
                            text = stringResource(Res.string.console_export_all_documents_csv),
                            onClick = {},
                        )
                        POutlinedButton(
                            text = stringResource(Res.string.console_export_purchase_invoices),
                            onClick = {},
                        )
                        POutlinedButton(
                            text = stringResource(Res.string.console_export_sales_invoices),
                            onClick = {},
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                MaterialTheme.shapes.small,
                            )
                            .padding(Constraints.Spacing.medium),
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                        ) {
                            Text(
                                text = stringResource(Res.string.console_export_whats_included),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = includedBody,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun previewExportClients(): List<ExportClientItem> = listOf(
    ExportClientItem("1", "Invoid BV", "BE0792.140.667", 14),
    ExportClientItem("2", "PixelForge BV", "BE0456.789.123", 8),
    ExportClientItem("3", "De Koffiebrander", "BE0321.654.987", 23),
    ExportClientItem("4", "TechConsult BVBA", "BE0567.234.891", 5),
    ExportClientItem("5", "Atelier Gent", "BE0890.123.456", 3),
    ExportClientItem("6", "BrainBridge AI", "BE0234.567.890", 19),
    ExportClientItem("7", "Bakkerij Janssen", "BE0678.901.234", 31),
    ExportClientItem("8", "Flux Mobility", "BE0345.678.912", 7),
)

@Preview(name = "Console Export Desktop", widthDp = 1440, heightDp = 900)
@Composable
private fun ConsoleExportDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleExportScreen(
            firmName = "Kantoor Boonen",
            period = "Feb 2026",
        )
    }
}

@Preview(name = "Console Export Mobile", widthDp = 390, heightDp = 844)
@Composable
private fun ConsoleExportMobilePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        ConsoleExportScreen(
            firmName = "Kantoor Boonen",
            period = "Feb 2026",
        )
    }
}
