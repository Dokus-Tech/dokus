package tech.dokus.features.cashflow.presentation.documents.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.dokus.foundation.aura.components.layout.DokusTableCell
import tech.dokus.foundation.aura.components.layout.DokusTableColumnSpec
import tech.dokus.foundation.aura.components.layout.DokusTableRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.constrains.Constraints

private object DropHintTableColumns {
    val Vendor = DokusTableColumnSpec(weight = 1f)
    val Reference = DokusTableColumnSpec(width = 150.dp)
    val Amount = DokusTableColumnSpec(width = 90.dp, horizontalAlignment = Alignment.End)
    val Date = DokusTableColumnSpec(width = 70.dp)
    val Source = DokusTableColumnSpec(width = 64.dp)
}

@Composable
internal fun DocumentsDropHintTableRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)

    DokusTableRow(
        modifier = modifier.drawBehind {
            drawRoundRect(
                color = fillColor,
                cornerRadius = CornerRadius(10.dp.toPx())
            )
            drawRoundRect(
                color = borderColor,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(
                        intervals = floatArrayOf(8f, 8f),
                        phase = 0f
                    )
                ),
                cornerRadius = CornerRadius(10.dp.toPx())
            )
        },
        minHeight = 48.dp,
        contentPadding = PaddingValues(horizontal = Constraints.Spacing.large)
    ) {
        DokusTableCell(DropHintTableColumns.Vendor) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusDot(
                    type = StatusDotType.Warning,
                    size = 5.dp,
                )
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.5.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        DokusTableCell(DropHintTableColumns.Reference) {
            DashCell()
        }
        DokusTableCell(DropHintTableColumns.Amount) {
            DashCell()
        }
        DokusTableCell(DropHintTableColumns.Date) {
            DashCell()
        }
        DokusTableCell(DropHintTableColumns.Source) {
            Spacer(modifier = Modifier.width(1.dp))
        }
    }
}

@Composable
private fun DashCell() {
    Text(
        text = "\u2014",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    )
}
