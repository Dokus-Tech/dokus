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
 * Use case for Peppol operations.
 */
interface PeppolUseCase {
    suspend fun connectPeppol(request: PeppolConnectRequest): Result<PeppolConnectResponse>

    suspend fun getPeppolSettings(): Result<PeppolSettingsDto?>

    suspend fun deletePeppolSettings(): Result<Unit>

    suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection? = null,
        status: PeppolStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PeppolTransmissionDto>>

    suspend fun verifyPeppolRecipient(peppolId: String): Result<PeppolVerifyResponse>

    suspend fun validateInvoiceForPeppol(invoiceId: InvoiceId): Result<PeppolValidationResult>

    suspend fun sendInvoiceViaPeppol(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse>

    suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse>

    suspend fun getPeppolTransmissionForInvoice(
        invoiceId: InvoiceId
    ): Result<PeppolTransmissionDto?>
}
