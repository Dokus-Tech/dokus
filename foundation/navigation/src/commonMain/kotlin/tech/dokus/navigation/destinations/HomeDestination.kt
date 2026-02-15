package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface HomeDestination : NavigationDestination {

    @Serializable
    @SerialName("today")
    data object Today : HomeDestination

    @Serializable
    @SerialName("tomorrow")
    data object Tomorrow : HomeDestination

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

/** Route string matching the @SerialName value for backstack matching. */
val HomeDestination.route: String get() = when (this) {
    HomeDestination.Today -> "today"
    HomeDestination.Tomorrow -> "tomorrow"
    HomeDestination.Documents -> "documents"
    HomeDestination.Cashflow -> "cashflow"
    HomeDestination.Contacts -> "contacts"
    HomeDestination.Team -> "team"
    HomeDestination.AiChat -> "ai-chat"
    HomeDestination.Settings -> "settings"
    HomeDestination.More -> "more"
    HomeDestination.UnderDevelopment -> "home/under_development"
}
