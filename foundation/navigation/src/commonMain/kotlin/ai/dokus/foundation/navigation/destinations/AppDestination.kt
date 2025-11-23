package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface AppDestination : NavigationDestination {
    @Serializable
    @SerialName("notifications")
    data object Notifications : AppDestination

    @Serializable
    @SerialName("app/under_development")
    data object UnderDevelopment : AppDestination

    @Serializable
    @SerialName("empty")
    data object Empty : AppDestination
}