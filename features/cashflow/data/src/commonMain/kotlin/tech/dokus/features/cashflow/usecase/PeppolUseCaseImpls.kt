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
import tech.dokus.features.cashflow.gateway.PeppolConnectionGateway
import tech.dokus.features.cashflow.gateway.PeppolInboxGateway
import tech.dokus.features.cashflow.gateway.PeppolInvoiceGateway
import tech.dokus.features.cashflow.gateway.PeppolRecipientGateway
import tech.dokus.features.cashflow.gateway.PeppolTransmissionsGateway
import tech.dokus.features.cashflow.usecases.ConnectPeppolUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolSettingsUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolTransmissionForInvoiceUseCase
import tech.dokus.features.cashflow.usecases.ListPeppolTransmissionsUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolInboxUseCase
import tech.dokus.features.cashflow.usecases.SendInvoiceViaPeppolUseCase
import tech.dokus.features.cashflow.usecases.ValidateInvoiceForPeppolUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolRecipientUseCase

internal class ConnectPeppolUseCaseImpl(
    private val peppolConnectionGateway: PeppolConnectionGateway
) : ConnectPeppolUseCase {
    override suspend fun invoke(request: PeppolConnectRequest): Result<PeppolConnectResponse> {
        return peppolConnectionGateway.connectPeppol(request)
    }
}

internal class GetPeppolSettingsUseCaseImpl(
    private val peppolConnectionGateway: PeppolConnectionGateway
) : GetPeppolSettingsUseCase {
    override suspend fun invoke(): Result<PeppolSettingsDto?> {
        return peppolConnectionGateway.getPeppolSettings()
    }
}

internal class ListPeppolTransmissionsUseCaseImpl(
    private val peppolTransmissionsGateway: PeppolTransmissionsGateway
) : ListPeppolTransmissionsUseCase {
    override suspend fun invoke(
        direction: PeppolTransmissionDirection?,
        status: PeppolStatus?,
        limit: Int,
        offset: Int
    ): Result<List<PeppolTransmissionDto>> {
        return peppolTransmissionsGateway.listPeppolTransmissions(
            direction = direction,
            status = status,
            limit = limit,
            offset = offset
        )
    }
}

internal class VerifyPeppolRecipientUseCaseImpl(
    private val peppolRecipientGateway: PeppolRecipientGateway
) : VerifyPeppolRecipientUseCase {
    override suspend fun invoke(peppolId: String): Result<PeppolVerifyResponse> {
        return peppolRecipientGateway.verifyPeppolRecipient(peppolId)
    }
}

internal class ValidateInvoiceForPeppolUseCaseImpl(
    private val peppolInvoiceGateway: PeppolInvoiceGateway
) : ValidateInvoiceForPeppolUseCase {
    override suspend fun invoke(invoiceId: InvoiceId): Result<PeppolValidationResult> {
        return peppolInvoiceGateway.validateInvoiceForPeppol(invoiceId)
    }
}

internal class SendInvoiceViaPeppolUseCaseImpl(
    private val peppolInvoiceGateway: PeppolInvoiceGateway
) : SendInvoiceViaPeppolUseCase {
    override suspend fun invoke(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse> {
        return peppolInvoiceGateway.sendInvoiceViaPeppol(invoiceId)
    }
}

internal class PollPeppolInboxUseCaseImpl(
    private val peppolInboxGateway: PeppolInboxGateway
) : PollPeppolInboxUseCase {
    override suspend fun invoke(): Result<PeppolInboxPollResponse> {
        return peppolInboxGateway.pollPeppolInbox()
    }
}

internal class GetPeppolTransmissionForInvoiceUseCaseImpl(
    private val peppolInvoiceGateway: PeppolInvoiceGateway
) : GetPeppolTransmissionForInvoiceUseCase {
    override suspend fun invoke(invoiceId: InvoiceId): Result<PeppolTransmissionDto?> {
        return peppolInvoiceGateway.getPeppolTransmissionForInvoice(invoiceId)
    }
}
