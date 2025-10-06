package ai.dokus.foundation.ui.extensions

import ai.dokus.foundation.ui.theme.ripple
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

@Composable
fun Modifier.clickableWithRipple(
    onClick: () -> Unit
): Modifier {
    return clickable(
        interactionSource = remember { MutableInteractionSource() },
        onClick = onClick,
        indication = MaterialTheme.colorScheme.ripple
    )
}