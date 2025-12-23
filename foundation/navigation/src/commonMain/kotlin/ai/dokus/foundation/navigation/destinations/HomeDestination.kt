package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface HomeDestination : NavigationDestination {

    @Serializable
    @SerialName("dashboard")
    data object Dashboard : HomeDestination

    @Serializable
    @SerialName("cashflow")
    data object Cashflow : HomeDestination

    @Serializable
    @SerialName("contacts")
    data object Contacts : HomeDestination

    @Serializable
    @SerialName("settings")
    data object Settings : HomeDestination

    @Serializable
    @SerialName("home/under_development")
    data object UnderDevelopment : HomeDestination
}
