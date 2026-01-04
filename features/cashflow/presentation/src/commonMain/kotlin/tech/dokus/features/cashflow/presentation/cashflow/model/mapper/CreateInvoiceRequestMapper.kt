package tech.dokus.features.cashflow.presentation.cashflow.model.mapper

import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState

private const val VatRateMultiplier = 100

internal fun CreateInvoiceFormState.toCreateInvoiceRequest(): CreateInvoiceRequest {
    val client = requireNotNull(selectedClient) {
        "Client must be selected before submitting invoice"
    }
    val issueDate = requireNotNull(issueDate) {
        "Issue date must be set before submitting invoice"
    }
    val dueDate = requireNotNull(dueDate) {
        "Due date must be set before submitting invoice"
    }

    return CreateInvoiceRequest(
        contactId = client.id,
        items = items
            .filter { it.isValid }
            .mapIndexed { index, item ->
                InvoiceItemDto(
                    description = item.description,
                    quantity = item.quantity,
                    unitPrice = Money.fromDouble(item.unitPriceDouble),
                    vatRate = VatRate(item.vatRatePercent * VatRateMultiplier),
                    lineTotal = Money.fromDouble(item.lineTotalDouble),
                    vatAmount = Money.fromDouble(item.vatAmountDouble),
                    sortOrder = index
                )
            },
        issueDate = issueDate,
        dueDate = dueDate,
        notes = notes.takeIf { it.isNotBlank() }
    )
}
