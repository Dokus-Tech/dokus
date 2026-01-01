package tech.dokus.features.cashflow.presentation.cashflow.model.usecase

import tech.dokus.features.cashflow.presentation.cashflow.components.VatSummaryData
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.domain.exceptions.DokusException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Use case for loading VAT summary data.
 *
 * TODO: Replace placeholder implementation with actual API call
 * when the VAT summary endpoint becomes available.
 */
class LoadVatSummaryUseCase {

    /**
     * Load VAT summary data.
     *
     * @return Flow emitting loading state then success/error with VAT data.
     */
    operator fun invoke(): Flow<DokusState<VatSummaryData>> = flow {
        emit(DokusState.loading())
        try {
            // TODO: Replace with actual API call when endpoint is available
            val data = VatSummaryData.empty
            emit(DokusState.success(data))
        } catch (e: Exception) {
            emit(DokusState.error(DokusException.Unknown(e)) { /* No-op: placeholder implementation */ })
        }
    }
}
