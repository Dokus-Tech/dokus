package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface HomeDestination : NavigationDestination {

    @Serializable
    @SerialName("dashboard")
    data object Dashboard : HomeDestination

    @Serializable
    @SerialName("documents")
    data object Documents : HomeDestination

    @Serializable
    @SerialName("cashflow")
    data object Cashflow : HomeDestination

    @Serializable
    @SerialName("contacts")
    data object Contacts : HomeDestination

    @Serializable
    @SerialName("team")
    data object Team : HomeDestination

    @Serializable
    @SerialName("ai-chat")
    data object AiChat : HomeDestination

    @Serializable
    @SerialName("settings")
    data object Settings : HomeDestination

    @Serializable
    @SerialName("more")
    data object More : HomeDestination

    @Serializable
    @SerialName("home/under_development")
    data object UnderDevelopment : HomeDestination
}
