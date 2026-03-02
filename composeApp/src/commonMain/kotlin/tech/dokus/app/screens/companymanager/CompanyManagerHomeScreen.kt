package tech.dokus.app.screens.companymanager

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.app.screens.HomeScreen
import tech.dokus.app.screens.home.HomeShellProfileData
import tech.dokus.domain.model.Tenant
import tech.dokus.foundation.app.shell.HomeShellTopBarConfig
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.model.MobileTabConfig
import tech.dokus.foundation.aura.model.NavItem
import tech.dokus.foundation.aura.model.NavSection
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun CompanyManagerHomeScreen(
    navSections: List<NavSection>,
    mobileTabs: List<MobileTabConfig>,
    selectedRoute: String?,
    topBarConfig: HomeShellTopBarConfig?,
    desktopPinnedItems: List<NavItem>,
    tenantState: DokusState<Tenant>,
    profileData: HomeShellProfileData?,
    isLoggingOut: Boolean,
    snackbarHostState: SnackbarHostState,
    onWorkspaceClick: () -> Unit,
    onProfileClick: () -> Unit,
    onAppearanceClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onNavItemClick: (NavItem) -> Unit,
    onTabClick: (MobileTabConfig) -> Unit,
    onSearchShortcut: () -> Unit,
    content: @Composable () -> Unit,
) {
    HomeScreen(
        navSections = navSections,
        mobileTabs = mobileTabs,
        selectedRoute = selectedRoute,
        topBarConfig = topBarConfig,
        desktopPinnedItems = desktopPinnedItems,
        tenantState = tenantState,
        profileData = profileData,
        isLoggingOut = isLoggingOut,
        snackbarHostState = snackbarHostState,
        onWorkspaceClick = onWorkspaceClick,
        onProfileClick = onProfileClick,
        onAppearanceClick = onAppearanceClick,
        onLogoutClick = onLogoutClick,
        onNavItemClick = onNavItemClick,
        onTabClick = onTabClick,
        onSearchShortcut = onSearchShortcut,
        content = content,
    )
}

@Preview(name = "Company Manager Home Desktop", widthDp = 1366, heightDp = 900)
@Composable
private fun CompanyManagerHomeScreenDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        CompanyManagerHomeScreen(
            navSections = emptyList(),
            mobileTabs = emptyList(),
            selectedRoute = null,
            topBarConfig = null,
            desktopPinnedItems = emptyList(),
            tenantState = DokusState.loading(),
            profileData = HomeShellProfileData(
                fullName = "Karel Boonen",
                email = "karel@example.com",
                tierLabel = null,
            ),
            isLoggingOut = false,
            snackbarHostState = remember { SnackbarHostState() },
            onWorkspaceClick = {},
            onProfileClick = {},
            onAppearanceClick = {},
            onLogoutClick = {},
            onNavItemClick = {},
            onTabClick = {},
            onSearchShortcut = {},
            content = {},
        )
    }
}
