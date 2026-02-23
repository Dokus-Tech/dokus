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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import tech.dokus.foundation.aura.constrains.Constraints
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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

    val colors = when (role) {
        ContactRole.Vendor -> BadgeColors(
            text = MaterialTheme.colorScheme.textMuted,
            background = MaterialTheme.colorScheme.surfaceVariant,
        )
        ContactRole.Bank -> BadgeColors(
            text = MaterialTheme.colorScheme.tertiary,
            background = MaterialTheme.colorScheme.tertiaryContainer,
        )
        ContactRole.Accountant -> BadgeColors(
            text = MaterialTheme.colorScheme.primary,
            background = MaterialTheme.colorScheme.amberSoft,
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.background)
            .padding(horizontal = PaddingH, vertical = PaddingV),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = role.name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
            maxLines = 1,
        )
    }
}

@Preview
@Composable
private fun RoleBadgePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        RoleBadge(role = ContactRole.Vendor)
    }
}

private data class BadgeColors(val text: Color, val background: Color)
