package tech.dokus.foundation.aura.extensions

import androidx.compose.foundation.focusable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Adds vertical arrow-key navigation to a composable.
 *
 * Requests focus on first composition so keys work immediately.
 * Uses [onKeyEvent] (not onPreviewKeyEvent), so child composables
 * like text fields get first priority for key events.
 *
 * Usage:
 * ```
 * Column(modifier = Modifier.arrowKeyNavigation(
 *     onUp = { selectPrevious() },
 *     onDown = { selectNext() },
 * ))
 * ```
 */
fun Modifier.arrowKeyNavigation(
    onUp: () -> Unit,
    onDown: () -> Unit,
): Modifier = composed {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    this
        .focusRequester(focusRequester)
        .focusable()
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown) {
                when (event.key) {
                    Key.DirectionUp -> { onUp(); true }
                    Key.DirectionDown -> { onDown(); true }
                    else -> false
                }
            } else {
                false
            }
        }
}
