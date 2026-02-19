package tech.dokus.foundation.aura.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import tech.dokus.foundation.aura.style.textMuted

/**
 * Uppercase section label (v2 atom).
 *
 * Style: 10sp, weight 600, uppercase, letter-spacing 0.1em.
 * Uses: stat card labels, server label in profile, danger zone label.
 */
@Composable
fun DokusLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.textMuted,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.em,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
