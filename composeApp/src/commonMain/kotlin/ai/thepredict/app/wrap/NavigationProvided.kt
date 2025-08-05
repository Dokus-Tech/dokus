package ai.thepredict.app.wrap

import ai.thepredict.app.banking.bankingScreensModule
import ai.thepredict.app.cashflow.cashflowScreensModule
import ai.thepredict.app.contacts.contactsScreensModule
import ai.thepredict.app.dashboard.dashboardScreensModule
import ai.thepredict.app.home.homeScreensModule
import ai.thepredict.app.onboarding.onboardingScreensModule
import ai.thepredict.app.home.splash.SplashScreen
import ai.thepredict.app.inventory.inventoryScreensModule
import ai.thepredict.app.simulations.simulationScreensModule
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.registry.ScreenRegistry
import cafe.adriel.voyager.jetpack.ProvideNavigatorLifecycleKMPSupport
import cafe.adriel.voyager.navigator.Navigator

@OptIn(ExperimentalVoyagerApi::class)
@Composable
fun NavigationProvided(content: @Composable () -> Unit) {
    ScreenRegistry {
        onboardingScreensModule()
        homeScreensModule()
        dashboardScreensModule()
        contactsScreensModule()
        cashflowScreensModule()
        simulationScreensModule()
        inventoryScreensModule()
        bankingScreensModule()
    }

    ProvideNavigatorLifecycleKMPSupport {
        Navigator(SplashScreen()) {
            content()
        }
    }
}