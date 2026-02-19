package tech.dokus.app.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.profile_logout
import tech.dokus.aura.resources.settings_appearance
import tech.dokus.aura.resources.settings_current_workspace
import tech.dokus.aura.resources.settings_profile
import tech.dokus.aura.resources.user
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.shell.HomeShellTopBarAction
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.shell.HomeShellTopBarMode
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusSizing
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.style.glassHeader
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

internal data class HomeShellProfileData(
    val fullName: String,
    val email: String,
    val tierLabel: String?,
)

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
            onAppearanceClick = onAppearanceClick,
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
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .size(sizing.iconLarge)
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.small,
            border = BorderStroke(
                width = sizing.strokeThin,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.user),
                    contentDescription = stringResource(Res.string.settings_profile),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(sizing.iconXSmall)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset.Zero.copy(y = -spacing.small),
            modifier = Modifier.widthIn(min = spacing.large * 15f)
        ) {
            ProfileMenuHeader(profileData = profileData)
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.settings_profile)) },
                onClick = {
                    expanded = false
                    onProfileClick()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.settings_appearance)) },
                onClick = {
                    expanded = false
                    onAppearanceClick()
                }
            )
            HorizontalDivider()
            DropdownMenuItem(
                enabled = !isLoggingOut,
                text = {
                    Text(
                        text = stringResource(Res.string.profile_logout),
                        color = MaterialTheme.colorScheme.statusError
                    )
                },
                onClick = {
                    expanded = false
                    onLogoutClick()
                }
            )
        }
    }
}

@Composable
private fun ProfileMenuHeader(
    profileData: HomeShellProfileData?,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    val fullName = profileData?.fullName ?: stringResource(Res.string.settings_profile)
    val email = profileData?.email ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.large, vertical = spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
    ) {
        Surface(
            modifier = Modifier.size(sizing.avatarExtraSmall),
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = sizing.strokeThin,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.user),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(sizing.iconXSmall)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing.xxSmall)
        ) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        profileData?.tierLabel?.let { tier ->
            Text(
                text = tier,
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .border(
                        width = sizing.strokeThin,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        shape = MaterialTheme.shapes.extraSmall
                    )
                    .padding(horizontal = spacing.small, vertical = spacing.xxSmall)
            )
        }
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
    topBarConfig: HomeShellTopBarConfig,
    tenantState: DokusState<Tenant>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    val spacing = MaterialTheme.dokusSpacing
    val sizing = MaterialTheme.dokusSizing
    val effects = MaterialTheme.dokusEffects
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.glassHeader)
    ) {
        // Row 1: Logo + avatar (always shown)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large, vertical = spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppNameText(modifier = Modifier.weight(1f))

            // Avatar monogram → navigates to profile
            val initials = profileData?.fullName
                ?.split(" ")
                ?.take(2)
                ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                ?.joinToString("") ?: ""
            Surface(
                modifier = Modifier
                    .size(sizing.avatarExtraSmall)
                    .clickable(onClick = onProfileClick),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.extraSmall,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // Row 2: Search/title + actions (conditional — only when topBarConfig has content)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.large)
                .padding(bottom = spacing.medium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when (val mode = topBarConfig.mode) {
                is HomeShellTopBarMode.Search -> {
                    val onExpandSearch = mode.onExpandSearch
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.small),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (!mode.isSearchExpanded && onExpandSearch != null) {
                            IconButton(
                                onClick = onExpandSearch,
                                modifier = Modifier.size(sizing.buttonHeight)
                            ) {
                                Icon(
                                    imageVector = FeatherIcons.Search,
                                    contentDescription = stringResource(Res.string.action_search)
                                )
                            }
                        }
                        AnimatedVisibility(
                            visible = mode.isSearchExpanded,
                            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                        ) {
                            PSearchFieldCompact(
                                value = mode.query,
                                onValueChange = mode.onQueryChange,
                                onClear = mode.onClear,
                                placeholder = mode.placeholder,
                                modifier = Modifier.widthIn(
                                    min = spacing.large * 7.5f,
                                    max = spacing.large * 17.5f
                                )
                            )
                        }
                    }
                }

                is HomeShellTopBarMode.Title -> {
                    Text(
                        text = mode.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            RouteTopBarActions(actions = topBarConfig.actions)
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
