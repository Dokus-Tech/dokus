package tech.dokus.foundation.aura.components.badges

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.amberWhisper
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val BadgeRadius = 3.dp
private val StandardPaddingH = 6.dp
private val StandardPaddingV = 2.dp
private val CompactPaddingH = 4.dp
private val CompactPaddingV = 1.dp

/**
 * Document origin type.
 */
enum class DocumentSource { Pdf, Peppol }

/**
 * Inline badge showing document origin — PDF or PEPPOL.
 *
 * @param source Document origin
 * @param compact Smaller variant for mobile
 */
@Composable
fun SourceBadge(
    source: DocumentSource,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val shape = RoundedCornerShape(BadgeRadius)
    val fontSize = if (compact) 8.sp else 9.sp
    val paddingH = if (compact) CompactPaddingH else StandardPaddingH
    val paddingV = if (compact) CompactPaddingV else StandardPaddingV

    val (text, textColor, bgColor, borderColor) = when (source) {
        DocumentSource.Peppol -> BadgeStyle(
            text = "PEPPOL",
            textColor = MaterialTheme.colorScheme.primary,
            bgColor = MaterialTheme.colorScheme.amberWhisper,
            borderColor = MaterialTheme.colorScheme.borderAmber,
        )
        DocumentSource.Pdf -> BadgeStyle(
            text = "PDF",
            textColor = MaterialTheme.colorScheme.textMuted,
            bgColor = MaterialTheme.colorScheme.surfaceVariant,
            borderColor = MaterialTheme.colorScheme.outlineVariant,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .padding(horizontal = paddingH, vertical = paddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
            color = textColor,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun SourceBadgePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        SourceBadge(source = DocumentSource.Peppol)
    }
}

private data class BadgeStyle(
    val text: String,
    val textColor: androidx.compose.ui.graphics.Color,
    val bgColor: androidx.compose.ui.graphics.Color,
    val borderColor: androidx.compose.ui.graphics.Color,
)
