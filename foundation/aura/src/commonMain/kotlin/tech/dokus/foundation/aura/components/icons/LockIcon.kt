package tech.dokus.foundation.aura.components.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * A lock icon indicating a field or section is locked/immutable.
 *
 * Used for:
 * - Fields that cannot be edited (e.g., after PEPPOL registration)
 * - Verified/confirmed information
 * - Read-only values
 *
 * @param modifier Optional modifier
 * @param tint Icon color (defaults to textMuted)
 */
@Composable
fun LockIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.textMuted,
) {
    Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        modifier = modifier.size(Constraints.IconSize.small),
        tint = tint,
    )
}
