package tech.dokus.navigation.destinations

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root-level navigation destinations for banking screens.
 * Used when navigating from the "More" menu on mobile.
 */
sealed interface BankingDestination : NavigationDestination {

    @Serializable
    @SerialName("root/banking/balances")
    data object Balances : BankingDestination

    @Serializable
    @SerialName("root/banking/payments")
    data object Payments : BankingDestination

    /**
     * Dialog for selecting a reason when ignoring a bank transaction.
     */
    @Serializable
    @SerialName("banking/dialog/ignore_reason")
    data class IgnoreReasonDialog(val transactionId: String) : BankingDestination
}
