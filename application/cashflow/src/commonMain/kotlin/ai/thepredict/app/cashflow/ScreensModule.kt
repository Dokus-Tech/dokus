package ai.thepredict.app.cashflow

import ai.thepredict.app.cashflow.screen.CashflowScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val cashflowScreensModule = screenModule {
    register<HomeTabsNavigation.Cashflow> {
        CashflowScreen()
    }
}