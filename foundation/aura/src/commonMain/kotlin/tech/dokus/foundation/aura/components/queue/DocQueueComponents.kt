package tech.dokus.foundation.aura.components.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.a11y_back_to_all_documents
import tech.dokus.aura.resources.document_queue_all_docs
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Queue header with a back button ("‹ All docs") and a position counter ("1/10").
 */
@Composable
fun DocQueueHeader(
    positionText: String,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val backDescription = stringResource(Res.string.a11y_back_to_all_documents)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .semantics { contentDescription = backDescription }
                .clickable(onClick = onExit),
        ) {
            Text(
                text = "\u2039",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = stringResource(Res.string.document_queue_all_docs),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = positionText,
            fontSize = 10.sp,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = MaterialTheme.colorScheme.textFaint,
        )
    }
}

/**
 * Single row in the document queue list with status dot, vendor name, date, and amount.
 * Selected items get a warm background and amber right-edge indicator.
 */
@Composable
fun DocQueueItemRow(
    vendorName: String,
    date: String,
    amount: String,
    statusDotType: StatusDotType,
    statusTextColor: Color,
    statusDetail: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warmBg = MaterialTheme.colorScheme.surfaceHover
    val amberColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier
                        .background(warmBg)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                color = amberColor,
                                topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                                size = Size(2.dp.toPx(), size.height),
                            )
                        }
                } else Modifier
            )
                .clickable(onClick = onClick)
                .padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.small,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        StatusDot(
            type = statusDotType,
            size = Constraints.StatusDot.size,
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vendorName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Constraints.Spacing.xxSmall))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
                statusDetail?.takeIf { it.isNotBlank() }?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        Text(
            text = amount,
            style = MaterialTheme.typography.labelMedium,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }

    HorizontalDivider(color = Color.Black.copy(alpha = 0.03f))
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview
@Composable
private fun DocQueueHeaderPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DocQueueHeader(
            positionText = "3/12",
            onExit = {},
        )
    }
}

@Preview
@Composable
private fun DocQueueItemRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        Column {
            DocQueueItemRow(
                vendorName = "Acme Corp",
                date = "Feb 15",
                amount = "1,250.00",
                statusDotType = StatusDotType.Warning,
                statusTextColor = MaterialTheme.colorScheme.statusWarning,
                statusDetail = "Needs review",
                isSelected = true,
                onClick = {},
            )
            DocQueueItemRow(
                vendorName = "Tech Solutions",
                date = "Feb 14",
                amount = "890.50",
                statusDotType = StatusDotType.Confirmed,
                statusTextColor = MaterialTheme.colorScheme.statusConfirmed,
                statusDetail = "Paid",
                isSelected = false,
                onClick = {},
            )
        }
    }
}
