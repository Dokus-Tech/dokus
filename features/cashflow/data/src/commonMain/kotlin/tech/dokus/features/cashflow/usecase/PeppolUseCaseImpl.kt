package tech.dokus.features.cashflow.usecase

import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolVerifyResponse
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import tech.dokus.features.cashflow.usecases.PeppolUseCase

internal class PeppolUseCaseImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolUseCase {
    override suspend fun connectPeppol(
        request: PeppolConnectRequest
    ): Result<PeppolConnectResponse> {
        return cashflowRemoteDataSource.connectPeppol(request)
    }

    override suspend fun getPeppolSettings(): Result<PeppolSettingsDto?> {
        return cashflowRemoteDataSource.getPeppolSettings()
    }

    override suspend fun deletePeppolSettings(): Result<Unit> {
        return cashflowRemoteDataSource.deletePeppolSettings()
    }

    override suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection?,
        status: PeppolStatus?,
        limit: Int,
        offset: Int
    ): Result<List<PeppolTransmissionDto>> {
        return cashflowRemoteDataSource.listPeppolTransmissions(
            direction = direction,
            status = status,
            limit = limit,
            offset = offset
        )
    }

    override suspend fun verifyPeppolRecipient(
        peppolId: String
    ): Result<PeppolVerifyResponse> {
        return cashflowRemoteDataSource.verifyPeppolRecipient(peppolId)
    }

    override suspend fun validateInvoiceForPeppol(
        invoiceId: InvoiceId
    ): Result<PeppolValidationResult> {
        return cashflowRemoteDataSource.validateInvoiceForPeppol(invoiceId)
    }

    override suspend fun sendInvoiceViaPeppol(
        invoiceId: InvoiceId
    ): Result<SendInvoiceViaPeppolResponse> {
        return cashflowRemoteDataSource.sendInvoiceViaPeppol(invoiceId)
    }

    override suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse> {
        return cashflowRemoteDataSource.pollPeppolInbox()
    }

    override suspend fun getPeppolTransmissionForInvoice(
        invoiceId: InvoiceId
    ): Result<PeppolTransmissionDto?> {
        return cashflowRemoteDataSource.getPeppolTransmissionForInvoice(invoiceId)
    }
}
