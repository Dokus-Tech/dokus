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
    @SerialName("media")
    data object Media : HomeDestination

    @Serializable
    @SerialName("users")
    data object Users : HomeDestination

    @Serializable
    @SerialName("operation_orders")
    data object OperationOrders : HomeDestination

    @Serializable
    @SerialName("operation_communications")
    data object OperationCommunications : HomeDestination

    @Serializable
    @SerialName("settings")
    data object Settings : HomeDestination

    @Serializable
    @SerialName("home/under_development")
    data object UnderDevelopment : HomeDestination
}
