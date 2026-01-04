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
import tech.dokus.features.cashflow.gateway.PeppolGateway
import tech.dokus.features.cashflow.usecases.ConnectPeppolUseCase
import tech.dokus.features.cashflow.usecases.DeletePeppolSettingsUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolSettingsUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolTransmissionForInvoiceUseCase
import tech.dokus.features.cashflow.usecases.ListPeppolTransmissionsUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolInboxUseCase
import tech.dokus.features.cashflow.usecases.SendInvoiceViaPeppolUseCase
import tech.dokus.features.cashflow.usecases.ValidateInvoiceForPeppolUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolRecipientUseCase

internal class ConnectPeppolUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : ConnectPeppolUseCase {
    override suspend fun invoke(request: PeppolConnectRequest): Result<PeppolConnectResponse> {
        return peppolGateway.connectPeppol(request)
    }
}

internal class GetPeppolSettingsUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : GetPeppolSettingsUseCase {
    override suspend fun invoke(): Result<PeppolSettingsDto?> {
        return peppolGateway.getPeppolSettings()
    }
}

internal class DeletePeppolSettingsUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : DeletePeppolSettingsUseCase {
    override suspend fun invoke(): Result<Unit> {
        return peppolGateway.deletePeppolSettings()
    }
}

internal class ListPeppolTransmissionsUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : ListPeppolTransmissionsUseCase {
    override suspend fun invoke(
        direction: PeppolTransmissionDirection?,
        status: PeppolStatus?,
        limit: Int,
        offset: Int
    ): Result<List<PeppolTransmissionDto>> {
        return peppolGateway.listPeppolTransmissions(
            direction = direction,
            status = status,
            limit = limit,
            offset = offset
        )
    }
}

internal class VerifyPeppolRecipientUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : VerifyPeppolRecipientUseCase {
    override suspend fun invoke(peppolId: String): Result<PeppolVerifyResponse> {
        return peppolGateway.verifyPeppolRecipient(peppolId)
    }
}

internal class ValidateInvoiceForPeppolUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : ValidateInvoiceForPeppolUseCase {
    override suspend fun invoke(invoiceId: InvoiceId): Result<PeppolValidationResult> {
        return peppolGateway.validateInvoiceForPeppol(invoiceId)
    }
}

internal class SendInvoiceViaPeppolUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : SendInvoiceViaPeppolUseCase {
    override suspend fun invoke(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse> {
        return peppolGateway.sendInvoiceViaPeppol(invoiceId)
    }
}

internal class PollPeppolInboxUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : PollPeppolInboxUseCase {
    override suspend fun invoke(): Result<PeppolInboxPollResponse> {
        return peppolGateway.pollPeppolInbox()
    }
}

internal class GetPeppolTransmissionForInvoiceUseCaseImpl(
    private val peppolGateway: PeppolGateway
) : GetPeppolTransmissionForInvoiceUseCase {
    override suspend fun invoke(invoiceId: InvoiceId): Result<PeppolTransmissionDto?> {
        return peppolGateway.getPeppolTransmissionForInvoice(invoiceId)
    }
}
