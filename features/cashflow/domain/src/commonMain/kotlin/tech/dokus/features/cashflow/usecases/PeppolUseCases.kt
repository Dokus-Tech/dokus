package tech.dokus.features.cashflow.usecases

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

/**
 * Use case for initiating a Peppol provider connection.
 */
interface ConnectPeppolUseCase {
    suspend operator fun invoke(request: PeppolConnectRequest): Result<PeppolConnectResponse>
}

/**
 * Use case for retrieving Peppol settings.
 */
interface GetPeppolSettingsUseCase {
    suspend operator fun invoke(): Result<PeppolSettingsDto?>
}

/**
 * Use case for deleting Peppol settings.
 */
interface DeletePeppolSettingsUseCase {
    suspend operator fun invoke(): Result<Unit>
}

/**
 * Use case for listing Peppol transmissions.
 */
interface ListPeppolTransmissionsUseCase {
    suspend operator fun invoke(
        direction: PeppolTransmissionDirection? = null,
        status: PeppolStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PeppolTransmissionDto>>
}

/**
 * Use case for verifying a Peppol recipient.
 */
interface VerifyPeppolRecipientUseCase {
    suspend operator fun invoke(peppolId: String): Result<PeppolVerifyResponse>
}

/**
 * Use case for validating an invoice for Peppol.
 */
interface ValidateInvoiceForPeppolUseCase {
    suspend operator fun invoke(invoiceId: InvoiceId): Result<PeppolValidationResult>
}

/**
 * Use case for sending an invoice via Peppol.
 */
interface SendInvoiceViaPeppolUseCase {
    suspend operator fun invoke(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse>
}

/**
 * Use case for polling the Peppol inbox.
 */
interface PollPeppolInboxUseCase {
    suspend operator fun invoke(): Result<PeppolInboxPollResponse>
}

/**
 * Use case for retrieving a Peppol transmission for a specific invoice.
 */
interface GetPeppolTransmissionForInvoiceUseCase {
    suspend operator fun invoke(invoiceId: InvoiceId): Result<PeppolTransmissionDto?>
}
