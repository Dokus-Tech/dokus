package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_download_pdf
import tech.dokus.domain.model.ai.ChatContentBlock
import tech.dokus.domain.model.ai.InvoiceLine
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val CardShape = RoundedCornerShape(7.dp)
private val VatColumnWidth = 36.dp

/**
 * Invoice detail breakdown card for AI chat responses.
 * Shows document header (name, ref, date, download), line items, and total.
 */
@Composable
fun ChatInvoiceDetailCard(
    block: ChatContentBlock.InvoiceDetail,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                CardShape,
            )
            .border(Constraints.Stroke.thin, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(Constraints.Spacing.medium),
    ) {
        // Header: name + ref/date + download button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = block.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "#${block.ref} \u00b7 ${block.date}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
            PButton(
                text = stringResource(Res.string.chat_download_pdf),
                variant = PButtonVariant.OutlineMuted,
                onClick = onDownload,
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = Constraints.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant,
        )

        // Line items
        block.lines.forEach { line ->
            InvoiceLineRow(line)
        }

        // Total
        HorizontalDivider(
            modifier = Modifier.padding(top = Constraints.Spacing.xSmall),
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.5.dp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Constraints.Spacing.xSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = block.total,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InvoiceLineRow(line: InvoiceLine) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Constraints.Spacing.xxSmall),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        Text(
            text = line.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = line.price,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        line.vatRate?.let { vat ->
            Text(
                text = vat,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
                textAlign = TextAlign.End,
                modifier = Modifier.padding(start = Constraints.Spacing.xSmall),
            )
        }
    }
}

@Preview
@Composable
private fun ChatInvoiceDetailCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatInvoiceDetailCard(
            block = ChatContentBlock.InvoiceDetail(
                name = "SRL Accounting & Tax Solutions",
                ref = "20260050",
                date = "2026-01-02",
                lines = listOf(
                    InvoiceLine("Comptabilit\u00e9 & prestations \u2014 Q4 2025", "\u20ac600.00", "21%"),
                    InvoiceLine("Gestion salaire dirigeant", "\u20ac60.00", "21%"),
                ),
                total = "\u20ac798.60",
            ),
            onDownload = {},
        )
    }
}
