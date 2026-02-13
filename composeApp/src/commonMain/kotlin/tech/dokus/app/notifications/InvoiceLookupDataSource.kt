package tech.dokus.app.notifications

import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.model.FinancialDocumentDto
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource

interface InvoiceLookupDataSource {
    suspend fun getInvoice(id: InvoiceId): Result<FinancialDocumentDto.InvoiceDto>
}

internal class CashflowInvoiceLookupDataSource(
    private val cashflowRemoteDataSource: CashflowRemoteDataSource
) : InvoiceLookupDataSource {
    override suspend fun getInvoice(id: InvoiceId): Result<FinancialDocumentDto.InvoiceDto> {
        return cashflowRemoteDataSource.getInvoice(id)
    }
}

