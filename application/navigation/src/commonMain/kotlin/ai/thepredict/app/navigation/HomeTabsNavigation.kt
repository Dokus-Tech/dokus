package ai.thepredict.app.navigation

import cafe.adriel.voyager.core.screen.ScreenKey

sealed interface HomeTabsNavigation {
    val screenKey: ScreenKey

    data object Dashboard : HomeTabsNavigation {
        override val screenKey: ScreenKey = "Dashboard"
    }

    data object Contacts : HomeTabsNavigation {
        override val screenKey: ScreenKey = "Contacts"
    }

    data object Items : HomeTabsNavigation {
        override val screenKey: ScreenKey = "Items"
    }

    data object Banking : HomeTabsNavigation {
        override val screenKey: ScreenKey = "Banking"
    }

    data object AddDocuments : HomeTabsNavigation {
        override val screenKey: ScreenKey = "AddDocuments"
    }
}