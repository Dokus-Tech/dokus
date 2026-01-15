package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.model.PeppolIdVerificationResult
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.PeppolRegistrationResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.EnablePeppolUseCase
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
    override suspend fun invoke(peppolId: String): Result<PeppolIdVerificationResult> {
        require(peppolId.isNotBlank()) { "PEPPOL ID must not be blank" }
        return remoteDataSource.verifyPeppolId(peppolId)
    }
}

/**
 * Implementation of [EnablePeppolUseCase].
 */
internal class EnablePeppolUseCaseImpl(
    private val remoteDataSource: CashflowRemoteDataSource
) : EnablePeppolUseCase {
    override suspend fun invoke(enterpriseNumber: String): Result<PeppolRegistrationResponse> {
        require(enterpriseNumber.isNotBlank()) { "Enterprise number must not be blank" }
        return remoteDataSource.enablePeppol(enterpriseNumber)
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
