package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse

/**
 * Gateway for Peppol invoice-specific operations.
 */
interface PeppolInvoiceGateway {
    suspend fun validateInvoiceForPeppol(invoiceId: InvoiceId): Result<PeppolValidationResult>

    suspend fun sendInvoiceViaPeppol(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse>

    suspend fun getPeppolTransmissionForInvoice(
        invoiceId: InvoiceId
    ): Result<PeppolTransmissionDto?>
}
