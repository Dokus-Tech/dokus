package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.documents_local_action_dismiss
import tech.dokus.aura.resources.upload_action_retry
import tech.dokus.features.cashflow.presentation.documents.model.DocumentsLocalUploadRow
import tech.dokus.features.cashflow.presentation.documents.model.localized
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private object LocalUploadTableColumns {
    val Vendor = DokusTableColumnSpec(weight = 1f)
    val Reference = DokusTableColumnSpec(width = 150.dp)
    val Amount = DokusTableColumnSpec(width = 90.dp, horizontalAlignment = Alignment.End)
    val Date = DokusTableColumnSpec(width = 70.dp)
    val Source = DokusTableColumnSpec(width = 64.dp)
}

@Composable
internal fun DocumentLocalUploadTableRow(
    row: DocumentsLocalUploadRow,
    onRetry: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusLabel = row.status.localized
    val statusDot = when (row.status) {
        DocumentsLocalUploadRow.Status.Uploading,
        DocumentsLocalUploadRow.Status.PreparingDocument,
        DocumentsLocalUploadRow.Status.ReadingDocument -> StatusDotType.Warning
        DocumentsLocalUploadRow.Status.Failed -> StatusDotType.Error
    }

    DokusTableRow(
        modifier = modifier,
        minHeight = 48.dp,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large)
    ) {
        DokusTableCell(LocalUploadTableColumns.Vendor) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusDot(type = statusDot, size = 5.dp)
                    Text(
                        text = row.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.5.sp,
                            fontStyle = if (row.status != DocumentsLocalUploadRow.Status.Failed) {
                                FontStyle.Italic
                            } else {
                                FontStyle.Normal
                            }
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (row.status == DocumentsLocalUploadRow.Status.Failed) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { onRetry(row.taskId) }) {
                            Text(text = stringResource(Res.string.upload_action_retry))
                        }
                        TextButton(onClick = { onDismiss(row.taskId) }) {
                            Text(text = stringResource(Res.string.documents_local_action_dismiss))
                        }
                    }
                }
            }
        }

        DokusTableCell(LocalUploadTableColumns.Reference) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp),
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        DokusTableCell(LocalUploadTableColumns.Amount) {
            DashCell()
        }

        DokusTableCell(LocalUploadTableColumns.Date) {
            DashCell()
        }

        DokusTableCell(LocalUploadTableColumns.Source) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
internal fun DocumentLocalUploadMobileRow(
    row: DocumentsLocalUploadRow,
    onRetry: (String) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val statusLabel = row.status.localized
    val statusDot = when (row.status) {
        DocumentsLocalUploadRow.Status.Uploading,
        DocumentsLocalUploadRow.Status.PreparingDocument,
        DocumentsLocalUploadRow.Status.ReadingDocument -> StatusDotType.Warning
        DocumentsLocalUploadRow.Status.Failed -> StatusDotType.Error
    }

    DokusCardSurface(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatusDot(type = statusDot, size = 6.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = row.fileName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        fontStyle = if (row.status != DocumentsLocalUploadRow.Status.Failed) {
                            FontStyle.Italic
                        } else {
                            FontStyle.Normal
                        }
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statusLabel,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (row.status == DocumentsLocalUploadRow.Status.Failed) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InlineRowAction(
                        label = stringResource(Res.string.upload_action_retry),
                        onClick = { onRetry(row.taskId) },
                        isPrimary = true
                    )
                    InlineRowAction(
                        label = stringResource(Res.string.documents_local_action_dismiss),
                        onClick = { onDismiss(row.taskId) },
                        isPrimary = false
                    )
                }
            } else {
                Text(
                    text = "\u203A",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.textFaint
                )
            }
        }
    }
}

@Composable
private fun InlineRowAction(
    label: String,
    onClick: () -> Unit,
    isPrimary: Boolean,
) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (isPrimary) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.textMuted
        },
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
    )
}

@Composable
private fun DashCell() {
    Text(
        text = "\u2014",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.textMuted
    )
}

@Preview(name = "Local Upload Mobile Uploading", widthDp = 390, heightDp = 130)
@Composable
private fun DocumentLocalUploadMobileUploadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentLocalUploadMobileRow(
            row = DocumentsLocalUploadRow(
                taskId = "preview-local-uploading",
                fileName = "receipt-feb-28.pdf",
                status = DocumentsLocalUploadRow.Status.Uploading
            ),
            onRetry = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Local Upload Mobile Preparing", widthDp = 390, heightDp = 130)
@Composable
private fun DocumentLocalUploadMobilePreparingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentLocalUploadMobileRow(
            row = DocumentsLocalUploadRow(
                taskId = "preview-local-preparing",
                fileName = "tesla-belgium.pdf",
                status = DocumentsLocalUploadRow.Status.PreparingDocument
            ),
            onRetry = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Local Upload Mobile Reading", widthDp = 390, heightDp = 130)
@Composable
private fun DocumentLocalUploadMobileReadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentLocalUploadMobileRow(
            row = DocumentsLocalUploadRow(
                taskId = "preview-local-reading",
                fileName = "anthropic-feb.pdf",
                status = DocumentsLocalUploadRow.Status.ReadingDocument
            ),
            onRetry = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Local Upload Mobile Failed", widthDp = 390, heightDp = 130)
@Composable
private fun DocumentLocalUploadMobileFailedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocumentLocalUploadMobileRow(
            row = DocumentsLocalUploadRow(
                taskId = "preview-local-failed",
                fileName = "unknown-vendor.pdf",
                status = DocumentsLocalUploadRow.Status.Failed
            ),
            onRetry = {},
            onDismiss = {}
        )
    }
}
