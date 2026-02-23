package tech.dokus.app.navigation

import tech.dokus.navigation.destinations.AppDestination
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.ContactsDestination
import tech.dokus.navigation.destinations.CoreDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.destinations.NavigationDestination
import tech.dokus.navigation.destinations.SettingsDestination
import kotlin.reflect.KClass

internal enum class NavHostOwner {
    Root,
    Home,
}

internal object NavigationOwnershipPolicy {
    val rootDestinationFamilies: Set<KClass<out NavigationDestination>> = setOf(
        CoreDestination::class,
        AppDestination::class,
        AuthDestination::class,
        SettingsDestination::class,
        CashFlowDestination::class,
        ContactsDestination::class,
    )

    val homeDestinationFamilies: Set<KClass<out NavigationDestination>> = setOf(
        HomeDestination::class,
    )

    fun ownerFor(destination: NavigationDestination): NavHostOwner {
        return when (destination) {
            is CoreDestination,
            is AppDestination,
            is AuthDestination,
            is SettingsDestination,
            is CashFlowDestination,
            is ContactsDestination -> NavHostOwner.Root

            is HomeDestination -> NavHostOwner.Home
        }
    }
}
