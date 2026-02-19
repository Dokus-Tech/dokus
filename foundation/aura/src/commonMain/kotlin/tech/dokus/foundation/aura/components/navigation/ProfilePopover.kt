package tech.dokus.foundation.aura.components.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.badges.TierBadge
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val PopoverWidth = Constraints.Shell.sidebarWidth
private val PopoverRadius = Constraints.Spacing.medium
private val PopoverShadow = Constraints.Spacing.medium
private const val AnimDuration = 150
private const val ChevronChar = "\u203A" // ›

// Avatar in popover
private val AvatarSize = Constraints.AvatarSize.small
private val AvatarRadius = Constraints.Spacing.small

/**
 * Profile popover triggered by sidebar footer.
 *
 * @param isVisible Controls visibility with animation
 * @param onDismiss Called when clicking outside
 * @param userName Display name
 * @param userEmail Email address
 * @param userInitials Initials for MonogramAvatar
 * @param tierLabel Subscription tier (e.g. "Core")
 * @param onProfileClick Navigate to profile
 * @param onLogoutClick Log out action
 */
@Composable
fun ProfilePopover(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    userName: String,
    userEmail: String,
    userInitials: String,
    tierLabel: String,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (isVisible) {
        // Backdrop to catch outside clicks
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Dismiss area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                )

                // Popover content
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(AnimDuration)) +
                        scaleIn(tween(AnimDuration), initialScale = 0.95f) +
                        slideInVertically(tween(AnimDuration)) { 4 },
                    exit = fadeOut(tween(AnimDuration)) +
                        scaleOut(tween(AnimDuration), targetScale = 0.95f) +
                        slideOutVertically(tween(AnimDuration)) { 4 },
                    modifier = modifier
                        .align(Alignment.BottomStart)
                        .padding(
                            start = Constraints.Spacing.medium,
                            bottom = Constraints.IconSize.xxLarge - Constraints.Spacing.xSmall,
                        ),
                ) {
                    PopoverContent(
                        userName = userName,
                        userEmail = userEmail,
                        userInitials = userInitials,
                        tierLabel = tierLabel,
                        onProfileClick = {
                            onProfileClick()
                            onDismiss()
                        },
                        onLogoutClick = {
                            onLogoutClick()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PopoverContent(
    userName: String,
    userEmail: String,
    userInitials: String,
    tierLabel: String,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.width(PopoverWidth),
        shape = RoundedCornerShape(PopoverRadius),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = PopoverShadow,
        tonalElevation = Constraints.Elevation.none,
    ) {
        Column {
            // User card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Constraints.Spacing.large,
                        vertical = Constraints.Spacing.medium + Constraints.Spacing.xxSmall
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
            ) {
                MonogramAvatar(
                    initials = userInitials,
                    size = AvatarSize,
                    radius = AvatarRadius,
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = userEmail,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                    Spacer(Modifier.height(Constraints.Spacing.xSmall))
                    TierBadge(label = tierLabel)
                }
            }

            HorizontalDivider(
                thickness = Constraints.Stroke.thin,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // Menu items
            PopoverMenuItem(label = "Profile", onClick = onProfileClick)

            HorizontalDivider(
                thickness = Constraints.Stroke.thin,
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            // Log out
            Text(
                text = "Log Out",
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onLogoutClick)
                    .padding(
                        horizontal = Constraints.Spacing.large,
                        vertical = Constraints.Spacing.small + Constraints.Spacing.xxSmall
                    ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun PopoverMenuItem(
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val hoverColor = MaterialTheme.colorScheme.surfaceHover

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .drawBehind { if (isHovered) drawRect(hoverColor) }
            .padding(
                horizontal = Constraints.Spacing.large,
                vertical = Constraints.Spacing.small + Constraints.Spacing.xxSmall
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = ChevronChar,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.textFaint,
        )
    }
}
