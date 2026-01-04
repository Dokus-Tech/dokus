package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolGateway {
    override suspend fun connectPeppol(
        request: PeppolConnectRequest
    ) = cashflowRemoteDataSource.connectPeppol(request)

    override suspend fun getPeppolSettings() = cashflowRemoteDataSource.getPeppolSettings()

    override suspend fun deletePeppolSettings() = cashflowRemoteDataSource.deletePeppolSettings()

    override suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection?,
        status: PeppolStatus?,
        limit: Int,
        offset: Int
    ) = cashflowRemoteDataSource.listPeppolTransmissions(
        direction = direction,
        status = status,
        limit = limit,
        offset = offset
    )

    override suspend fun verifyPeppolRecipient(
        peppolId: String
    ) = cashflowRemoteDataSource.verifyPeppolRecipient(peppolId)

    override suspend fun validateInvoiceForPeppol(
        invoiceId: InvoiceId
    ) = cashflowRemoteDataSource.validateInvoiceForPeppol(invoiceId)

    override suspend fun sendInvoiceViaPeppol(
        invoiceId: InvoiceId
    ) = cashflowRemoteDataSource.sendInvoiceViaPeppol(invoiceId)

    override suspend fun pollPeppolInbox() = cashflowRemoteDataSource.pollPeppolInbox()

    override suspend fun getPeppolTransmissionForInvoice(
        invoiceId: InvoiceId
    ) = cashflowRemoteDataSource.getPeppolTransmissionForInvoice(invoiceId)
}
