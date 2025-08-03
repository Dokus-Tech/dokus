package ai.thepredict.app.dashboard

import ai.thepredict.app.dashboard.screen.DashboardScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val dashboardScreensModule = screenModule {
    register<HomeTabsNavigation.Dashboard> {
        DashboardScreen()
    }
}