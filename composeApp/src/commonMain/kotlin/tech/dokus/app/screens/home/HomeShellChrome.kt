package tech.dokus.app.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.settings_current_workspace
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.navigation.ProfilePopover
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.style.glassHeader
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

internal data class HomeShellProfileData(
    val fullName: String,
    val email: String,
    val tierLabel: String?,
) {
    val initials: String
        get() = fullName
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
}

@Composable
internal fun DesktopSidebarBottomControls(
    tenantState: DokusState<Tenant>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.medium)
            .border(
                width = sizing.strokeThin,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = spacing.small, vertical = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        DesktopWorkspaceArea(
            tenantState = tenantState,
            onClick = onWorkspaceClick,
            modifier = Modifier.weight(1f)
        )
        DesktopProfileMenuButton(
            profileData = profileData,
            isLoggingOut = isLoggingOut,
            onProfileClick = onProfileClick,
            onLogoutClick = onLogoutClick
        )
    }
}

@Composable
private fun DesktopWorkspaceArea(
    tenantState: DokusState<Tenant>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tenant = (tenantState as? DokusState.Success<Tenant>)?.data
    val isLoading = tenantState is DokusState.Loading

    Row(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .hoverable(interactionSource = interactionSource)
            .background(
                if (isHovered) MaterialTheme.colorScheme.surfaceHover else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.small, vertical = spacing.xSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.small)
    ) {
        if (isLoading) {
            ShimmerBox(
                modifier = Modifier
                    .size(sizing.iconMedium)
                    .clip(MaterialTheme.shapes.extraSmall)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xSmall)
            ) {
                ShimmerLine(
                    modifier = Modifier.fillMaxWidth(0.65f),
                    height = spacing.medium
                )
                ShimmerLine(
                    modifier = Modifier.fillMaxWidth(0.45f),
                    height = spacing.small
                )
            }
        } else if (tenant != null) {
            val workspaceName = tenant.displayName.value
            val workspaceVat = tenant.vatNumber.formatted
            val initial = tenant.displayName.value.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"

            CompanyAvatarImage(
                avatarUrl = tenant.avatar?.small,
                initial = initial,
                size = AvatarSize.ExtraSmall,
                shape = AvatarShape.RoundedSquare
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxSmall)
            ) {
                Text(
                    text = workspaceName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = workspaceVat,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Text(
                text = stringResource(Res.string.settings_current_workspace),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DesktopProfileMenuButton(
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val sizing = MaterialTheme.dokusSizing
    var popoverVisible by remember { mutableStateOf(false) }
    val initials = profileData?.initials ?: ""

    Box {
        MonogramAvatar(
            initials = initials,
            size = sizing.avatarExtraSmall,
            radius = sizing.avatarExtraSmall / 4,
            modifier = Modifier.clickable { popoverVisible = true },
        )

        ProfilePopover(
            isVisible = popoverVisible,
            onDismiss = { popoverVisible = false },
            userName = profileData?.fullName ?: "",
            userEmail = profileData?.email ?: "",
            userInitials = initials,
            tierLabel = profileData?.tierLabel ?: "",
            onProfileClick = onProfileClick,
            onLogoutClick = {
                if (!isLoggingOut) onLogoutClick()
            },
        )
    }
}

@Composable
internal fun DesktopShellTopBar(
    topBarConfig: HomeShellTopBarConfig,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    val effects = MaterialTheme.dokusEffects
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.glassHeader)
                .padding(horizontal = spacing.xLarge, vertical = spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Search or title slot
            when (val mode = topBarConfig.mode) {
                is HomeShellTopBarMode.Search -> {
                    PSearchFieldCompact(
                        value = mode.query,
                        onValueChange = mode.onQueryChange,
                        placeholder = mode.placeholder,
                        onClear = mode.onClear,
                        modifier = Modifier.widthIn(
                            min = sizing.searchFieldMinWidth,
                            max = sizing.searchFieldMaxWidth
                        )
                    )
                }

                is HomeShellTopBarMode.Title -> {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Actions slot
            RouteTopBarActions(actions = topBarConfig.actions)

            // Date display
            val dateText = remember { formattedCurrentDate() }
            Spacer(modifier = Modifier.width(spacing.medium))
            Text(
                text = dateText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textFaint,
            )
        }
        HorizontalDivider(color = effects.railTrackLine)
    }
}

@Composable
internal fun MobileShellTopBar(
    profileData: HomeShellProfileData?,
    onProfileClick: () -> Unit,
) {
    val effects = MaterialTheme.dokusEffects
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.glassHeader)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large, vertical = spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNameText(modifier = Modifier.weight(1f))

            MonogramAvatar(
                initials = profileData?.initials ?: "",
                size = sizing.avatarExtraSmall,
                radius = sizing.avatarExtraSmall / 4,
                modifier = Modifier.clickable(onClick = onProfileClick),
            )
        }

        HorizontalDivider(color = effects.railTrackLine)
    }
}

@Composable
private fun RowScope.RouteTopBarActions(
    actions: List<HomeShellTopBarAction>,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    actions.forEachIndexed { index, action ->
        when (action) {
            is HomeShellTopBarAction.Icon -> {
                IconButton(
                    onClick = action.onClick,
                    enabled = action.enabled,
                    modifier = Modifier.size(sizing.buttonHeight)
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = action.contentDescription
                    )
                }
            }

            is HomeShellTopBarAction.Text -> {
                TextButton(
                    onClick = action.onClick,
                    enabled = action.enabled
                ) {
                    Text(text = action.label)
                }
            }
        }

        if (index < actions.lastIndex) {
            Spacer(modifier = Modifier.width(spacing.xSmall))
        }
    }
}

private val MonthNames = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

/** Format current date as "18 Feb 2026". Uses kotlin.time to avoid compat issues. */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun formattedCurrentDate(): String {
    val now = kotlin.time.Clock.System.now()
    val epochDays = (now.epochSeconds / 86400).toInt()
    // Civil date from epoch days (Meeus algorithm)
    val z = epochDays + 719468
    val era = (if (z >= 0) z else z - 146096) / 146097
    val doe = z - era * 146097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + (if (mp < 10) 3 else -9)
    val year = y + (if (m <= 2) 1 else 0)
    return "$d ${MonthNames[m - 1]} $year"
}
