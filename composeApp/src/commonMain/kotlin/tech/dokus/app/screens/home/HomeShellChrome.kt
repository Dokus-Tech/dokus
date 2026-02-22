package tech.dokus.app.screens.home

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.a11y_profile_menu
import tech.dokus.aura.resources.date_month_short_apr
import tech.dokus.aura.resources.date_month_short_aug
import tech.dokus.aura.resources.date_month_short_dec
import tech.dokus.aura.resources.date_month_short_feb
import tech.dokus.aura.resources.date_month_short_jan
import tech.dokus.aura.resources.date_month_short_jul
import tech.dokus.aura.resources.date_month_short_jun
import tech.dokus.aura.resources.date_month_short_mar
import tech.dokus.aura.resources.date_month_short_may
import tech.dokus.aura.resources.date_month_short_nov
import tech.dokus.aura.resources.date_month_short_oct
import tech.dokus.aura.resources.date_month_short_sep
import tech.dokus.aura.resources.settings_current_workspace
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.MonogramAvatar
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.components.navigation.ProfilePopover
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.style.glassHeader
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.time.Clock

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

// Cashflow desktop baseline: 42dp control height + 12dp breathing room.
private val DesktopShellTopBarHeight = Constraints.Height.button + Constraints.Spacing.medium

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
            contentDescription = stringResource(Res.string.a11y_profile_menu),
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
private fun DesktopShellTopBarFrame(
    actions: List<HomeShellTopBarAction>,
    modifier: Modifier = Modifier,
    leadingContent: @Composable RowScope.() -> Unit,
) {
    val spacing = MaterialTheme.dokusSpacing
    val effects = MaterialTheme.dokusEffects
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(DesktopShellTopBarHeight)
                .background(MaterialTheme.colorScheme.glassHeader)
                .padding(horizontal = spacing.xLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leadingContent()

            Spacer(modifier = Modifier.weight(1f))

            // Actions slot
            RouteTopBarActions(actions = actions)

            // Date display
            val dateText = formattedCurrentDate()
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
internal fun DesktopShellTopBar(
    topBarConfig: HomeShellTopBarConfig,
    modifier: Modifier = Modifier,
) {
    val sizing = MaterialTheme.dokusSizing
    DesktopShellTopBarFrame(
        actions = topBarConfig.actions,
        modifier = modifier
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
                Column {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    mode.subtitle?.let { subtitle ->
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
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
            .statusBarsPadding()
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
                contentDescription = stringResource(Res.string.a11y_profile_menu),
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

/** Format current date as "18 Feb 2026" using localized month names. */
@Composable
private fun formattedCurrentDate(): String {
    val today = remember {
        Clock.System.todayIn(TimeZone.currentSystemDefault())
    }
    val monthName = shortMonthName(today.month)
    return "${today.day} $monthName ${today.year}"
}

@Composable
private fun shortMonthName(month: Month): String = when (month) {
    Month.JANUARY -> stringResource(Res.string.date_month_short_jan)
    Month.FEBRUARY -> stringResource(Res.string.date_month_short_feb)
    Month.MARCH -> stringResource(Res.string.date_month_short_mar)
    Month.APRIL -> stringResource(Res.string.date_month_short_apr)
    Month.MAY -> stringResource(Res.string.date_month_short_may)
    Month.JUNE -> stringResource(Res.string.date_month_short_jun)
    Month.JULY -> stringResource(Res.string.date_month_short_jul)
    Month.AUGUST -> stringResource(Res.string.date_month_short_aug)
    Month.SEPTEMBER -> stringResource(Res.string.date_month_short_sep)
    Month.OCTOBER -> stringResource(Res.string.date_month_short_oct)
    Month.NOVEMBER -> stringResource(Res.string.date_month_short_nov)
    Month.DECEMBER -> stringResource(Res.string.date_month_short_dec)
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun DesktopShellTopBarSearchPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DesktopShellTopBar(
            topBarConfig = HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Search(
                    query = "",
                    placeholder = "Search documents",
                    onQueryChange = {}
                ),
                actions = listOf(
                    HomeShellTopBarAction.Icon(
                        icon = Icons.Default.Upload,
                        contentDescription = "Upload",
                        onClick = {}
                    )
                )
            )
        )
    }
}

@Preview
@Composable
private fun DesktopShellTopBarTitleOnlyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DesktopShellTopBar(
            topBarConfig = HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title(title = "Accountant")
            )
        )
    }
}

@Preview
@Composable
private fun DesktopShellTopBarTitleSubtitlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DesktopShellTopBar(
            topBarConfig = HomeShellTopBarConfig(
                mode = HomeShellTopBarMode.Title(
                    title = "Cashflow",
                    subtitle = "Track incoming and outgoing payments"
                )
            )
        )
    }
}

@Preview
@Composable
private fun MobileShellTopBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MobileShellTopBar(
            profileData = HomeShellProfileData(
                fullName = "John Doe",
                email = "john@dokus.be",
                tierLabel = "Core"
            ),
            onProfileClick = {}
        )
    }
}
