package tech.dokus.foundation.aura.components.badges

import androidx.compose.foundation.background
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
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.textMuted

private val PaddingH = Constraints.Spacing.small
private val PaddingV = Constraints.Spacing.xxSmall

/**
 * Contact role type.
 */
enum class ContactRole {
    Vendor,
    Bank,
    Accountant,
}

/**
 * Contact role badge.
 *
 * Uses: Contact list, contact detail.
 *
 * @param role Contact role determining style
 */
@Composable
fun RoleBadge(
    role: ContactRole,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Constraints.CornerRadius.badge)

    val (textColor, bgColor) = when (role) {
        ContactRole.Vendor -> Pair(
            MaterialTheme.colorScheme.textMuted,
            MaterialTheme.colorScheme.surfaceVariant,
        )
        ContactRole.Bank -> Pair(
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.tertiaryContainer,
        )
        ContactRole.Accountant -> Pair(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.amberSoft,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .padding(horizontal = PaddingH, vertical = PaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = role.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            maxLines = 1,
        )
    }
}
