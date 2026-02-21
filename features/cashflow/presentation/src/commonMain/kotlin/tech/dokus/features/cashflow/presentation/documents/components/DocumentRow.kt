package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.document_table_amount
import tech.dokus.aura.resources.document_table_date
import tech.dokus.aura.resources.documents_table_counterparty
import tech.dokus.aura.resources.documents_table_description
import tech.dokus.domain.Money
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.badges.SourceBadge
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.surfaceHover
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.components.badges.DocumentSource as UiDocumentSource

private val TableRowHeight = 48.dp

/**
 * Column specifications for the documents table.
 * v2: Vendor (dot+name) | Reference | Amount | Date | Source
 */
private object DocumentTableColumns {
    val Vendor = DokusTableColumnSpec(weight = 1f)
    val Reference = DokusTableColumnSpec(width = 150.dp)
    val Amount = DokusTableColumnSpec(width = 90.dp, horizontalAlignment = Alignment.End)
    val Date = DokusTableColumnSpec(width = 70.dp)
    val Source = DokusTableColumnSpec(width = 64.dp)
}

@Composable
internal fun DocumentTableHeaderRow(
    modifier: Modifier = Modifier
) {
    DokusTableRow(
        modifier = modifier,
        minHeight = 40.dp,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large)
    ) {
        DokusTableCell(DocumentTableColumns.Vendor) {
            HeaderLabel(text = stringResource(Res.string.documents_table_counterparty))
        }
        DokusTableCell(DocumentTableColumns.Reference) {
            HeaderLabel(text = stringResource(Res.string.documents_table_description))
        }
        DokusTableCell(DocumentTableColumns.Amount) {
            HeaderLabel(text = stringResource(Res.string.document_table_amount))
        }
        DokusTableCell(DocumentTableColumns.Date) {
            HeaderLabel(text = stringResource(Res.string.document_table_date))
        }
        DokusTableCell(DocumentTableColumns.Source) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
private fun HeaderLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
internal fun DocumentTableRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vendorName = resolveCounterparty(document, stringResource(Res.string.common_unknown))
    val reference = extractReference(document)
    val amountDouble = extractAmountDouble(document)
    val dateLabel = formatShortDate(extractDocumentDate(document))
    val needsAttention = computeNeedsAttention(document)
    val isProcessing = computeIsProcessing(document)
    val source = document.document.source.toUiSource()

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DokusTableRow(
        modifier = modifier
            .hoverable(interactionSource)
            .alpha(if (isProcessing) 0.5f else 1f),
        minHeight = TableRowHeight,
        onClick = onClick,
        backgroundColor = if (isHovered) {
            MaterialTheme.colorScheme.surfaceHover
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large)
    ) {
        // Vendor: dot + name
        DokusTableCell(DocumentTableColumns.Vendor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(
                    type = if (needsAttention) StatusDotType.Warning else StatusDotType.Confirmed,
                    size = 5.dp,
                )
                Text(
                    text = vendorName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                        fontStyle = if (isProcessing) FontStyle.Italic else FontStyle.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // Reference (mono)
        DokusTableCell(DocumentTableColumns.Reference) {
            Text(
                text = reference,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                ),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Amount (negative = expense)
        DokusTableCell(DocumentTableColumns.Amount) {
            if (amountDouble != null) {
                Amt(value = -amountDouble, size = 12.sp)
            } else {
                Text(
                    text = "\u2014",
                    color = MaterialTheme.colorScheme.textFaint,
                )
            }
        }

        // Date
        DokusTableCell(DocumentTableColumns.Date) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        // Source badge
        DokusTableCell(DocumentTableColumns.Source) {
            SourceBadge(source = source)
        }
    }
}

@Composable
internal fun DocumentMobileRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vendorName = resolveCounterparty(document, stringResource(Res.string.common_unknown))
    val reference = extractReference(document)
    val amountDouble = extractAmountDouble(document)
    val dateLabel = formatShortDate(extractDocumentDate(document))
    val needsAttention = computeNeedsAttention(document)
    val isProcessing = computeIsProcessing(document)
    val source = document.document.source.toUiSource()

    DokusCardSurface(
        modifier = modifier,
        accent = needsAttention,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .alpha(if (isProcessing) 0.5f else 1f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusDot(
                type = if (needsAttention) StatusDotType.Warning else StatusDotType.Confirmed,
                size = 6.dp,
            )

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = vendorName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        fontStyle = if (isProcessing) FontStyle.Italic else FontStyle.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = reference,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 9.sp,
                            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        ),
                        color = MaterialTheme.colorScheme.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    SourceBadge(source = source, compact = true)
                }
            }

            // Amount + Date
            Column(horizontalAlignment = Alignment.End) {
                if (amountDouble != null) {
                    Amt(value = -amountDouble, size = 13.sp)
                }
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }

            // Chevron
            Text(
                text = "\u203A",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.textFaint,
            )
        }
    }
}

// =============================================================================
// Document State Helpers
// =============================================================================

/**
 * Determines if a document needs user attention.
 */
internal fun computeNeedsAttention(document: DocumentRecordDto): Boolean {
    val ingestionStatus = document.latestIngestion?.status
    val documentStatus = document.draft?.documentStatus
    val hasConfirmedEntity = document.confirmedEntity != null
    val hasPendingMatchReview = document.pendingMatchReview != null

    if (documentStatus == DocumentStatus.Rejected) {
        return false
    }

    val ingestionNeedsAttention = ingestionStatus == IngestionStatus.Failed ||
        ingestionStatus == IngestionStatus.Processing ||
        ingestionStatus == IngestionStatus.Queued
    val draftNeedsReview = documentStatus == DocumentStatus.NeedsReview
    val confirmedButNoEntity = documentStatus == DocumentStatus.Confirmed && !hasConfirmedEntity
    val succeededButNoDraft = document.draft == null && ingestionStatus == IngestionStatus.Succeeded
    val isNotConfirmed = documentStatus == null ||
        documentStatus != DocumentStatus.Confirmed ||
        !hasConfirmedEntity

    return hasPendingMatchReview ||
        confirmedButNoEntity ||
        (isNotConfirmed && (ingestionNeedsAttention || draftNeedsReview || succeededButNoDraft))
}

/**
 * Determines if a document is currently processing.
 */
internal fun computeIsProcessing(document: DocumentRecordDto): Boolean {
    val ingestionStatus = document.latestIngestion?.status
    return ingestionStatus == IngestionStatus.Processing ||
        ingestionStatus == IngestionStatus.Queued
}

// =============================================================================
// Data Extraction Helpers
// =============================================================================

/**
 * Resolves the primary description for a document.
 */
internal fun resolveDescription(document: DocumentRecordDto, unknownLabel: String): String {
    val extractedData = document.draft?.extractedData
    val ingestionStatus = document.latestIngestion?.status
    val documentNumber = when (extractedData) {
        is InvoiceDraftData -> extractedData.invoiceNumber.nonBlank()
        is ReceiptDraftData -> extractedData.receiptNumber.nonBlank()
        is CreditNoteDraftData -> extractedData.creditNoteNumber.nonBlank()
        else -> null
    }
    val counterparty = document.draft?.counterpartySnapshot?.name.nonBlank()
    val filename = document.document.filename.nonBlank()

    return when {
        counterparty != null && documentNumber != null -> "$counterparty — $documentNumber"
        counterparty == null && documentNumber != null && filename != null -> "$filename — $documentNumber"
        counterparty == null && documentNumber != null -> "Document — $documentNumber"
        counterparty != null -> counterparty
        ingestionStatus == IngestionStatus.Processing ||
            ingestionStatus == IngestionStatus.Queued -> "Processing document\u2026"
        else -> filename
            ?: unknownLabel
    }
}

/**
 * Resolves the counterparty name for display.
 */
internal fun resolveCounterparty(document: DocumentRecordDto, emptyLabel: String = "\u2014"): String {
    val snapshot = document.draft?.counterpartySnapshot?.name.nonBlank()
    if (snapshot != null) return snapshot

    val fromDraft = when (val data = document.draft?.extractedData) {
        is InvoiceDraftData -> data.seller.name.nonBlank() ?: data.buyer.name.nonBlank()
        is CreditNoteDraftData -> data.counterpartyName.nonBlank()
        is ReceiptDraftData -> data.merchantName.nonBlank()
        else -> null
    }
    return fromDraft ?: document.document.filename.nonBlank() ?: emptyLabel
}

/**
 * Extracts the document reference number (invoice/receipt/credit note number).
 */
private fun extractReference(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    val number = when (extractedData) {
        is InvoiceDraftData -> extractedData.invoiceNumber.nonBlank()
        is ReceiptDraftData -> extractedData.receiptNumber.nonBlank()
        is CreditNoteDraftData -> extractedData.creditNoteNumber.nonBlank()
        else -> null
    }
    return number ?: document.document.filename.nonBlank() ?: "\u2014"
}

/**
 * Extracts the total amount as a Double for the Amt component.
 */
private fun extractAmountDouble(document: DocumentRecordDto): Double? {
    val extractedData = document.draft?.extractedData
    val amount = when (extractedData) {
        is InvoiceDraftData -> extractedData.totalAmount
        is ReceiptDraftData -> extractedData.totalAmount
        is CreditNoteDraftData -> extractedData.totalAmount
        else -> null
    }
    return amount?.toDouble()
}

@Composable
private fun extractAmount(document: DocumentRecordDto): String {
    val extractedData = document.draft?.extractedData
    val amount = when (extractedData) {
        is InvoiceDraftData -> extractedData.totalAmount
        is ReceiptDraftData -> extractedData.totalAmount
        is CreditNoteDraftData -> extractedData.totalAmount
        else -> null
    }
    val currency = when (extractedData) {
        is InvoiceDraftData -> extractedData.currency
        is ReceiptDraftData -> extractedData.currency
        is CreditNoteDraftData -> extractedData.currency
        else -> null
    }

    return if (amount != null) {
        "${currency?.displaySign ?: "\u20AC"}${amount.toDisplayStringSafe()}"
    } else {
        "\u2014"
    }
}

private fun extractDocumentDate(document: DocumentRecordDto): LocalDate {
    val extractedData = document.draft?.extractedData
    return when (extractedData) {
        is InvoiceDraftData -> extractedData.issueDate
        is ReceiptDraftData -> extractedData.date
        is CreditNoteDraftData -> extractedData.issueDate
        else -> null
    }
        ?: document.document.uploadedAt.date
}

private fun tech.dokus.domain.enums.DocumentSource.toUiSource(): UiDocumentSource {
    return when (this) {
        tech.dokus.domain.enums.DocumentSource.Peppol -> UiDocumentSource.Peppol
        else -> UiDocumentSource.Pdf
    }
}

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

private fun Money.toDisplayStringSafe(): String {
    return runCatching { toDisplayString() }.getOrElse { "0.00" }
}

@Preview
@Composable
private fun DocumentTableHeaderRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentTableHeaderRow()
    }
}
