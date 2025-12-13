package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceUiState
import ai.dokus.app.cashflow.viewmodel.InvoiceLineItem
import ai.dokus.foundation.domain.ids.ClientId
import ai.dokus.foundation.domain.model.ClientDto
import ai.dokus.foundation.domain.model.ClientName
import kotlinx.datetime.LocalDate

/**
 * Mock data for invoice component previews.
 */
object Mocks {

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    val sampleClient = ClientDto(
        id = ClientId.generate(),
        name = ClientName("Acme Corporation"),
        email = "billing@acme.com",
        vatNumber = "BE0123456789",
        peppolId = "0208:0123456789",
        isPeppolEnabled = true,
        street = "123 Business Street",
        city = "Brussels",
        postalCode = "1000",
        country = "Belgium",
        phone = "+32 2 123 4567",
        notes = null
    )

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    val sampleClientWithoutPeppol = ClientDto(
        id = ClientId.generate(),
        name = ClientName("Local Business BVBA"),
        email = "contact@localbusiness.be",
        vatNumber = "BE9876543210",
        peppolId = null,
        isPeppolEnabled = false,
        street = "456 Commerce Lane",
        city = "Antwerp",
        postalCode = "2000",
        country = "Belgium",
        phone = null,
        notes = null
    )

    val sampleLineItems = listOf(
        InvoiceLineItem(
            id = "1",
            description = "Web Development Services",
            quantity = 40.0,
            unitPrice = "85.00",
            vatRatePercent = 21
        ),
        InvoiceLineItem(
            id = "2",
            description = "UI/UX Design",
            quantity = 16.0,
            unitPrice = "75.00",
            vatRatePercent = 21
        )
    )

    val sampleSingleLineItem = listOf(
        InvoiceLineItem(
            id = "1",
            description = "Consulting Services",
            quantity = 8.0,
            unitPrice = "120.00",
            vatRatePercent = 21
        )
    )

    val sampleFormState = CreateInvoiceFormState(
        selectedClient = sampleClient,
        issueDate = LocalDate(2024, 12, 13),
        dueDate = LocalDate(2025, 1, 13),
        items = sampleLineItems,
        notes = "Thank you for your business!"
    )

    val sampleFormStateWithWarning = CreateInvoiceFormState(
        selectedClient = sampleClientWithoutPeppol,
        issueDate = LocalDate(2024, 12, 13),
        dueDate = LocalDate(2025, 1, 13),
        items = sampleSingleLineItem,
        notes = null
    )

    val emptyFormState = CreateInvoiceFormState()

    val sampleUiState = CreateInvoiceUiState()
}
