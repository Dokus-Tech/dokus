package ai.thepredict.app.banking

import ai.thepredict.app.banking.screen.BankingScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val bankingScreensModule = screenModule {
    register<HomeTabsNavigation.Banking> {
        BankingScreen()
    }
}