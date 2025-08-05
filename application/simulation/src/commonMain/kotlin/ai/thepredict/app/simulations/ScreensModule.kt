package ai.thepredict.app.simulations

import ai.thepredict.app.simulations.screen.SimulationScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val simulationScreensModule = screenModule {
    register<HomeTabsNavigation.Simulations> {
        SimulationScreen()
    }
}