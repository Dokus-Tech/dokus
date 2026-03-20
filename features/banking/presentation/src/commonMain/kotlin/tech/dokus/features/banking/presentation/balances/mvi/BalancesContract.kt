package tech.dokus.features.banking.presentation.balances.mvi

import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.StringResource
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_balances_tab_1y
import tech.dokus.aura.resources.banking_balances_tab_30d
import tech.dokus.aura.resources.banking_balances_tab_7d
import tech.dokus.aura.resources.banking_balances_tab_90d
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.BalanceHistoryResponse
import tech.dokus.domain.model.BankAccountDto
import tech.dokus.domain.model.BankAccountSummaryDto
import tech.dokus.domain.model.BankTransactionSummaryDto
import tech.dokus.foundation.app.state.DokusState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

@Immutable
enum class BalanceTimeRange(val id: String, val timeframe: Duration, val labelRes: StringResource) {
    SevenDays("7d", 7.days, Res.string.banking_balances_tab_7d),
    ThirtyDays("30d", 30.days, Res.string.banking_balances_tab_30d),
    NinetyDays("90d", 90.days, Res.string.banking_balances_tab_90d),
    OneYear("1y", 1.days, Res.string.banking_balances_tab_1y),
}

@Immutable
data class BalancesState(
    val accounts: DokusState<List<BankAccountDto>>,
    val summary: DokusState<BankAccountSummaryDto>,
    val transactionSummary: DokusState<BankTransactionSummaryDto>,
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
    data object UploadStatement : BalancesIntent
    data object ConnectAccount : BalancesIntent
}

@Immutable
sealed interface BalancesAction : MVIAction {
    data class ShowError(val error: DokusException) : BalancesAction
}
