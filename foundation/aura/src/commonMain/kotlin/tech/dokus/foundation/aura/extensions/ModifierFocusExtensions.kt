package tech.dokus.foundation.aura.extensions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import tech.dokus.foundation.aura.local.LocalScreenSize

/**
 * Clears focus when the user taps outside focused inputs.
 *
 * Useful on mobile screens with text fields to dismiss the software keyboard.
 * By default this behavior is only active on non-large screens.
 */
fun Modifier.dismissKeyboardOnTapOutside(
    enabled: Boolean = true,
    mobileOnly: Boolean = true,
): Modifier = composed {
    val isLargeScreen = LocalScreenSize.current.isLarge
    val isActive = enabled && (!mobileOnly || !isLargeScreen)
    if (!isActive) return@composed this

    val focusManager = LocalFocusManager.current
    pointerInput(focusManager) {
        detectTapGestures {
            focusManager.clearFocus()
        }
    }
}
