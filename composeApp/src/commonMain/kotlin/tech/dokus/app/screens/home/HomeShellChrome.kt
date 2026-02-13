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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.profile_logout
import tech.dokus.aura.resources.search_placeholder
import tech.dokus.aura.resources.settings_appearance
import tech.dokus.aura.resources.settings_profile
import tech.dokus.aura.resources.settings_select_workspace
import tech.dokus.aura.resources.user
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact
import tech.dokus.foundation.aura.components.common.PTopAppBarSearchAction
import tech.dokus.foundation.aura.components.common.ShimmerBox
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.style.brandGold
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textMuted

internal data class HomeShellProfileData(
    val fullName: String,
    val email: String,
    val tierLabel: String?,
)

internal fun tierBadgeLabel(tier: SubscriptionTier?): String? = when (tier) {
    SubscriptionTier.CoreFounder -> "Founder"
    SubscriptionTier.Core -> "Core"
    SubscriptionTier.One -> "One"
    SubscriptionTier.SelfHosted -> "Self-hosted"
    null -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DesktopShellSearchTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    PTopAppBarSearchAction(
        searchContent = {
            PSearchFieldCompact(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = stringResource(Res.string.search_placeholder),
                modifier = Modifier.widthIn(min = 220.dp, max = 360.dp)
            )
        },
        actions = {},
        modifier = modifier
    )
}

@Composable
internal fun DesktopSidebarBottomControls(
    tenantState: DokusState<Tenant?>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
    tenantState: DokusState<Tenant?>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tenant = (tenantState as? DokusState.Success<Tenant?>)?.data
    val workspaceName = tenant?.displayName?.value ?: stringResource(Res.string.settings_select_workspace)
    val workspaceVat = tenant?.vatNumber?.formatted
    val initial = tenant?.displayName?.value?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val isLoading = tenantState is DokusState.Loading

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .hoverable(interactionSource = interactionSource)
            .background(
                if (isHovered) MaterialTheme.colorScheme.surfaceHover else MaterialTheme.colorScheme.surface
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            ShimmerBox(
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShimmerLine(
                    modifier = Modifier.fillMaxWidth(0.65f),
                    height = 12.dp
                )
                ShimmerLine(
                    modifier = Modifier.fillMaxWidth(0.45f),
                    height = 10.dp
                )
            }
        } else {
            CompanyAvatarImage(
                avatarUrl = tenant?.avatar?.small,
                initial = initial,
                size = AvatarSize.ExtraSmall,
                shape = AvatarShape.RoundedSquare
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = workspaceName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                workspaceVat?.let { vat ->
                    Text(
                        text = vat,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
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
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            modifier = Modifier
                .size(30.dp)
                .clickable { expanded = true },
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(6.dp),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.user),
                    contentDescription = stringResource(Res.string.settings_profile),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = 0.dp, y = (-8).dp),
            modifier = Modifier.widthIn(min = 240.dp)
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
    val fullName = profileData?.fullName ?: stringResource(Res.string.settings_profile)
    val email = profileData?.email ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(4.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
            )
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(Res.drawable.user),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = fullName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (email.isNotBlank()) {
                Text(
                    text = email,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        profileData?.tierLabel?.let { tier ->
            Text(
                text = tier,
                color = MaterialTheme.colorScheme.brandGold,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.brandGold.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileShellTopBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    onExpandSearch: () -> Unit,
    tenantState: DokusState<Tenant?>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    var showProfileSheet by rememberSaveable { mutableStateOf(false) }

    PTopAppBarSearchAction(
        searchContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isSearchExpanded) {
                    IconButton(
                        onClick = onExpandSearch,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = FeatherIcons.Search,
                            contentDescription = stringResource(Res.string.action_search)
                        )
                    }
                }
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
                ) {
                    PSearchFieldCompact(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        placeholder = stringResource(Res.string.search_placeholder),
                        modifier = Modifier.widthIn(min = 120.dp, max = 220.dp)
                    )
                }
            }
        },
        actions = {
            MobileWorkspaceBadge(
                tenantState = tenantState,
                onClick = onWorkspaceClick
            )
            Spacer(modifier = Modifier.width(8.dp))
            ProfileIconButton { showProfileSheet = true }
        }
    )

    if (showProfileSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showProfileSheet = false },
            sheetState = sheetState,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 8.dp)
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f))
                )
            }
        ) {
            ProfileMenuHeader(profileData = profileData)
            HorizontalDivider()
            MobileMenuItem(
                label = stringResource(Res.string.settings_profile),
                onClick = {
                    showProfileSheet = false
                    onProfileClick()
                }
            )
            MobileMenuItem(
                label = stringResource(Res.string.settings_appearance),
                onClick = {
                    showProfileSheet = false
                    onAppearanceClick()
                }
            )
            HorizontalDivider()
            MobileMenuItem(
                label = stringResource(Res.string.profile_logout),
                color = MaterialTheme.colorScheme.statusError,
                enabled = !isLoggingOut,
                onClick = {
                    showProfileSheet = false
                    onLogoutClick()
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RowScope.MobileWorkspaceBadge(
    tenantState: DokusState<Tenant?>,
    onClick: () -> Unit,
) {
    val tenant = (tenantState as? DokusState.Success<Tenant?>)?.data
    val name = tenant?.displayName?.value ?: stringResource(Res.string.settings_select_workspace)
    val initial = tenant?.displayName?.value?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "D"
    val isLoading = tenantState is DokusState.Loading

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (isLoading) {
            ShimmerBox(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(3.dp))
            )
            ShimmerLine(
                modifier = Modifier.width(90.dp),
                height = 12.dp
            )
        } else {
            CompanyAvatarImage(
                avatarUrl = tenant?.avatar?.small,
                initial = initial,
                size = AvatarSize.ExtraSmall,
                shape = AvatarShape.RoundedSquare
            )
            Text(
                text = name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 140.dp)
            )
        }
    }
}

@Composable
private fun ProfileIconButton(
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(30.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(Res.drawable.user),
                contentDescription = stringResource(Res.string.settings_profile),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun MobileMenuItem(
    label: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) color else color.copy(alpha = 0.45f)
        )
    }
}
