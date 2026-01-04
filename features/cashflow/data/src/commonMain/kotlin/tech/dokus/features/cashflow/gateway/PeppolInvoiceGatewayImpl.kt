package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.ids.InvoiceId
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

internal class PeppolInvoiceGatewayImpl(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : PeppolInvoiceGateway {
    override suspend fun validateInvoiceForPeppol(
        invoiceId: InvoiceId
    ) = cashflowRemoteDataSource.validateInvoiceForPeppol(invoiceId)

    override suspend fun sendInvoiceViaPeppol(
        invoiceId: InvoiceId
    ) = cashflowRemoteDataSource.sendInvoiceViaPeppol(invoiceId)

    override suspend fun getPeppolTransmissionForInvoice(
        invoiceId: InvoiceId
    ) = cashflowRemoteDataSource.getPeppolTransmissionForInvoice(invoiceId)
}
