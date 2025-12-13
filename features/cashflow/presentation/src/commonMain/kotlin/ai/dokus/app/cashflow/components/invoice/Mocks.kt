package ai.dokus.app.cashflow.components.invoice

import ai.dokus.app.cashflow.viewmodel.CreateInvoiceFormState
import ai.dokus.app.cashflow.viewmodel.CreateInvoiceUiState
import ai.dokus.app.cashflow.viewmodel.InvoiceLineItem
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.enums.ContactType
import ai.dokus.foundation.domain.ids.ContactId
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.ContactDto
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

/**
 * Mock data for invoice component previews.
 */
object Mocks {

    private val now = LocalDateTime(2024, 12, 13, 12, 0)

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    val sampleClient = ContactDto(
        id = ContactId.generate(),
        tenantId = TenantId.generate(),
        name = Name("Acme Corporation"),
        email = Email("billing@acme.com"),
        vatNumber = VatNumber("BE0123456789"),
        contactType = ContactType.Customer,
        peppolId = "0208:0123456789",
        peppolEnabled = true,
        addressLine1 = "123 Business Street",
        city = "Brussels",
        postalCode = "1000",
        country = "BE",
        phone = "+32 2 123 4567",
        createdAt = now,
        updatedAt = now
    )

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    val sampleClientWithoutPeppol = ContactDto(
        id = ContactId.generate(),
        tenantId = TenantId.generate(),
        name = Name("Local Business BVBA"),
        email = Email("contact@localbusiness.be"),
        vatNumber = VatNumber("BE9876543210"),
        contactType = ContactType.Customer,
        peppolId = null,
        peppolEnabled = false,
        addressLine1 = "456 Commerce Lane",
        city = "Antwerp",
        postalCode = "2000",
        country = "BE",
        phone = null,
        createdAt = now,
        updatedAt = now
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
