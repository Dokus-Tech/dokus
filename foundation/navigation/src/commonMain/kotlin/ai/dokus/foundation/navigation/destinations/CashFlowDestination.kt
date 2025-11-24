package ai.dokus.foundation.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

sealed interface CashFlowDestination : NavigationDestination {
    @Serializable
    @SerialName("cashflow/add_document")
    data object AddDocument : HomeDestination
}