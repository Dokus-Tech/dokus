package tech.dokus.app.notifications

import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.DocDto
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

interface InvoiceLookupDataSource {
    suspend fun getInvoice(id: InvoiceId): Result<DocDto.Invoice.Confirmed>
}

internal class CashflowInvoiceLookupDataSource(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : InvoiceLookupDataSource {
    override suspend fun getInvoice(id: InvoiceId): Result<DocDto.Invoice.Confirmed> {
        return cashflowRemoteDataSource.getInvoice(id)
    }
}

