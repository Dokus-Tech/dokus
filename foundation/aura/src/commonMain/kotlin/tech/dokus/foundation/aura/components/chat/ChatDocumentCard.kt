package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_download_pdf
import tech.dokus.domain.model.ai.DocumentReference
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.textMuted
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val CardShape = RoundedCornerShape(7.dp)
private val TypeDotSize = 5.dp

/**
 * Document card for AI chat responses.
 * Shows type dot, name, ref, amount, and a download button.
 */
@Composable
fun ChatDocumentCard(
    doc: DocumentReference,
    onDownload: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val typeColor = when (doc.type) {
        "Invoice" -> MaterialTheme.colorScheme.primary
        "Expense", "Receipt" -> MaterialTheme.colorScheme.statusError
        else -> MaterialTheme.colorScheme.textMuted
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                CardShape,
            )
            .border(Constraints.Stroke.thin, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .clickable(onClick = onClick)
            .padding(
                horizontal = if (compact) Constraints.Spacing.small else Constraints.Spacing.medium,
                vertical = if (compact) Constraints.Spacing.small else Constraints.Spacing.small,
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
    ) {
        // Type dot
        Box(
            modifier = Modifier
                .size(TypeDotSize)
                .background(typeColor, CircleShape),
        )

        // Name + ref + amount
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = doc.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                doc.ref?.let { ref ->
                    Text(
                        text = ref,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
                    )
                }
                doc.amount?.let { amount ->
                    Text(
                        text = "\u20ac${formatAmount(amount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }
        }

        // Download button
        PButton(
            text = stringResource(Res.string.chat_download_pdf),
            variant = PButtonVariant.OutlineMuted,
            onClick = onDownload,
        )
    }
}

private fun formatAmount(amount: Double): String {
    val whole = amount.toLong()
    val cents = ((amount - whole) * 100).toLong().let { kotlin.math.abs(it) }
    return "$whole.${cents.toString().padStart(2, '0')}"
}

@Preview
@Composable
private fun ChatDocumentCardPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatDocumentCard(
            doc = DocumentReference(
                name = "SRL Accounting & Tax",
                ref = "20260050",
                type = "Invoice",
                amount = 798.60,
            ),
            onDownload = {},
            onClick = {},
        )
    }
}
