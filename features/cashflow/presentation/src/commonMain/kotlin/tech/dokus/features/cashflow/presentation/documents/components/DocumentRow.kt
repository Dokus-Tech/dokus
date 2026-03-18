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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.document_table_amount
import tech.dokus.aura.resources.document_table_date
import tech.dokus.aura.resources.documents_table_counterparty
import tech.dokus.aura.resources.documents_table_description
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.IngestionStatus
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.model.toUiStatus
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.badges.SourceBadge
import tech.dokus.foundation.aura.components.layout.DokusHeaderColumn
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableHeader
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.iconizedOrDefault
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.model.DocumentUiStatus
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.foundation.aura.components.badges.toUiSource
import tech.dokus.foundation.aura.components.badges.DocumentSource as UiDocumentSource

private val TableRowHeight = 48.dp

internal sealed interface DocumentListReferenceValue {
    data class Status(val value: DocumentUiStatus) : DocumentListReferenceValue
    data class Reference(val value: String) : DocumentListReferenceValue
}

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
    DokusTableHeader(
        columns = listOf(
            DokusHeaderColumn(label = stringResource(Res.string.documents_table_counterparty), weight = 1f),
            DokusHeaderColumn(label = stringResource(Res.string.documents_table_description), width = 150.dp),
            DokusHeaderColumn(label = stringResource(Res.string.document_table_amount), width = 90.dp, alignment = Alignment.End),
            DokusHeaderColumn(label = stringResource(Res.string.document_table_date), width = 70.dp),
            DokusHeaderColumn(label = "", width = 64.dp),
        ),
        modifier = modifier,
    )
}

@Composable
internal fun DocumentTableRow(
    document: DocumentListItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listStatus = resolveListInlineStatus(document)
    val listReference = resolveListReferenceValue(document, listStatus)
    val unknownVendor = stringResource(Res.string.common_unknown)
    val vendorName = resolveListVendorName(document, unknownVendor, listStatus)
    val reference = when (listReference) {
        is DocumentListReferenceValue.Reference -> listReference.value
        is DocumentListReferenceValue.Status -> listReference.value.localized
    }
    val amountDouble = document.totalAmount?.toDouble()
    val dateLabel = formatShortDate(document.sortDate)
    val needsAttention = when (listStatus) {
        DocumentUiStatus.Queued,
        DocumentUiStatus.Processing,
        DocumentUiStatus.Failed -> true

        null -> computeListItemNeedsAttention(document)
        else -> false
    }
    val isProcessing = listStatus == DocumentUiStatus.Queued ||
        listStatus == DocumentUiStatus.Processing
    val source = document.effectiveOrigin.toUiSource()

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
                Icon(
                    imageVector = document.documentType.iconizedOrDefault(),
                    contentDescription = null,
                    modifier = Modifier.size(Constraints.IconSize.xSmall),
                    tint = if (needsAttention) {
                        MaterialTheme.colorScheme.statusWarning
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
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
    document: DocumentListItemDto,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listStatus = resolveListInlineStatus(document)
    val listReference = resolveListReferenceValue(document, listStatus)
    val unknownVendor = stringResource(Res.string.common_unknown)
    val vendorName = resolveListVendorName(document, unknownVendor, listStatus)
    val reference = when (listReference) {
        is DocumentListReferenceValue.Reference -> listReference.value
        is DocumentListReferenceValue.Status -> listReference.value.localized
    }
    val amountDouble = document.totalAmount?.toDouble()
    val dateLabel = formatShortDate(document.sortDate)
    val needsAttention = when (listStatus) {
        DocumentUiStatus.Queued,
        DocumentUiStatus.Processing,
        DocumentUiStatus.Failed -> true

        null -> computeListItemNeedsAttention(document)
        else -> false
    }
    val isProcessing = listStatus == DocumentUiStatus.Queued ||
        listStatus == DocumentUiStatus.Processing
    val source = document.effectiveOrigin.toUiSource()

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
            Icon(
                imageVector = document.documentType.iconizedOrDefault(),
                contentDescription = null,
                modifier = Modifier.size(Constraints.IconSize.xSmall),
                tint = if (needsAttention) {
                    MaterialTheme.colorScheme.statusWarning
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
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
// List-item helpers (DocumentListItemDto)
// =============================================================================

/**
 * Determines if a list item needs user attention using flat fields.
 */
internal fun computeListItemNeedsAttention(document: DocumentListItemDto): Boolean {
    val documentStatus = document.documentStatus
    val documentType = document.documentType
    val ingestionStatus = document.ingestionStatus
    val hasPendingMatchReview = document.hasPendingMatchReview

    if (documentStatus == DocumentStatus.Rejected) {
        return false
    }

    val ingestionNeedsAttention = ingestionStatus == IngestionStatus.Failed
    val draftNeedsReview = documentStatus == DocumentStatus.NeedsReview
    // In the list DTO we don't have confirmedEntity, so Confirmed status is treated as ready
    // unless it has a pending match review.
    val succeededButNoDraft = documentStatus == null && ingestionStatus == IngestionStatus.Succeeded
    val isNotConfirmed = documentStatus == null ||
        documentStatus != DocumentStatus.Confirmed ||
        documentType != DocumentType.BankStatement

    return hasPendingMatchReview ||
        (isNotConfirmed && (ingestionNeedsAttention || draftNeedsReview || succeededButNoDraft))
}

internal fun resolveListInlineStatus(document: DocumentListItemDto): DocumentUiStatus? {
    if (document.ingestionStatus == null) return null

    return when (val status = document.toUiStatus()) {
        DocumentUiStatus.Queued,
        DocumentUiStatus.Processing,
        DocumentUiStatus.Failed -> status

        DocumentUiStatus.Review,
        DocumentUiStatus.Ready,
        DocumentUiStatus.Unsupported -> null
    }
}

internal fun resolveListReferenceValue(
    document: DocumentListItemDto,
    listStatus: DocumentUiStatus? = resolveListInlineStatus(document),
): DocumentListReferenceValue {
    return if (listStatus == null) {
        // Use purposeRendered or filename as reference (no extractedData in list DTO)
        val ref = document.purposeRendered?.takeIf { it.isNotBlank() }
            ?: document.filename.takeIf { it.isNotBlank() }
            ?: "\u2014"
        DocumentListReferenceValue.Reference(ref)
    } else {
        DocumentListReferenceValue.Status(listStatus)
    }
}

internal fun resolveListVendorName(
    document: DocumentListItemDto,
    unknownLabel: String,
    listStatus: DocumentUiStatus? = resolveListInlineStatus(document),
): String {
    return if (listStatus == null) {
        document.counterpartyDisplayName?.takeIf { it.isNotBlank() }
            ?: document.filename.takeIf { it.isNotBlank() }
            ?: unknownLabel
    } else {
        document.filename.takeIf { it.isNotBlank() } ?: unknownLabel
    }
}

// =============================================================================
// DocumentDetailDto helpers (kept for review queue mapper)
// =============================================================================

/**
 * Determines if a document needs user attention.
 * Used by [DocumentQueueMapper] in the review context.
 */
internal fun computeNeedsAttention(document: DocumentDetailDto): Boolean {
    val ingestionStatus = document.latestIngestion?.status
    val documentStatus = document.draft?.documentStatus
    val documentType = document.draft?.documentType
    val content = document.draft?.content
    // For core financial types, confirmed content means the entity was created.
    // ClassifiedDoc types don't produce separate entities, so treat them as confirmed.
    val hasConfirmedContent = content is DocDto.Invoice.Confirmed ||
        content is DocDto.CreditNote.Confirmed ||
        content is DocDto.Receipt.Confirmed ||
        content is DocDto.BankStatement.Confirmed ||
        (documentStatus == DocumentStatus.Confirmed && content is DocDto.ClassifiedDoc)
    val hasPendingMatchReview = document.pendingMatchReview != null

    if (documentStatus == DocumentStatus.Rejected) {
        return false
    }

    val ingestionNeedsAttention = ingestionStatus == IngestionStatus.Failed
    val draftNeedsReview = documentStatus == DocumentStatus.NeedsReview
    val confirmedButNoEntity =
        documentStatus == DocumentStatus.Confirmed &&
            !hasConfirmedContent &&
            documentType != DocumentType.BankStatement
    val succeededButNoDraft = document.draft == null && ingestionStatus == IngestionStatus.Succeeded
    val isNotConfirmed = documentStatus == null ||
        documentStatus != DocumentStatus.Confirmed ||
        (!hasConfirmedContent && documentType != DocumentType.BankStatement)

    return hasPendingMatchReview ||
        confirmedButNoEntity ||
        (isNotConfirmed && (ingestionNeedsAttention || draftNeedsReview || succeededButNoDraft))
}

/**
 * Determines if a document is currently processing.
 */
internal fun computeIsProcessing(document: DocumentDetailDto): Boolean {
    val ingestionStatus = document.latestIngestion?.status
    return ingestionStatus == IngestionStatus.Processing ||
        ingestionStatus == IngestionStatus.Queued
}

/**
 * Resolves the primary description for a document (detail context).
 */
internal fun resolveDescription(document: DocumentDetailDto, unknownLabel: String): String {
    val content = document.draft?.content
    val ingestionStatus = document.latestIngestion?.status
    val documentNumber = when (content) {
        is DocDto.Invoice -> content.invoiceNumber.nonBlank()
        is DocDto.Receipt -> content.receiptNumber.nonBlank()
        is DocDto.CreditNote -> content.creditNoteNumber.nonBlank()
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc,
        null -> null
    }
    val counterparty = document.draft?.resolvedContact.displayName.nonBlank()
    val filename = document.document.filename.nonBlank()

    return when {
        counterparty != null && documentNumber != null -> "$counterparty \u2014 $documentNumber"
        counterparty == null && documentNumber != null && filename != null -> "$filename \u2014 $documentNumber"
        counterparty == null && documentNumber != null -> "Document \u2014 $documentNumber"
        counterparty != null -> counterparty
        ingestionStatus == IngestionStatus.Processing ||
            ingestionStatus == IngestionStatus.Queued -> "Processing document\u2026"
        else ->
            filename
                ?: unknownLabel
    }
}

/**
 * Resolves the counterparty name for display (detail context).
 * Used by [DocumentQueueMapper] in the review context.
 */
internal fun resolveCounterparty(document: DocumentDetailDto, emptyLabel: String = "\u2014"): String {
    val displayName = document.draft?.resolvedContact.displayName.nonBlank()
    if (displayName != null) return displayName

    val fromDraft = when (val data = document.draft?.content) {
        is DocDto.Invoice.Draft -> data.counterparty.name.nonBlank()
        is DocDto.Invoice.Confirmed -> null
        is DocDto.CreditNote.Draft -> data.counterparty.name.nonBlank()
        is DocDto.CreditNote.Confirmed -> null
        is DocDto.Receipt -> data.merchantName.nonBlank()
        is DocDto.BankStatement.Draft -> data.transactions.firstOrNull()?.counterparty?.name?.nonBlank()
        is DocDto.BankStatement.Confirmed -> null
        is DocDto.ClassifiedDoc,
        null -> null
    }
    return fromDraft ?: document.document.filename.nonBlank() ?: emptyLabel
}

// =============================================================================
// Shared Helpers
// =============================================================================

private val ResolvedContact?.displayName: String?
    get() = when (this) {
        is ResolvedContact.Linked -> name
        is ResolvedContact.Suggested -> name
        is ResolvedContact.Detected -> name
        is ResolvedContact.Unknown, null -> null
    }

private fun String?.nonBlank(): String? = this?.takeIf { it.isNotBlank() }

@Preview
@Composable
private fun DocumentTableHeaderRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentTableHeaderRow()
    }
}
