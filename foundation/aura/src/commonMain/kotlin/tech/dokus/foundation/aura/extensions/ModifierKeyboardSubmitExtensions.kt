package tech.dokus.foundation.aura.extensions

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Intercepts Enter key (without Shift) to trigger a submit action.
 *
 * Uses [onPreviewKeyEvent] so this fires BEFORE child composables
 * (like multi-line TextFields) consume the Enter key for newlines.
 * Shift+Enter still inserts a newline in multi-line fields.
 *
 * Usage:
 * ```
 * OutlinedTextField(
 *     modifier = Modifier
 *         .fillMaxWidth()
 *         .submitOnEnter(enabled = text.isNotBlank()) { onSubmit() },
 * )
 * ```
 */
fun Modifier.submitOnEnter(
    enabled: Boolean,
    onSubmit: () -> Unit,
): Modifier = onPreviewKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown &&
        event.key == Key.Enter &&
        !event.isShiftPressed &&
        enabled
    ) {
        onSubmit()
        true
    } else {
        false
    }
}
