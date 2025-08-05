package ai.thepredict.app.inventory

import ai.thepredict.app.inventory.screen.InventoryScreen
import ai.thepredict.app.navigation.HomeTabsNavigation
import cafe.adriel.voyager.core.registry.screenModule

val inventoryScreensModule = screenModule {
    register<HomeTabsNavigation.Items> {
        InventoryScreen()
    }
}