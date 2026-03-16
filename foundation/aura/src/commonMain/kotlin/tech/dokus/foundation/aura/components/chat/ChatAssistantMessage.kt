package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val AvatarSize = 16.dp
private val AvatarCorner = 4.dp
private val AvatarFontSize = 7.sp
private const val DokusLabel = "Dokus"

/**
 * AI assistant message with "D" avatar badge and "Dokus" label.
 * Matches v29 design: left-aligned, full-width, with content slot.
 *
 * @param content Slot for message body — text, structured cards, citations etc.
 */
@Composable
fun ChatAssistantMessage(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        // Avatar + label row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
        ) {
            // "D" avatar badge
            Box(
                modifier = Modifier
                    .size(AvatarSize)
                    .background(
                        color = MaterialTheme.colorScheme.amberSoft,
                        shape = RoundedCornerShape(AvatarCorner),
                    )
                    .border(
                        width = Constraints.Stroke.thin,
                        color = MaterialTheme.colorScheme.borderAmber,
                        shape = RoundedCornerShape(AvatarCorner),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "D",
                    fontSize = AvatarFontSize,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = DokusLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.textMuted,
            )
        }

        // Message content
        content()
    }
}

@Preview
@Composable
private fun ChatAssistantMessagePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatAssistantMessage {
            Text(
                text = "Based on your confirmed documents, here are the largest expenses.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
