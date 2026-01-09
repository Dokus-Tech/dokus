package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.date_format_short
import tech.dokus.aura.resources.date_month_short_apr
import tech.dokus.aura.resources.date_month_short_aug
import tech.dokus.aura.resources.date_month_short_dec
import tech.dokus.aura.resources.date_month_short_feb
import tech.dokus.aura.resources.date_month_short_jan
import tech.dokus.aura.resources.date_month_short_jul
import tech.dokus.aura.resources.date_month_short_jun
import tech.dokus.aura.resources.date_month_short_mar
import tech.dokus.aura.resources.date_month_short_may
import tech.dokus.aura.resources.date_month_short_nov
import tech.dokus.aura.resources.date_month_short_oct
import tech.dokus.aura.resources.date_month_short_sep
import tech.dokus.aura.resources.document_table_amount
import tech.dokus.aura.resources.document_table_date
import tech.dokus.aura.resources.document_type_bill
import tech.dokus.aura.resources.document_type_document
import tech.dokus.aura.resources.document_type_expense
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.document_type_unclassified
import tech.dokus.aura.resources.documents_table_document
import tech.dokus.aura.resources.documents_table_status
import tech.dokus.aura.resources.documents_view_details
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DraftStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.constrains.Constrains

private val TableRowHeight = 56.dp
private val TableHeaderBackgroundAlpha = 0.6f
private val ActionIconSize = 18.dp

@Immutable
private object DocumentTableColumns {
    val Document = DokusTableColumnSpec(weight = 2.4f)
    val Amount = DokusTableColumnSpec(weight = 0.9f, horizontalAlignment = Alignment.End)
    val Status = DokusTableColumnSpec(width = 120.dp, horizontalAlignment = Alignment.CenterHorizontally)
    val Date = DokusTableColumnSpec(weight = 0.9f)
    val Action = DokusTableColumnSpec(width = 36.dp, horizontalAlignment = Alignment.CenterHorizontally)
}

@Composable
internal fun DocumentTableHeaderRow(
    modifier: Modifier = Modifier
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = TableRowHeight,
        backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = TableHeaderBackgroundAlpha),
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        DokusTableCell(DocumentTableColumns.Document) {
            HeaderLabel(text = stringResource(Res.string.documents_table_document))
        }
        DokusTableCell(DocumentTableColumns.Amount) {
            HeaderLabel(
                text = stringResource(Res.string.document_table_amount),
                textAlign = TextAlign.End
            )
        }
        DokusTableCell(DocumentTableColumns.Status) {
            HeaderLabel(
                text = stringResource(Res.string.documents_table_status),
                textAlign = TextAlign.Center
            )
        }
        DokusTableCell(DocumentTableColumns.Date) {
            HeaderLabel(text = stringResource(Res.string.document_table_date))
        }
        DokusTableCell(DocumentTableColumns.Action) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
internal fun DocumentTableRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTitle = resolveDocumentPrimaryTitle(document)
    val documentType = extractDocumentType(document)
    val amount = extractAmount(document)
    val status = computeDisplayStatus(document)
    val dateLabel = formatDate(extractDocumentDate(document))

    DokusTableRow(
        modifier = modifier,
        minHeight = TableRowHeight,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = Constrains.Spacing.large)
    ) {
        DokusTableCell(DocumentTableColumns.Document) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = primaryTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = documentType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        DokusTableCell(DocumentTableColumns.Amount) {
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DokusTableCell(DocumentTableColumns.Status) {
            DocumentStatusChip(status = status)
        }
        DokusTableCell(DocumentTableColumns.Date) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        DokusTableCell(DocumentTableColumns.Action) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = stringResource(Res.string.documents_view_details),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ActionIconSize)
            )
        }
    }
}

@Composable
internal fun DocumentMobileRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryTitle = resolveDocumentPrimaryTitle(document)
    val documentType = extractDocumentType(document)
    val amount = extractAmount(document)
    val status = computeDisplayStatus(document)
    val dateLabel = formatDate(extractDocumentDate(document))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = primaryTitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = amount,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.End,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
        ) {
            Text(
                text = documentType,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            DocumentStatusChip(status = status)
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

enum class DocumentDisplayStatus {
    Processing,
    NeedsReview,
    Ready,
    Confirmed,
    Failed,
    Rejected
}

internal fun computeDisplayStatus(document: DocumentRecordDto): DocumentDisplayStatus {
    val ingestionStatus = document.latestIngestion?.status
    val draftStatus = document.draft?.draftStatus

    return when {
        draftStatus == DraftStatus.Rejected -> DocumentDisplayStatus.Rejected
        ingestionStatus == IngestionStatus.Failed -> DocumentDisplayStatus.Failed
        ingestionStatus == IngestionStatus.Processing ||
            ingestionStatus == IngestionStatus.Queued -> DocumentDisplayStatus.Processing
        draftStatus == DraftStatus.NeedsReview ||
            draftStatus == DraftStatus.NeedsInput -> DocumentDisplayStatus.NeedsReview
        draftStatus == DraftStatus.Ready -> DocumentDisplayStatus.Ready
        draftStatus == DraftStatus.Confirmed -> DocumentDisplayStatus.Confirmed
        else -> DocumentDisplayStatus.Processing
    }
}

@Composable
private fun HeaderLabel(
    text: String,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun resolveDocumentPrimaryTitle(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    return extractedData?.invoice?.clientName?.takeIf { it.isNotBlank() }
        ?: extractedData?.bill?.supplierName?.takeIf { it.isNotBlank() }
        ?: extractedData?.expense?.merchant?.takeIf { it.isNotBlank() }
        ?: document.document.filename?.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.common_unknown)
}

@Composable
private fun extractDocumentType(document: DocumentRecordDto): String {
    val type = document.draft?.documentType ?: document.draft?.extractedData?.documentType
    return when (type) {
        DocumentType.Invoice -> stringResource(Res.string.document_type_invoice)
        DocumentType.Bill -> stringResource(Res.string.document_type_bill)
        DocumentType.Expense -> stringResource(Res.string.document_type_expense)
        DocumentType.Unknown, null -> stringResource(Res.string.document_type_unclassified)
        else -> stringResource(Res.string.document_type_document)
    }
}

@Composable
private fun extractAmount(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    val amount = extractedData?.invoice?.totalAmount
        ?: extractedData?.bill?.amount
        ?: extractedData?.expense?.amount
    val currency = extractedData?.invoice?.currency
        ?: extractedData?.bill?.currency
        ?: extractedData?.expense?.currency

    return if (amount != null) {
        "${currency?.displaySign ?: "\u20AC"}${amount.toDisplayStringSafe()}"
    } else {
        "\u2014"
    }
}

private fun extractDocumentDate(document: DocumentRecordDto): LocalDate {
    val extractedData = document.draft?.extractedData
    return extractedData?.invoice?.issueDate
        ?: extractedData?.bill?.issueDate
        ?: extractedData?.expense?.date
        ?: document.document.uploadedAt.date
}

@Composable
private fun formatDate(date: LocalDate): String {
    val months = listOf(
        stringResource(Res.string.date_month_short_jan),
        stringResource(Res.string.date_month_short_feb),
        stringResource(Res.string.date_month_short_mar),
        stringResource(Res.string.date_month_short_apr),
        stringResource(Res.string.date_month_short_may),
        stringResource(Res.string.date_month_short_jun),
        stringResource(Res.string.date_month_short_jul),
        stringResource(Res.string.date_month_short_aug),
        stringResource(Res.string.date_month_short_sep),
        stringResource(Res.string.date_month_short_oct),
        stringResource(Res.string.date_month_short_nov),
        stringResource(Res.string.date_month_short_dec)
    )
    val monthName = months.getOrElse(date.month.ordinal) {
        stringResource(Res.string.common_unknown)
    }
    return stringResource(Res.string.date_format_short, monthName, date.day, date.year)
}

private fun Money.toDisplayStringSafe(): String {
    return runCatching { toDisplayString() }.getOrElse { "0.00" }
}
