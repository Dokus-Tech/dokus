package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import kotlinx.coroutines.delay
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.textMuted

/**
 * Copyable label/value row (Design System v1).
 *
 * Useful for IDs, credentials, and other short values.
 */
@Composable
fun PCopyRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    copyText: String = "Copy",
    copiedText: String = "Copied",
) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (!copied) return@LaunchedEffect
        delay(2000)
        copied = false
    }

    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constrains.Spacing.medium,
                    vertical = Constrains.Spacing.medium
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f, fill = true)) {
                Row {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                    Text(
                        text = "  $value",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            TextButton(
                onClick = {
                    clipboard.setText(AnnotatedString(value))
                    copied = true
                }
            ) {
                Text(
                    text = if (copied) copiedText else copyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (copied) MaterialTheme.colorScheme.statusConfirmed else MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

