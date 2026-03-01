package tech.dokus.features.cashflow.presentation.cashflow.model.mapper

import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.StructuredCommunication
import tech.dokus.domain.model.CreateInvoiceRequest
import tech.dokus.domain.model.InvoiceItemDto
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState

internal fun CreateInvoiceFormState.toCreateInvoiceRequest(
    deliveryMethod: InvoiceDeliveryMethod
): CreateInvoiceRequest {
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
                    unitPrice = item.unitPriceMoney,
                    vatRate = item.vatRate,
                    lineTotal = item.lineTotalMoney,
                    vatAmount = item.vatAmountMoney,
                    sortOrder = index
                )
            },
        issueDate = issueDate,
        dueDate = dueDate,
        paymentTermsDays = paymentTermsDays,
        dueDateMode = dueDateMode,
        structuredCommunication = StructuredCommunication.from(structuredCommunication),
        senderIban = senderIban.takeIf { it.isNotBlank() }?.let { Iban.from(it) },
        senderBic = senderBic.trim()
            .uppercase()
            .takeIf { it.isNotBlank() }
            ?.let(::Bic),
        deliveryMethod = deliveryMethod,
        notes = notes.takeIf { it.isNotBlank() }
    )
}
