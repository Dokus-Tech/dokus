package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface CoreDestination : NavigationDestination {
    @Serializable
    @SerialName("splash")
    data object Splash : CoreDestination

    @Serializable
    @SerialName("home")
    data object Home : CoreDestination

    @Serializable
    @SerialName("update_required")
    data object UpdateRequired : CoreDestination
}