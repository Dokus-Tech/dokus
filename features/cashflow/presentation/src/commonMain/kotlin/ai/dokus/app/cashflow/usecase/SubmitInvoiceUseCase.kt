package ai.dokus.app.cashflow.usecase

import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.viewmodel.model.CreateInvoiceFormState
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import ai.dokus.foundation.domain.model.CreateInvoiceRequest
import ai.dokus.foundation.domain.model.FinancialDocumentDto
import ai.dokus.foundation.domain.model.InvoiceItemDto

/**
 * Use case for submitting an invoice to the backend.
 *
 * Transforms form state into API request format and submits to the server.
 * The form must be validated using [ValidateInvoiceUseCase] before submission.
 */
class SubmitInvoiceUseCase(
    private val dataSource: CashflowRemoteDataSource
) {

    /**
     * Submit an invoice to the backend.
     *
     * @param formState The validated form state to submit.
     * @return Result containing the created invoice or error.
     * @throws IllegalStateException if required fields are null (should be caught by validation).
     */
    suspend operator fun invoke(formState: CreateInvoiceFormState): Result<FinancialDocumentDto.InvoiceDto> {
        // These should be guaranteed by validation, but we check for safety
        val client = requireNotNull(formState.selectedClient) {
            "Client must be selected before submitting invoice"
        }
        val issueDate = requireNotNull(formState.issueDate) {
            "Issue date must be set before submitting invoice"
        }
        val dueDate = requireNotNull(formState.dueDate) {
            "Due date must be set before submitting invoice"
        }

        val request = CreateInvoiceRequest(
            contactId = client.id,
            items = formState.items
                .filter { it.isValid }
                .mapIndexed { index, item ->
                    InvoiceItemDto(
                        description = item.description,
                        quantity = item.quantity,
                        unitPrice = Money.fromDouble(item.unitPriceDouble),
                        vatRate = VatRate("${item.vatRatePercent}.00"),
                        lineTotal = Money.fromDouble(item.lineTotalDouble),
                        vatAmount = Money.fromDouble(item.vatAmountDouble),
                        sortOrder = index
                    )
                },
            issueDate = issueDate,
            dueDate = dueDate,
            notes = formState.notes.takeIf { it.isNotBlank() }
        )

        return dataSource.createInvoice(request)
    }
}
