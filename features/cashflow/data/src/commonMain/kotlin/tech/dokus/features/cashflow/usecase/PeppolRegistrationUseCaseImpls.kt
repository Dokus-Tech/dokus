package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolActivityDto
import tech.dokus.domain.model.PeppolIdVerificationResult
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.PeppolRegistrationResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.EnablePeppolSendingOnlyUseCase
import tech.dokus.features.cashflow.usecases.EnablePeppolUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolActivityUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolRegistrationUseCase
import tech.dokus.features.cashflow.usecases.OptOutPeppolUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolTransferUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolIdUseCase
import tech.dokus.features.cashflow.usecases.WaitForPeppolTransferUseCase

/**
 * Implementation of [GetPeppolRegistrationUseCase].
 */
internal class GetPeppolRegistrationUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : GetPeppolRegistrationUseCase {
    override suspend fun invoke(): Result<PeppolRegistrationDto?> {
        return remoteDataSource.getPeppolRegistration()
    }
}

/**
 * Implementation of [VerifyPeppolIdUseCase].
 */
internal class VerifyPeppolIdUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : VerifyPeppolIdUseCase {
    override suspend fun invoke(vatNumber: VatNumber): Result<PeppolIdVerificationResult> {
        require(vatNumber.isValid) { "Invalid VAT number" }
        return remoteDataSource.verifyPeppolId(vatNumber)
    }
}

/**
 * Implementation of [EnablePeppolUseCase].
 */
internal class EnablePeppolUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : EnablePeppolUseCase {
    override suspend fun invoke(): Result<PeppolRegistrationResponse> {
        return remoteDataSource.enablePeppol()
    }
}

/**
 * Implementation of [EnablePeppolSendingOnlyUseCase].
 */
internal class EnablePeppolSendingOnlyUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : EnablePeppolSendingOnlyUseCase {
    override suspend fun invoke(): Result<PeppolRegistrationResponse> {
        return remoteDataSource.enablePeppolSendingOnly()
    }
}

/**
 * Implementation of [WaitForPeppolTransferUseCase].
 */
internal class WaitForPeppolTransferUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : WaitForPeppolTransferUseCase {
    override suspend fun invoke(): Result<PeppolRegistrationResponse> {
        return remoteDataSource.waitForPeppolTransfer()
    }
}

/**
 * Implementation of [OptOutPeppolUseCase].
 */
internal class OptOutPeppolUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : OptOutPeppolUseCase {
    override suspend fun invoke(): Result<Unit> {
        return remoteDataSource.optOutPeppol()
    }
}

/**
 * Implementation of [PollPeppolTransferUseCase].
 */
internal class PollPeppolTransferUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : PollPeppolTransferUseCase {
    override suspend fun invoke(): Result<PeppolRegistrationResponse> {
        return remoteDataSource.pollPeppolTransfer()
    }
}

/**
 * Implementation of [GetPeppolActivityUseCase].
 *
 * Fetches the most recent successful INBOUND and OUTBOUND transmissions
 * and returns their timestamps.
 */
internal class GetPeppolActivityUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : GetPeppolActivityUseCase {
    override suspend fun invoke(): Result<PeppolActivityDto?> = runCatching {
        // Get most recent inbound transmission (Delivered = successfully received)
        val inboundResult = remoteDataSource.listPeppolTransmissions(
            direction = PeppolTransmissionDirection.Inbound,
            status = PeppolStatus.Delivered,
            limit = 1,
            offset = 0
        )

        // Get most recent outbound transmission (Delivered = successfully sent)
        val outboundResult = remoteDataSource.listPeppolTransmissions(
            direction = PeppolTransmissionDirection.Outbound,
            status = PeppolStatus.Delivered,
            limit = 1,
            offset = 0
        )

        val lastInboundAt = inboundResult.getOrNull()?.firstOrNull()?.transmittedAt
        val lastOutboundAt = outboundResult.getOrNull()?.firstOrNull()?.transmittedAt

        // Return null if no activity at all
        if (lastInboundAt == null && lastOutboundAt == null) {
            null
        } else {
            PeppolActivityDto(
                lastInboundAt = lastInboundAt,
                lastOutboundAt = lastOutboundAt
            )
        }
    }
}
