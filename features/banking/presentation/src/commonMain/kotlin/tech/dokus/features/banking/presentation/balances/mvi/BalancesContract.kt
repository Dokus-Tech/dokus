package tech.dokus.features.banking.presentation.balances.mvi

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_tab_7d
import tech.dokus.aura.resources.banking_balances_tab_30d
import tech.dokus.aura.resources.banking_balances_tab_90d
import tech.dokus.aura.resources.banking_balances_tab_1y
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountSummary
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankTransactionSummary
import tech.dokus.foundation.app.state.DokusState

@Immutable
enum class BalanceTimeRange(val id: String, val days: Int, val labelRes: StringResource) {
    SevenDays("7d", 7, Res.string.banking_balances_tab_7d),
    ThirtyDays("30d", 30, Res.string.banking_balances_tab_30d),
    NinetyDays("90d", 90, Res.string.banking_balances_tab_90d),
    OneYear("1y", 365, Res.string.banking_balances_tab_1y),
}

@Immutable
data class BalancesState(
    val accounts: DokusState<List<BankAccountDto>>,
    val summary: DokusState<BankAccountSummary>,
    val transactionSummary: DokusState<BankTransactionSummary>,
    val balanceHistory: DokusState<BalanceHistoryResponse>,
    val timeRange: BalanceTimeRange = BalanceTimeRange.ThirtyDays,
) : MVIState {
    companion object {
        val initial = BalancesState(
            accounts = DokusState.loading(),
            summary = DokusState.loading(),
            transactionSummary = DokusState.loading(),
            balanceHistory = DokusState.loading(),
        )
    }
}

@Immutable
sealed interface BalancesIntent : MVIIntent {
    data object Refresh : BalancesIntent
    data class SetTimeRange(val range: BalanceTimeRange) : BalancesIntent
}

@Immutable
sealed interface BalancesAction : MVIAction {
    data class ShowError(val error: DokusException) : BalancesAction
}
