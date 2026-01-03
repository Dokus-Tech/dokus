package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_thinking
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun SendingIndicator(
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!isSending) return

    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomCenter,
    ) {
        DokusCardSurface(
            variant = DokusCardVariant.Soft,
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = Constrains.Spacing.medium,
                    vertical = Constrains.Spacing.small
                ),
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Text(
                    text = stringResource(Res.string.chat_thinking),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
